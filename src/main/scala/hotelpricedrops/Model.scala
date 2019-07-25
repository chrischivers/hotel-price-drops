package hotelpricedrops

import io.circe.{Decoder, Encoder}
import org.http4s.Uri
import io.circe.generic.semiauto._

object Model {

  case class ComparisonSite(name: String)
  case class PriceDetails(seller: String, price: Int, screenshot: Array[Byte])
  case class Hotel(name: String, kayakUrl: Uri)

  object Hotel {
    implicit val uriDecoder: Decoder[Uri] =
      Decoder.decodeString.map(Uri.unsafeFromString)
    implicit val uriEncoder: Encoder[Uri] =
      Encoder.encodeString.contramap[Uri](_.renderString)
    implicit val decoder: Decoder[Hotel] = deriveDecoder
    implicit val encoder: Encoder[Hotel] = deriveEncoder
  }

}
