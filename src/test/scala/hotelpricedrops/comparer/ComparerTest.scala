package hotelpricedrops.comparer

import java.time.LocalDate

import cats.effect.IO
import cats.effect.concurrent.Ref
import hotelpricedrops.ComparisonSite.Trivago
import hotelpricedrops.Model.{PriceDetails, Search}
import hotelpricedrops.db._
import hotelpricedrops.notifier.PriceNotification.PriceNotificationConfig
import hotelpricedrops.notifier.{Notifier, PriceNotification}
import hotelpricedrops.pricefetchers.PriceFetcher
import hotelpricedrops.pricefetchers.PriceFetcher.ErrorString
import hotelpricedrops.utils.Any
import hotelpricedrops.{ComparisonSite, Main, Model}
import org.scalactic.TypeCheckedTripleEquals
import org.scalatest.prop.{TableDrivenPropertyChecks, TableFor7}
import org.scalatest.{Assertion, Matchers, WordSpec}

import scala.concurrent.ExecutionContext

class ComparerTest
    extends WordSpec
    with TypeCheckedTripleEquals
    with Matchers
    with TableDrivenPropertyChecks {

  val hotel = Any.hotel.withId(1)
  val search = Search(LocalDate.now(), LocalDate.now().plusDays(7), 2).withId(1)
  val user = Any.user
  val seller = Any.str
  val url = Any.uri

  def persistDefaults(fixture: Setup.Fixture): IO[Unit] =
    for {
      _ <- fixture.hotelsDB.persistHotel(hotel.withoutid)
      _ <- fixture.searchesDB.persistSearch(search.withoutId)
    } yield ()

  "Comparer" should {
    "Record the lowest result in DB for a set of price fetcher results" in Setup() {
      f =>
        val priceFetcherResults = List(
          PriceFetcher.Result(
            ComparisonSite.SkyScanner,
            PriceDetails(seller, 750, Any.uri),
            Any.screenshot
          ),
          PriceFetcher.Result(
            ComparisonSite.Trivago,
            PriceDetails(seller, 90, Any.uri),
            Any.screenshot
          ),
          PriceFetcher.Result(
            ComparisonSite.Kayak,
            PriceDetails(seller, 100, Any.uri),
            Any.screenshot
          )
        )
        for {
          _ <- persistDefaults(f)
          _ <- f.comparer.compareAndNotify(
            hotel,
            search,
            user,
            priceFetcherResults
          )
          result <- f.resultsDB
            .mostRecentPriceFor(search.searchId, hotel.hotelId)
        } yield {
          result.map(_.withoutIdAndTimestamp) should ===(
            Some(Model.Result(search.searchId, hotel.hotelId, 90, Trivago.name))
          )
        }
    }
    "Send correct notifications" should {

      def setUpTable(
        values: (Boolean, Boolean, Boolean, Int, Int, ComparisonSite, Boolean)*
      ) = {
        Table(
          (
            "emailOnDecrease",
            "emailOnIncrease",
            "emailOnStaySame",
            "previousPrice",
            "currentPrice",
            "currentComparisonSite",
            "emailSent"
          ),
          values: _*
        )
      }

      "when price decreases" in {

        val priceDecreaseTests = setUpTable(
          (true, false, false, 100, 90, ComparisonSite.Kayak, true),
          (false, false, false, 100, 90, ComparisonSite.Kayak, false),
          (true, false, false, 90, 100, ComparisonSite.Kayak, false)
        )
        runTablePropertyTests(priceDecreaseTests)
      }

      "when price increases" in {

        val priceIncreaseTests = setUpTable(
          (false, true, false, 90, 100, ComparisonSite.Kayak, true),
          (false, false, false, 90, 100, ComparisonSite.Kayak, false),
          (false, true, false, 100, 90, ComparisonSite.Kayak, false)
        )
        runTablePropertyTests(priceIncreaseTests)
      }
    }
  }

  def runTablePropertyTests(
    table: TableFor7[Boolean,
                     Boolean,
                     Boolean,
                     Int,
                     Int,
                     ComparisonSite,
                     Boolean]
  ) = {
    forAll(table) {
      case (
          emailOnDecrease,
          emailOnIncrease,
          emailOnStaySame,
          previousPrice,
          currentPrice,
          currentComparisonSite,
          emailSent
          ) =>
        val config = PriceNotificationConfig(
          emailOnDecrease,
          emailOnIncrease,
          emailOnStaySame,
          emailOnLowestPriceSinceCreated = false,
          emailScreenshotOnError = false
        )

        Setup(config) { f =>
          val previousResult =
            Model.Result(1, 1, previousPrice, ComparisonSite.Kayak.name)

          val priceFetcherResults = List(
            PriceFetcher.Result(
              currentComparisonSite,
              PriceDetails(seller, currentPrice, url),
              Any.screenshot
            )
          )

          for {
            _ <- persistDefaults(f)
            _ <- f.resultsDB.persistResult(previousResult)
            _ <- f.comparer.compareAndNotify(
              hotel,
              search,
              user,
              priceFetcherResults
            )
            notifications <- f.notifications.get
          } yield {
            if (emailSent) {
              notifications should ===(
                List(
                  PriceNotification(
                    user.emailAddress,
                    hotel.name,
                    previousPrice,
                    currentPrice,
                    seller,
                    currentComparisonSite.name,
                    url,
                    Some(previousPrice)
                  )
                )
              )
            } else {
              notifications should ===(List.empty)
            }
          }
        }
    }
  }

  object Setup {
    implicit val executionContext = ExecutionContext.global
    implicit val contextShift = IO.contextShift(executionContext)
    implicit val logger = Main.logger

    case class Fixture(comparer: Comparer,
                       notifier: Notifier,
                       resultsDB: ResultsDB,
                       searchesDB: SearchesDB,
                       hotelsDB: HotelsDB,
                       notifications: Ref[IO, List[PriceNotification]],
                       errorNotificiations: Ref[IO, List[ErrorString]])

    val defaultPriceNotificationConfig = PriceNotificationConfig(
      emailOnAllPriceDecreases = false,
      emailOnAllPriceIncreases = false,
      emailOnPriceNoChange = false,
      emailOnLowestPriceSinceCreated = true,
      emailScreenshotOnError = false
    )

    private def stubEmailNotifier(
      notifications: Ref[IO, List[PriceNotification]],
      errorNotifications: Ref[IO, List[ErrorString]]
    ): Notifier = new Notifier {
      override def priceNotify(priceNotification: PriceNotification,
                               screenshot: Model.Screenshot): IO[Unit] =
        notifications.update(_ :+ priceNotification)
      override def errorNotify(errorMsg: ErrorString,
                               screenshot: Model.Screenshot): IO[Unit] =
        errorNotifications.update(_ :+ errorMsg)
    }

    def apply[T](
      config: PriceNotificationConfig = defaultPriceNotificationConfig
    )(f: Fixture => IO[Assertion]): Assertion = {

      DB.transactorResource(h2DbConfig)
        .use { tr =>
          for {
            notificationsRef <- Ref.of[IO, List[PriceNotification]](List.empty)
            errorsRef <- Ref.of[IO, List[ErrorString]](List.empty)
            resultsDB = ResultsDB(tr)
            searchesDB = SearchesDB(tr)
            hotelsDB = HotelsDB(tr)
            notifier = stubEmailNotifier(notificationsRef, errorsRef)
            comparer = Comparer(resultsDB, notifier, config)
            fixture = Fixture(
              comparer,
              notifier,
              resultsDB,
              searchesDB,
              hotelsDB,
              notificationsRef,
              errorsRef
            )
            result <- f(fixture)
          } yield result
        }
        .unsafeRunSync()
    }
  }

}
