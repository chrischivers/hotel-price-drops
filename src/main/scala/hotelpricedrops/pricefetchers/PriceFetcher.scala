package hotelpricedrops.pricefetchers

import cats.effect.{IO, Timer}
import hotelpricedrops.Model.{ComparisonSite, Hotel, PriceDetails}
import org.openqa.selenium.By
import org.openqa.selenium.remote.RemoteWebDriver

import scala.concurrent.duration._



trait PriceFetcher {

  def comparisonSite: ComparisonSite

  def getPriceDetailsFor(hotel: Hotel): IO[PriceDetails]

}
