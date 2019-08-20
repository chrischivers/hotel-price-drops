package hotelpricedrops

import java.util.concurrent.Executors

import cats.effect._
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

    val config = Config.load()

    def runner: IO[Unit] = {
      DB.transactorResource(config.dbConfig)
        .use { db =>
          val webDriverResource =
            WebDriver.resource(config.geckoDriverPath, headless = true)
          Application.run(db, webDriverResource, config)
        }
        .flatMap(_ => timer.sleep(config.timeBetweenRuns))
        .flatMap(_ => runner)
    }

    runner.map(_ => ExitCode.Success)
  }
}
