package hotelpricedrops

import cats.effect.IO
import cats.syntax.traverse._
import cats.instances.list._
import hotelpricedrops.Model.PriceDetails
import hotelpricedrops.pricefetchers.PriceFetcher.PriceFetcherSpec
import org.http4s.Uri
import org.openqa.selenium.{By, WebElement}
import org.openqa.selenium.remote.RemoteWebDriver

import scala.collection.JavaConverters._

sealed trait ComparisonSite {
  def name: String
  def priceFetcherSpec: PriceFetcherSpec
}

object ComparisonSite {

  case object Kayak extends ComparisonSite {
    override def name: String = "Kayak"

    override def priceFetcherSpec: PriceFetcherSpec = {

      val waitCondition: RemoteWebDriver => IO[Boolean] = d =>
        IO(
          d.findElementsByClassName("price")
            .asScala
            .toList
            .exists(_.getText.headOption.contains('£')))

      def priceFromProviderElement(element: WebElement) = {
        IO {
          val seller = element.getAttribute("id").trim
          val price = if (seller.nonEmpty) {
            Some(
              element
                .findElement(By.className("price"))
                .getText
                .drop(1)
                .toInt)
          } else None
          price.map((seller, _))
        }
      }

      val getLowestPrice: RemoteWebDriver => IO[PriceDetails] = driver =>
        for {
          elements <- IO(
            driver.findElementsByClassName("provider").asScala.toList)
          sellerPriceList <- elements
            .traverse(priceFromProviderElement)
            .map(_.flatten)
          (lowestPriceSeller, lowestPrice) = sellerPriceList.minBy {
            case (_, price) => price
          }
        } yield
          PriceDetails(lowestPriceSeller,
                       lowestPrice,
                       Uri.unsafeFromString(driver.getCurrentUrl))

      PriceFetcherSpec(waitCondition, getLowestPrice)
    }
  }

  case object SkyScanner extends ComparisonSite {
    override def name: String = "SkyScanner"

    override def priceFetcherSpec: PriceFetcherSpec = ???
  }

}
