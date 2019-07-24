package hotelpricedrops.pricefetchers

import cats.effect.{IO, Timer}
import cats.syntax.traverse._
import cats.instances.list._
import hotelpricedrops.Model
import hotelpricedrops.Model.{ComparisonSite, Hotel, PriceDetails}
import io.chrisdavenport.log4cats.Logger
import org.openqa.selenium.By
import org.openqa.selenium.remote.RemoteWebDriver
import scala.collection.JavaConverters._

import scala.concurrent.duration._

class KayakPriceFetcher(driver: RemoteWebDriver,
                        pageLoadWaiTime: FiniteDuration)(
    implicit timer: Timer[IO],
    logger: Logger[IO])
    extends PriceFetcher {

  override def comparisonSite: Model.ComparisonSite = ComparisonSite("kayak")

  override def getPriceDetailsFor(hotel: Hotel): IO[PriceDetails] =
    for {
      _ <- logger.info(s"Looking up prices for hotel $hotel on Kayak")
      _ <- IO(driver.get(hotel.kayakUrl.renderString))
      _ <- IO.sleep(pageLoadWaiTime)
      elements <- IO(driver.findElementsByClassName("provider").asScala.toList)
      idPriceList <- elements
        .traverse { element =>
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
        .map(_.flatten)
      (lowestPriceId, lowestPrice) = idPriceList.minBy {
        case (_, price) => price
      } //todo make safer
      _ <- logger.info(
        s"Found price of $lowestPrice on Kayak for hotel $hotel (on $lowestPriceId)")
    } yield {
      PriceDetails(lowestPriceId, lowestPrice)
    }
}
