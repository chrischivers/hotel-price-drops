package hotelpricedrops

import java.time.Instant

import cats.effect.{ContextShift, IO, Resource, Timer}
import cats.syntax.traverse._
import cats.instances.list._
import doobie.hikari.HikariTransactor
import hotelpricedrops.Model.{Search, User}
import hotelpricedrops.comparer.Comparer
import hotelpricedrops.db.{
  DBStaticLoader,
  HotelsDB,
  ResultsDB,
  SearchesDB,
  UsersDB
}
import hotelpricedrops.notifier.EmailNotifier
import hotelpricedrops.pricefetchers.PriceFetcher
import hotelpricedrops.selenium.WebDriver
import io.chrisdavenport.log4cats.Logger

object Application {

  def run(db: HikariTransactor[IO],
          webDriverResource: Resource[IO, WebDriver],
          config: Config)(implicit logger: Logger[IO],
                          timer: Timer[IO],
                          contextShift: ContextShift[IO]) = {

    val hotelsDb = HotelsDB(db)
    val searchesDb = SearchesDB(db)
    val resultsDb = ResultsDB(db)
    val usersDb = UsersDB(db)

    val notifier = EmailNotifier(config.emailerConfig)

    val priceFetchers =
      List(
        PriceFetcher(
          webDriverResource,
          ComparisonSite.Kayak,
          config.priceNotificationConfig.emailScreenshotOnError,
          notifier.errorNotify
        ),
        PriceFetcher(
          webDriverResource,
          ComparisonSite.SkyScanner,
          config.priceNotificationConfig.emailScreenshotOnError,
          notifier.errorNotify
        ),
        PriceFetcher(
          webDriverResource,
          ComparisonSite.Trivago,
          config.priceNotificationConfig.emailScreenshotOnError,
          notifier.errorNotify
        )
      )
    val comparer =
      Comparer(resultsDb, notifier, config.priceNotificationConfig)

    for {
      _ <- logger.info(s"Starting run at ${Instant.now().toString}")
      _ <- DBStaticLoader.populateHotels(hotelsDb)
      _ <- DBStaticLoader.populateSearches(searchesDb)
      _ <- DBStaticLoader.populateUsers(usersDb)
      searches <- searchesDb.allSearches
      _ <- searches.traverse { search =>
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
                    comparer: Comparer)(implicit logger: Logger[IO],
                                        timer: Timer[IO],
                                        contextShift: ContextShift[IO]) = {
    for {
      allHotels <- hotelsDB.allHotels
      _ <- allHotels.traverse { hotel =>
        Hotels
          .pricesForHotel(
            hotel.withoutid,
            priceFetchers,
            search.withoutId.numberOfNights
          )
          .flatMap { results =>
            comparer.compareAndNotify(hotel, search, user, results)
          }
      }
    } yield ()
  }

}
