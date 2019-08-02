package hotelpricedrops

import java.util.concurrent.Executors

import cats.effect._
import hotelpricedrops.Application.Resources
import hotelpricedrops.db.DB
import hotelpricedrops.selenium.WebDriver
import io.chrisdavenport.log4cats.SelfAwareStructuredLogger
import io.chrisdavenport.log4cats.slf4j.Slf4jLogger

import scala.concurrent.ExecutionContext

object Main extends IOApp.WithContext {

  implicit val logger: SelfAwareStructuredLogger[IO] =
    Slf4jLogger.getLogger[IO]

  override protected def executionContextResource
    : Resource[SyncIO, ExecutionContext] =
    Resource
      .make(SyncIO(Executors.newFixedThreadPool(100)))(
        ec => SyncIO(ec.shutdown())
      )
      .map(ExecutionContext.fromExecutor(_))

  override def run(args: List[String]): IO[ExitCode] = {

    val config = Config()

    val resources = for {
      _ <- Resource.liftF(logger.info("Loading application resources"))
      db <- DB.transactorResource(config.dbConfig)
      webDriver <- WebDriver(config.geckoDriverPath, headless = true)
    } yield Resources(webDriver, db, config)

    def runner: IO[Unit] = {
      resources
        .use { resources =>
          Application.run(resources)
        }
        .flatMap(_ => timer.sleep(config.timeBetweenRuns))
        .flatMap(_ => runner)
    }

    runner.map(_ => ExitCode.Success)
  }
}
