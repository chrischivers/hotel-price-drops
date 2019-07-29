package hotelpricedrops

import java.time.Instant

import cats.effect.{IO, Timer}
import cats.syntax.traverse._
import cats.instances.list._
import doobie.hikari.HikariTransactor
import hotelpricedrops.Model.{Search, User}
import hotelpricedrops.db.{
  DBStaticLoader,
  HotelsDB,
  ResultsDB,
  SearchesDB,
  UsersDB
}
import hotelpricedrops.notifier.EmailNotifier
import hotelpricedrops.pricefetchers.PriceFetcher
import io.chrisdavenport.log4cats.Logger
import org.openqa.selenium.remote.RemoteWebDriver

object Application {

  case class Resources(webDriver: RemoteWebDriver,
                       db: HikariTransactor[IO],
                       config: Config.Config)

  def run(resources: Resources)(implicit logger: Logger[IO],
                                timer: Timer[IO]) = {

    val hotelsDb = HotelsDB(resources.db)
    val searchesDb = SearchesDB(resources.db)
    val resultsDb = ResultsDB(resources.db)
    val usersDb = UsersDB(resources.db)

    val notifier = EmailNotifier(resources.config.emailerConfig)

    val priceFetchers =
      List(
        PriceFetcher(resources.webDriver,
                     ComparisonSite.Kayak,
                     resources.config.screenshotOnError,
                     notifier.errorNotify),
        PriceFetcher(resources.webDriver,
                     ComparisonSite.SkyScanner,
                     resources.config.screenshotOnError,
                     notifier.errorNotify)
      )
    val comparer = Comparer(resultsDb, notifier, resources.config)

    for {
      _ <- logger.info(s"Starting run at ${Instant.now().toString}")
      _ <- DBStaticLoader.populateHotels(hotelsDb)
      _ <- DBStaticLoader.populateSearches(searchesDb)
      _ <- DBStaticLoader.populateUsers(usersDb)
      searches <- searchesDb.allSearches
      - <- searches.traverse { search =>
        usersDb.usersFor(search.searchId).flatMap { users =>
          users.traverse { user =>
            processSearch(search, user, hotelsDb, priceFetchers, comparer)
          }
        }
      }
      _ <- logger.info(s"Finished run at ${Instant.now().toString}")
    } yield ()
  }

  def processSearch(search: Search.WithId,
                    user: User,
                    hotelsDB: HotelsDB,
                    priceFetchers: List[PriceFetcher],
                    comparer: Comparer)(implicit logger: Logger[IO]) = {
    for {
      allHotels <- hotelsDB.allHotels
      _ <- allHotels.traverse { hotel =>
        Hotels.pricesForHotel(hotel.withoutid, priceFetchers).flatMap {
          results =>
            comparer.compareAndNotify(hotel, search, user, results)
        }
      }
    } yield ()
  }

}
