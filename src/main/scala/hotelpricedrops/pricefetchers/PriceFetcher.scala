package hotelpricedrops.pricefetchers

import java.time.Instant

import cats.effect.{IO, Timer}
import hotelpricedrops.{ComparisonSite, pricefetchers}
import io.chrisdavenport.log4cats.Logger
import org.openqa.selenium.{By, OutputType, WebElement}
import org.openqa.selenium.remote.RemoteWebDriver
import cats.effect.{IO, Timer}
import cats.syntax.flatMap._
import hotelpricedrops.ComparisonSite
import hotelpricedrops.Model.{Hotel, PriceDetails, ReportedRateType, Screenshot}
import hotelpricedrops.util._
import io.chrisdavenport.log4cats.Logger
import org.openqa.selenium.remote.RemoteWebDriver
import org.openqa.selenium.{By, OutputType, WebElement}

import scala.concurrent.duration._

trait PriceFetcher {

  def comparisonSite: ComparisonSite

  def getPriceDetailsFor(hotel: Hotel): IO[Option[PriceFetcher.Results]]

}

object PriceFetcher {

  type ErrorString = String

  case class Results(comparisonSite: ComparisonSite,
                     priceDetails: PriceDetails,
                     screenshot: Screenshot)

  def apply(driver: RemoteWebDriver,
            site: ComparisonSite,
            notifyOnError: (ErrorString, Screenshot) => IO[Unit])(
      implicit timer: Timer[IO],
      logger: Logger[IO]) = new PriceFetcher {

    override def comparisonSite: ComparisonSite = site

    val maxLoadWaitTime: FiniteDuration = 2.minutes
    val timeBetweenLoadReadyAttempts: FiniteDuration = 5.seconds

    override def getPriceDetailsFor(
        hotel: Hotel): IO[Option[pricefetchers.PriceFetcher.Results]] = {

      hotel
        .urlFor(comparisonSite)
        .fold[IO[Option[PriceFetcher.Results]]](IO.pure(None)) { url =>
          val getResults = for {
            _ <- logger.info(
              s"Looking up prices for hotel ${hotel.name} on ${comparisonSite.name}")
            _ <- IO(driver.get(url.renderString)).withRetry(3)
            _ <- waitToBeReady(driver)()
            priceDetails <- comparisonSite.getLowestPrice(driver)
            nightlyPrice = comparisonSite.reportedRateType match {
              case ReportedRateType.Nightly => priceDetails.price
              case ReportedRateType.Entirety =>
                priceDetails.price / 7 //todo fix
            }
            _ <- logger.info(
              s"Found nightly price of £$nightlyPrice on ${comparisonSite.name} for hotel ${hotel.name} (on ${priceDetails.seller})")
            screenshot <- IO(driver.getScreenshotAs(OutputType.BYTES))
          } yield {
            Some(
              PriceFetcher
                .Results(comparisonSite, priceDetails, Screenshot(screenshot)))
          }

          getResults.handleErrorWith { err =>
            IO(driver.getScreenshotAs(OutputType.BYTES))
              .map(Screenshot)
              .flatMap(screenshot => notifyOnError(err.toString, screenshot)) >> IO
              .raiseError(err)
          }
        }
    }

    private def priceFromProviderElement(element: WebElement) = {
      IO {
        val id = element.getAttribute("id").trim
        val price = if (id.nonEmpty) {
          Some(
            element
              .findElement(By.className("price"))
              .getText
              .drop(1)
              .toInt)
        } else None
        price.map((id, _))
      }
    }

    private def waitToBeReady(remoteWebDriver: RemoteWebDriver)(
        maxLoadWaitTime: FiniteDuration = maxLoadWaitTime,
        timeBetweenLoadReadyAttempts: FiniteDuration =
          timeBetweenLoadReadyAttempts): IO[Unit] = {
      val startTime = Instant.now

      def helper: IO[Unit] =
        comparisonSite
          .waitToBeReadyCondition(remoteWebDriver)
          .flatMap {
            case true => IO.unit
            case false
                if (Instant.now.toEpochMilli - startTime.toEpochMilli) < maxLoadWaitTime.toMillis =>
              IO.sleep(timeBetweenLoadReadyAttempts) >> helper
            case _ =>
              IO.raiseError(
                new RuntimeException("Wait condition not satisfied"))
          }
      logger
        .info(s"Waiting for page to load (max wait time $maxLoadWaitTime)") >> helper
    }
  }
}
