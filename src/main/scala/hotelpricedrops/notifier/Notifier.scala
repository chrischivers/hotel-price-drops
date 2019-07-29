package hotelpricedrops.notifier

import cats.effect.IO
import hotelpricedrops.Model.Screenshot
import hotelpricedrops.pricefetchers.PriceFetcher.ErrorString
import org.http4s.Uri

trait Notifier {
  def priceNotify(priceNotification: PriceNotification,
                  screenshot: Screenshot): IO[Unit]
  def errorNotify(errorMsg: ErrorString, screenshot: Screenshot): IO[Unit]
}

case class PriceNotification(to: String,
                             hotelName: String,
                             previousLowestPrice: Int,
                             currentLowestPrice: Int,
                             seller: String,
                             comparisonSiteName: String,
                             url: Uri,
                             allTimeLowestPrice: Option[Int]) {

  private def priceMovementDesc =
    if (previousLowestPrice > currentLowestPrice)
      s"dropping from £${previousLowestPrice} to £${currentLowestPrice}"
    else if (previousLowestPrice < currentLowestPrice)
      s"increasing from £${previousLowestPrice} to £${currentLowestPrice}"
    else s"staying the same at £${currentLowestPrice}"

  def subject = s"Price Notification: ${hotelName} $priceMovementDesc"

  def toText =
    s"Price for hotel ${hotelName} $priceMovementDesc " +
      s"\nSeller: ${seller}" +
      s"\nFound on: ${comparisonSiteName}" +
      s"\nUrl: ${url}. " +
      s"\nAll time lowest price was ${allTimeLowestPrice
        .map(r => s"£${r}")
        .getOrElse("[Unknown]")}"

  def toHtml =
    s"""
       |<p>Price for hotel <strong>${hotelName}</strong> $priceMovementDesc </p>
       |<p>Seller: <strong>${seller} </strong></p>
       |<p>Found on: <strong>${comparisonSiteName} </strong>(<a href="${url}">url</a>)</p>
       |<p>All time lowest price was <strong>&pound;${allTimeLowestPrice
         .getOrElse("[Unknown]")}</strong></p>
       |""".stripMargin
}
