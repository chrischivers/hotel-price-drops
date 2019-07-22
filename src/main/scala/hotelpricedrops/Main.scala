package hotelpricedrops

import java.util.concurrent.Executors

import cats.effect._
import cats.instances.list._
import cats.syntax.traverse._
import cats.syntax.flatMap._
import dev.profunktor.redis4cats.log4cats._
import hotelpricedrops.db.{DB, RedisDB}
import hotelpricedrops.notifier.{EmailNotifier, Notifier}
import hotelpricedrops.pricefetchers.KayakPriceFetcher
import io.chrisdavenport.log4cats.SelfAwareStructuredLogger
import io.chrisdavenport.log4cats.slf4j.Slf4jLogger
import org.openqa.selenium.remote.RemoteWebDriver

import scala.concurrent.ExecutionContext

object Main extends IOApp.WithContext {

  System.setProperty(
    "webdriver.gecko.driver",
    "/Users/chrichiv/Downloads/geckodriver") //todo remove from code

  override protected def executionContextResource
    : Resource[SyncIO, ExecutionContext] =
    Resource
      .make(SyncIO(Executors.newFixedThreadPool(100)))(ec =>
        SyncIO(ec.shutdown()))
      .map(ExecutionContext.fromExecutor(_))

  override def run(args: List[String]): IO[ExitCode] = {

    case class Resources(webDriver: RemoteWebDriver, db: DB, notifier: Notifier)

    implicit val logger: SelfAwareStructuredLogger[IO] =
      Slf4jLogger.getLogger[IO]

    val resources = for {
      webDriver <- WebDriver(headless = false)
      redis <- RedisDB.redisClientResource
      config <- Resource.liftF(Config())
      notifier <- Resource.liftF(EmailNotifier(config.emailerConfig))
    } yield Resources(webDriver, RedisDB(redis), notifier)

    resources.use { resources =>
      val priceFetchers = List(new KayakPriceFetcher(resources.webDriver))

      for {
        _ <- logger.info("Starting run")
        hotels <- Hotels.getHotelsFromFile("hotel-list.json")
        fetchResults <- hotels.traverse(hotel =>
          Hotels.pricesForHotel(hotel, priceFetchers).map((hotel, _)))
        _ <- Comparer.compare(resources.db, resources.notifier, fetchResults)
      } yield {
        ExitCode.Success
      }

    }
  }
}
