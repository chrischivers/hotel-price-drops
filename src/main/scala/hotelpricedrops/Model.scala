package hotelpricedrops

import cats.effect.IO
import hotelpricedrops.pricefetchers.PriceFetcher.PriceFetcherSpec
import io.circe.{Decoder, Encoder}
import org.http4s.Uri
import io.circe.generic.semiauto._
import org.openqa.selenium.remote.RemoteWebDriver
import scala.collection.JavaConverters._

object Model {

  case class Screenshot(value: Array[Byte])

  case class PriceDetails(seller: String, price: Int, url: Uri)

  case class Hotel(name: String,
                   kayakUrl: Option[Uri],
                   skyscannerUrl: Option[Uri]) {
    def urlFor(comparisonSite: ComparisonSite) = {
      comparisonSite match {
        case ComparisonSite.Kayak      => kayakUrl
        case ComparisonSite.SkyScanner => skyscannerUrl
      }
    }
  }

  object Hotel {
    implicit val uriDecoder: Decoder[Uri] =
      Decoder.decodeString.map(Uri.unsafeFromString)
    implicit val uriEncoder: Encoder[Uri] =
      Encoder.encodeString.contramap[Uri](_.renderString)
    implicit val decoder: Decoder[Hotel] = deriveDecoder
    implicit val encoder: Encoder[Hotel] = deriveEncoder
  }

}
