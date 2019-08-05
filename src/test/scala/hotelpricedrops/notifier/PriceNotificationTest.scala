package hotelpricedrops.notifier

import hotelpricedrops.utils.Any
import org.scalactic.TypeCheckedTripleEquals
import org.scalatest.{FunSuite, Matchers, WordSpec}

class PriceNotificationTest
    extends WordSpec
    with TypeCheckedTripleEquals
    with Matchers {
  "price notification" should {
    "render description correctly" when {

      val hotelName = Any.str
      val seller = Any.str
      val comparisonSiteName = Any.str
      val uri = Any.uri

      "price decreasing" in {
        val pn = PriceNotification(
          "",
          hotelName,
          100,
          90,
          seller,
          comparisonSiteName,
          uri,
          Some(90)
        )
        List(pn.toText, pn.toHtml).foreach { str =>
          str should include("dropping from £100 to £90")
        }
      }

      "price increasing" in {
        val pn = PriceNotification(
          "",
          hotelName,
          90,
          100,
          seller,
          comparisonSiteName,
          uri,
          Some(90)
        )
        List(pn.toText, pn.toHtml, pn.subject).foreach { str =>
          str should include("increasing from £90 to £100")
        }
      }

      "price staying the same" in {
        val pn = PriceNotification(
          "",
          hotelName,
          90,
          90,
          seller,
          comparisonSiteName,
          uri,
          Some(90)
        )
        List(pn.toText, pn.toHtml, pn.subject).foreach { str =>
          str should include("staying the same at £90")
        }
      }

      "is all time lowest price" in {
        val pn = PriceNotification(
          "",
          hotelName,
          100,
          90,
          seller,
          comparisonSiteName,
          uri,
          Some(100)
        )
        List(pn.toText, pn.toHtml, pn.subject).foreach { str =>
          str should include("dropping from £100 to £90")
          str should include("ALL TIME LOWEST PRICE")
        }
      }
    }
  }
}
