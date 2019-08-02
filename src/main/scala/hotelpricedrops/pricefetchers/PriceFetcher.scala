package hotelpricedrops.pricefetchers

import java.time.Instant

import cats.effect.{IO, Timer}
import cats.syntax.flatMap._
import hotelpricedrops.Model.{Hotel, PriceDetails, ReportedRateType, Screenshot}
import hotelpricedrops.selenium.WebDriver
import hotelpricedrops.util._
import hotelpricedrops.{ComparisonSite, pricefetchers}
import io.chrisdavenport.log4cats.Logger

import scala.concurrent.duration._

trait PriceFetcher {

  def comparisonSite: ComparisonSite

  def getPriceDetailsFor(hotel: Hotel,
                         nights: Int): IO[Option[PriceFetcher.Results]]
}

object PriceFetcher {

  type ErrorString = String

  case class Results(comparisonSite: ComparisonSite,
                     priceDetails: PriceDetails,
                     screenshot: Screenshot)

  def apply(
    driver: WebDriver,
    site: ComparisonSite,
    screenshotOnError: Boolean,
    notifyOnError: (ErrorString, Screenshot) => IO[Unit]
  )(implicit timer: Timer[IO], logger: Logger[IO]): PriceFetcher =
    new PriceFetcher {

      override def comparisonSite: ComparisonSite = site

      val maxLoadWaitTime: FiniteDuration = 2.minutes
      val timeBetweenLoadReadyAttempts: FiniteDuration = 5.seconds

      override def getPriceDetailsFor(
        hotel: Hotel,
        nights: Int
      ): IO[Option[pricefetchers.PriceFetcher.Results]] = {

        hotel
          .urlFor(comparisonSite)
          .fold[IO[Option[PriceFetcher.Results]]](IO.pure(None)) { url =>
            val getResults = for {
              _ <- logger.info(
                s"Looking up prices for hotel ${hotel.name} on ${comparisonSite.name}"
              )
              _ <- driver.setUrl(url).withRetry(3)
              _ <- waitToBeReady(driver)()
              priceDetails <- comparisonSite.getLowestPrice(driver)
              _ <- logger.info(
                s"lowest price retried from ${comparisonSite.name} for ${hotel.name} is ${priceDetails.price}"
              )
              nightlyPrice = comparisonSite.reportedRateType match {
                case ReportedRateType.Nightly => priceDetails.price
                case ReportedRateType.Entirety =>
                  priceDetails.price / nights
              }
              _ <- logger.info(
                s"Found nightly price of £$nightlyPrice on ${comparisonSite.name} for hotel ${hotel.name} (on ${priceDetails.seller})"
              )
              screenshot <- driver.takeScreenshot
            } yield {
              Some(
                PriceFetcher
                  .Results(comparisonSite, priceDetails, screenshot)
              )
            }

            getResults.handleErrorWith { err =>
              (if (screenshotOnError) {
                 driver.takeScreenshot
                   .flatMap(
                     screenshot => notifyOnError(err.toString, screenshot)
                   )
               } else IO.unit) >> IO.raiseError(err)
            }
          }
      }

      private def waitToBeReady(webDriver: WebDriver)(
        maxLoadWaitTime: FiniteDuration = maxLoadWaitTime,
        timeBetweenLoadReadyAttempts: FiniteDuration =
          timeBetweenLoadReadyAttempts
      ): IO[Unit] = {
        val startTime = Instant.now

        def helper: IO[Unit] =
          comparisonSite
            .waitToBeReadyCondition(webDriver)
            .flatMap {
              case true => IO.unit
              case false
                  if (Instant.now.toEpochMilli - startTime.toEpochMilli) < maxLoadWaitTime.toMillis =>
                IO.sleep(timeBetweenLoadReadyAttempts) >> helper
              case _ =>
                IO.raiseError(
                  new RuntimeException("Wait condition not satisfied")
                )
            }
        logger
          .info(s"Waiting for page to load (max wait time $maxLoadWaitTime)") >> helper >> IO
          .sleep(3.seconds)
      }
    }
}
