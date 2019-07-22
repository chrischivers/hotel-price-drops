package hotelpricedrops.pricefetchers

import cats.effect.{IO, Timer}
import hotelpricedrops.Model
import hotelpricedrops.Model.{ComparisonSite, Hotel, PriceDetails}
import io.chrisdavenport.log4cats.Logger
import org.openqa.selenium.By
import org.openqa.selenium.remote.RemoteWebDriver

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
      element <- IO(driver.findElementByClassName("top-deal"))
      seller <- IO(element.getAttribute("id"))
      price <- IO(element.findElement(By.className("price")).getText)
      _ <- IO(assert(price.head == '£'))
      _ <- logger.info(s"Found price of $price on Kayak for hotel $hotel")
    } yield {
      PriceDetails(seller, price.drop(1).toInt)
    }
}
