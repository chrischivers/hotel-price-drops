package hotelpricedrops.notifier

import cats.effect.IO
import hotelpricedrops.Model.Screenshot
import hotelpricedrops.pricefetchers.PriceFetcher.ErrorString

trait Notifier {
  def priceNotify(message: String, screenshot: Screenshot): IO[Unit]
  def errorNotify(error: ErrorString, screenshot: Screenshot): IO[Unit]
}
