package hotelpricedrops.pricefetchers

import cats.effect.IO
import hotelpricedrops.ComparisonSite.{Kayak, SkyScanner, Trivago}
import hotelpricedrops.Model.{Hotel, PriceDetails, Screenshot}
import hotelpricedrops.utils.{MockWebDriver, MockWebElement}
import hotelpricedrops.{ComparisonSite, Main}
import org.http4s.Uri
import org.scalactic.TypeCheckedTripleEquals
import org.scalatest.{FunSuite, Matchers, WordSpec}

import scala.concurrent.ExecutionContext

class PriceFetcherTest
    extends WordSpec
    with TypeCheckedTripleEquals
    with Matchers {

  val comparisonSites = List(
    ComparisonSite.Kayak,
    ComparisonSite.SkyScanner,
    ComparisonSite.Trivago
  )

  "Price fetcher" should {
    "getPriceDetailsFor" should {
      comparisonSites.foreach { comparisonSite =>
        s"should get the price details for ${comparisonSite.name}" in new Setup(
          comparisonSite
        ) {
          priceFetcher
            .getPriceDetailsFor(testHotel, 1)
            .unsafeRunSync() should ===(
            Some(
              PriceFetcher.Result(
                comparisonSite,
                PriceDetails("Unknown", 100, url),
                Screenshot(Array.empty[Byte])
              )
            )
          )
        }
      }
    }
  }

  class Setup(comparisonSite: ComparisonSite) {
    implicit val executionContext = ExecutionContext.global
    implicit val timer = IO.timer(executionContext)
    implicit val logger = Main.logger

    val url = Uri.unsafeFromString("https://testul.com/hotel")

    val testHotel = Hotel("Test Hotel", Some(url), Some(url), Some(url))

    val kayakMockWebDriver = MockWebDriver(
      elementsByClassName = Map(
        "provider" -> List(
          MockWebElement(
            attributes = Map("id" -> "Unknown"),
            elementsByClassName =
              Map("price" -> List(MockWebElement(textOpt = Some("£100"))))
          ),
          MockWebElement(
            attributes = Map("id" -> "Agoda"),
            elementsByClassName =
              Map("price" -> List(MockWebElement(textOpt = Some("£150"))))
          )
        )
      )
    )

    val skyscannerMockWebDriver = MockWebDriver(
      elementsByXPath = Map(
        "//span[@data-test-id='main-cta-price']" -> List(
          MockWebElement(textOpt = Some("£100")),
          MockWebElement(textOpt = Some("£150"))
        )
      )
    )

    val trivagoMockWebDriver = MockWebDriver(
      elementsByXPath = Map(
        "//article[@data-qa='itemlist-element']" -> List(
          MockWebElement(
            elementsByClassName = Map(
              "item__best-price" -> List(
                MockWebElement(textOpt = Some("£180"))
              ),
              "item__deal-best-ota" -> List(
                MockWebElement(textOpt = Some("Agoda"))
              ),
              "deals__price" -> List(
                MockWebElement(textOpt = Some("£150")),
                MockWebElement(textOpt = Some("£100"))
              ),
              "deal-other__advertiser" -> List(
                MockWebElement(textOpt = Some("Booking")),
                MockWebElement(textOpt = Some("Unknown"))
              )
            )
          )
        )
      )
    )

    val webDriver = comparisonSite match {
      case Kayak      => kayakMockWebDriver
      case SkyScanner => skyscannerMockWebDriver
      case Trivago    => trivagoMockWebDriver
    }

    val priceFetcher = PriceFetcher(
      webDriver,
      comparisonSite,
      screenshotOnError = false,
      (_, _) => IO.unit
    )
  }
}
