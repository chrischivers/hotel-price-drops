package hotelpricedrops

import java.time.{Instant, LocalDateTime}
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

    val config = Config()

    val resources = for {
      _ <- Resource.liftF(logger.info("Loading application resources"))
      _ <- Resource.liftF(
        IO(
          System.setProperty("webdriver.gecko.driver", config.geckoDriverPath)))
      webDriver <- WebDriver(headless = true)
      redis <- RedisDB.redisClientResource
      notifier <- Resource.liftF(EmailNotifier(config.emailerConfig))
    } yield Resources(webDriver, RedisDB(redis), notifier)

    val runComparison = resources.use { resources =>
      val priceFetchers = List(new KayakPriceFetcher(resources.webDriver))
      val comparer = Comparer(resources.db, resources.notifier, config)

      for {
        _ <- logger.info(s"Starting run at ${Instant.now().toString}")
        hotels <- Hotels.getHotelsFromFile("hotel-list.json")
        _ <- hotels.traverse { hotel =>
          for {
            results <- Hotels.pricesForHotel(hotel, priceFetchers)
            _ <- comparer.compareAndNotify(hotel, results)
          } yield ()
        }
      } yield ()
    }

    def runner: IO[Unit] = {
      runComparison
        .flatMap(_ => timer.sleep(config.timeBetweenRuns))
        .flatMap(_ => runner)
    }

    runner.map(_ => ExitCode.Success)
  }
}
