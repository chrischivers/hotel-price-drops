package hotelpricedrops.pricefetchers

import cats.effect.IO
import hotelpricedrops.ComparisonSite.Kayak
import hotelpricedrops.Model.{Hotel, PriceDetails, Screenshot}
import hotelpricedrops.utils.MockWebDriver
import hotelpricedrops.{ComparisonSite, Main}
import org.http4s.Uri
import org.scalactic.TypeCheckedTripleEquals
import org.scalatest.{FunSuite, Matchers, WordSpec}

import scala.concurrent.ExecutionContext

class PriceFetcherTest
    extends WordSpec
    with TypeCheckedTripleEquals
    with Matchers {

  "Price fetcher" should {
    "getPriceDetailsFor" should {
      "should get the price details for Kayak" in new SetUp(Kayak) {
        priceFetcher
          .getPriceDetailsFor(testHotel, 1)
          .unsafeRunSync() should ===(
          Some(
            PriceFetcher.Results(
              Kayak,
              PriceDetails("Agoda", 100, url),
              Screenshot(Array.empty[Byte])
            )
          )
        )
      }

    }
  }

  class SetUp(comparisonSite: ComparisonSite) {
    implicit val executionContext = ExecutionContext.global
    implicit val timer = IO.timer(executionContext)
    implicit val logger = Main.logger

    val url = Uri.unsafeFromString("https://testul.com/hotel")

    val testHotel = Hotel("Test Hotel", kayakUrl = Some(url), None, None)

    val mockWebDriver = MockWebDriver()

    val priceFetcher = PriceFetcher(
      mockWebDriver,
      comparisonSite,
      screenshotOnError = false,
      (_, _) => IO.unit
    )
  }
}
