package hotelpricedrops

import cats.effect.IO
import cats.syntax.traverse._
import cats.instances.list._
import hotelpricedrops.Model.{PriceDetails, ReportedRateType}
import org.http4s.Uri
import org.openqa.selenium.{By, WebElement}
import org.openqa.selenium.remote.RemoteWebDriver

import scala.collection.JavaConverters._

sealed trait ComparisonSite {
  def name: String
  def reportedRateType: ReportedRateType
  def waitToBeReadyCondition: RemoteWebDriver => IO[Boolean]
  def getLowestPrice: RemoteWebDriver => IO[PriceDetails]
}

object ComparisonSite {

  case object Kayak extends ComparisonSite {
    override def name: String = "Kayak"

    override def reportedRateType: ReportedRateType = ReportedRateType.Nightly

    override def waitToBeReadyCondition: RemoteWebDriver => IO[Boolean] =
      d =>
        IO(
          d.findElementsByClassName("price")
            .asScala
            .toList
            .exists(_.getText.headOption.contains('£')))

    override def getLowestPrice: RemoteWebDriver => IO[PriceDetails] =
      driver =>
        for {
          elements <- IO(
            driver.findElementsByClassName("provider").asScala.toList)
          sellerPriceList <- elements
            .traverse(priceFromProviderElement)
            .map(_.flatten)
          (lowestPriceSeller, lowestPrice) = sellerPriceList
            .minBy { //todo unsafe
              case (_, price) => price
            }
        } yield
          PriceDetails(lowestPriceSeller,
                       lowestPrice,
                       Uri.unsafeFromString(driver.getCurrentUrl))

    private def priceFromProviderElement(element: WebElement) = IO {
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

  case object SkyScanner extends ComparisonSite {
    override def name: String = "SkyScanner"

    override def reportedRateType: ReportedRateType = ReportedRateType.Entirety

    override def waitToBeReadyCondition: RemoteWebDriver => IO[Boolean] =
      d =>
        IO {
          d.findElements(By.xpath("//span[@data-test-id='main-cta-price']"))
            .asScala
            .toList
            .exists(_.getText.headOption.contains('£'))
      }

    override def getLowestPrice: RemoteWebDriver => IO[PriceDetails] =
      d =>
        IO {
          val price: Int = d
            .findElements(By.xpath("//span[@data-test-id='main-cta-price']"))
            .asScala
            .toList
            .map(_.getText)
            .filter(_.headOption.contains('£'))
            .map(_.drop(1).toInt)
            .min //todo unsafe

          PriceDetails("Unknown", price, Uri.unsafeFromString(d.getCurrentUrl))
      }
  }

  case object Trivago extends ComparisonSite {
    override def name: String = "Trivago"

    override def reportedRateType: ReportedRateType = ReportedRateType.Nightly

    override def waitToBeReadyCondition: RemoteWebDriver => IO[Boolean] =
      d =>
        IO(
          d.findElementsByClassName("item__best-price")
            .asScala
            .toList
            .exists(_.getText.headOption.contains('£')))


    override def getLowestPrice: RemoteWebDriver => IO[PriceDetails] =
      d =>
        IO {
          val result = for {
            topHotel <- d.findElements(By.xpath("//article[@data-qa='itemlist-element']")).asScala.headOption
            mainPrice <-  topHotel.findElements(By.className("item__best-price")).asScala.headOption.map(_.getText)
            mainPriceSeller <-  topHotel.findElements(By.className("item__deal-best-ota")).asScala.headOption.map(_.getText)
            otherPrices = topHotel.findElements(By.className("deals__price")).asScala.toList.map(_.getText)
            otherPricesSellers = topHotel.findElements(By.className("deal-other__advertiser")).asScala.toList.map(_.getText)
          } yield {
            val sellersAndPrices = List((mainPriceSeller, mainPrice)) ++ otherPricesSellers.zip(otherPrices)
            val (lowestSeller, lowestPrice) = sellersAndPrices.collect{case (seller, price) if price.headOption.contains('£') => (seller, price.drop(1).toInt)}.minBy{case (_, price) => price}
            PriceDetails(lowestSeller, lowestPrice, Uri.unsafeFromString(d.getCurrentUrl))
          }
          result.get //todo make safe
        }
  }

}
