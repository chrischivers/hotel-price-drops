package hotelpricedrops

import cats.data.OptionT
import cats.effect.IO
import cats.instances.list._
import cats.syntax.traverse._
import hotelpricedrops.Model.{PriceDetails, ReportedRateType}
import hotelpricedrops.selenium.{WebDriver, WebElement}

sealed trait ComparisonSite {
  def name: String
  def reportedRateType: ReportedRateType
  def waitToBeReadyCondition: WebDriver => IO[Boolean]
  def getLowestPrice: WebDriver => IO[PriceDetails]
}

object ComparisonSite {

  case object Kayak extends ComparisonSite {
    override def name: String = "Kayak"

    override def reportedRateType: ReportedRateType = ReportedRateType.Nightly

    override def waitToBeReadyCondition: WebDriver => IO[Boolean] =
      d =>
        for {
          priceElems <- d.findElementsByClassName("price")
          prices <- priceElems.traverse(_.text)
        } yield prices.exists(_.headOption.contains('£'))

    override def getLowestPrice: WebDriver => IO[PriceDetails] =
      driver =>
        for {
          elements <- driver.findElementsByClassName("provider")
          sellerPriceList <- elements
            .traverse(priceFromProviderElement)
            .map(_.flatten)
          (lowestPriceSeller, lowestPrice) = sellerPriceList
            .minBy { //todo unsafe
              case (_, price) => price
            }
          currentUrl <- driver.getCurrentUrl
        } yield PriceDetails(lowestPriceSeller, lowestPrice, currentUrl)

    private def priceFromProviderElement(
      element: WebElement
    ): IO[Option[(String, Int)]] = {

      for {
        seller <- element.getAttribute("id").map(_.trim)
        price <- if (seller.nonEmpty) {
          element
            .findElementByClassName("price")
            .flatMap(_.text)
            .flatMap(str => IO(Some(str.drop(1).toInt)))
        } else IO(None)

      } yield price.map((seller, _))
    }
  }

  case object SkyScanner extends ComparisonSite {
    override def name: String = "SkyScanner"

    override def reportedRateType: ReportedRateType =
      ReportedRateType.Entirety

    override def waitToBeReadyCondition: WebDriver => IO[Boolean] =
      d =>
        for {
          elems <- d.findElementsByXPath(
            "//span[@data-test-id='main-cta-price']"
          )
          elemsText <- elems.traverse(_.text)
        } yield elemsText.exists(_.headOption.contains('£'))

    override def getLowestPrice: WebDriver => IO[PriceDetails] =
      d => {

        for {
          elemsByXPath <- d.findElementsByXPath(
            "//span[@data-test-id='main-cta-price']"
          )
          elemsText <- elemsByXPath.traverse(_.text)
          filteredText = elemsText.filter(_.headOption.contains('£'))
          trimmedPrices <- filteredText.traverse(str => IO(str.drop(1).toInt))
          lowestPrice <- IO(trimmedPrices.min)
          url <- d.getCurrentUrl
        } yield PriceDetails("Unknown", lowestPrice, url)
      }
  }

  case object Trivago extends ComparisonSite {
    override def name: String = "Trivago"

    override def reportedRateType: ReportedRateType = ReportedRateType.Nightly

    override def waitToBeReadyCondition: WebDriver => IO[Boolean] =
      d =>
        for {
          elems <- d.findElementsByClassName("item__best-price")
          elemsText <- elems.traverse(_.text)
        } yield elemsText.exists(_.headOption.contains('£'))

    override def getLowestPrice: WebDriver => IO[PriceDetails] =
      d => {
        val result = for {
          topHotel <- OptionT(
            d.findElementsByXPath("//article[@data-qa='itemlist-element']")
              .map(_.headOption)
          )
          mainPriceElem <- OptionT(
            topHotel
              .findElementsByClassName("item__best-price")
              .map(_.headOption)
          )
          mainPriceElemText <- OptionT.liftF(mainPriceElem.text)
          mainPriceSellerElem <- OptionT(
            topHotel
              .findElementsByClassName("item__deal-best-ota")
              .map(_.headOption)
          )
          mainPriceSellerElemText <- OptionT.liftF(mainPriceSellerElem.text)
          otherPricesElems <- OptionT.liftF(
            topHotel
              .findElementsByClassName("deals__price")
          )
          otherPricesElemsText <- OptionT.liftF(
            otherPricesElems.traverse(_.text)
          )
          otherPricesSellersElems <- OptionT.liftF(
            topHotel
              .findElementsByClassName("deal-other__advertiser")
          )
          otherPricesSellersElemsText <- OptionT.liftF(
            otherPricesSellersElems.traverse(_.text)
          )
          currentUrl <- OptionT.liftF(d.getCurrentUrl)

        } yield {
          val sellersAndPrices = List(
            (mainPriceSellerElemText, mainPriceElemText)
          ) ++ otherPricesSellersElemsText
            .zip(otherPricesElemsText)
          val (lowestSeller, lowestPrice) = sellersAndPrices
            .collect {
              case (seller, price) if price.headOption.contains('£') =>
                (seller, price.drop(1).toInt)
            }
            .minBy { case (_, price) => price }
          PriceDetails(lowestSeller, lowestPrice, currentUrl)
        }
        result.value.flatMap {
          case Some(result) => IO.pure(result)
          case None =>
            IO.raiseError(new RuntimeException("Unable to obtain lowest price"))
        }
      }
  }
}
