package hotelpricedrops.pricefetchers

import java.time.Instant

import cats.effect.{IO, Timer}
import cats.syntax.traverse._
import cats.syntax.flatMap._
import cats.instances.list._
import hotelpricedrops.Model
import hotelpricedrops.Model.{ComparisonSite, Hotel, PriceDetails}
import io.chrisdavenport.log4cats.Logger
import org.openqa.selenium.{By, OutputType, WebElement}
import org.openqa.selenium.remote.RemoteWebDriver

import scala.collection.JavaConverters._
import scala.concurrent.duration._
import hotelpricedrops.util._

class KayakPriceFetcher(driver: RemoteWebDriver)(implicit timer: Timer[IO],
                                                 logger: Logger[IO])
    extends PriceFetcher {

  val maxLoadWaitTime: FiniteDuration = 2.minutes
  val timeBetweenLoadReadyChecks: FiniteDuration = 5.seconds

  override def comparisonSite: Model.ComparisonSite = ComparisonSite("kayak")

  override def getPriceDetailsFor(hotel: Hotel): IO[PriceDetails] =
    for {
      _ <- logger.info(s"Looking up prices for hotel ${hotel.name} on Kayak")
      _ <- IO(driver.get(hotel.kayakUrl.renderString))
        .withRetry(attempts = 3)
      _ <- wait(driver)
      elements <- IO(driver.findElementsByClassName("provider").asScala.toList)
      idPriceList <- elements.traverse(priceFromProviderElement).map(_.flatten)
      (lowestPriceId, lowestPrice) = idPriceList.minBy {
        case (_, price) => price
      }
      _ <- logger.info(
        s"Found price of £$lowestPrice on Kayak for hotel ${hotel.name} (on $lowestPriceId)")
      screenshot <- IO(driver.getScreenshotAs(OutputType.BYTES))
    } yield {
      PriceDetails(lowestPriceId, lowestPrice, screenshot)
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

  private def wait(driver: RemoteWebDriver) =
    waitToBeReady(maxLoadWaitTime,
                  timeBetweenLoadReadyChecks,
                  d =>
                    IO(
                      d.findElementsByClassName("price")
                        .asScala
                        .toList
                        .exists(_.getText.headOption.contains('£'))))(driver)

  private def waitToBeReady(maxLoadWaitTime: FiniteDuration,
                            timeBetweenLoadReadyAttempts: FiniteDuration,
                            condition: RemoteWebDriver => IO[Boolean])(
      remoteWebDriver: RemoteWebDriver): IO[Unit] = {
    val startTime = Instant.now

    def helper: IO[Unit] = condition(remoteWebDriver).flatMap {
      case true => IO.unit
      case false
          if (Instant.now.toEpochMilli - startTime.toEpochMilli) < maxLoadWaitTime.toMillis =>
        IO.sleep(timeBetweenLoadReadyAttempts) >> helper
      case _ =>
        IO.raiseError(new RuntimeException("Wait condition not satisfied"))
    }
    logger.info(s"Waiting for page to load (max wait time $maxLoadWaitTime)") >> helper
  }
}
