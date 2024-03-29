package hotelpricedrops

import java.time.{Instant, LocalDate}
import java.time.temporal.ChronoUnit.DAYS
import io.circe.generic.semiauto._
import io.circe.{Decoder, Encoder}
import org.http4s.Uri

object Model {

  case class Screenshot(value: Array[Byte]) {
    override def equals(obj: Any): Boolean = obj match {
      case Screenshot(arr) =>
        arr.toList == value.toList //required to assert equality in testing
      case _ => false
    }
  }

  case class PriceDetails(seller: String, price: Int, url: Uri)

  sealed trait ReportedRateType

  object ReportedRateType {

    case object Nightly extends ReportedRateType

    case object Entirety extends ReportedRateType

  }

  case class Hotel(name: String,
                   kayakUrl: Option[Uri],
                   skyscannerUrl: Option[Uri],
                   trivagoUrl: Option[Uri]) {

    def withId(id: Int): Hotel.WithId =
      Hotel.WithId(id, name, kayakUrl, skyscannerUrl, trivagoUrl)

    def urlFor(comparisonSite: ComparisonSite) = {
      comparisonSite match {
        case ComparisonSite.Kayak      => kayakUrl
        case ComparisonSite.SkyScanner => skyscannerUrl
        case ComparisonSite.Trivago    => trivagoUrl
      }
    }
  }

  object Hotel {

    case class WithId(hotelId: Int,
                      name: String,
                      kayakUrl: Option[Uri],
                      skyscannerUrl: Option[Uri],
                      trivagoUrl: Option[Uri]) {
      def withoutid = Hotel(name, kayakUrl, skyscannerUrl, trivagoUrl)
    }

    implicit val uriDecoder: Decoder[Uri] =
      Decoder.decodeString.map(Uri.unsafeFromString)
    implicit val uriEncoder: Encoder[Uri] =
      Encoder.encodeString.contramap[Uri](_.renderString)
    implicit val decoder: Decoder[Hotel] = deriveDecoder
    implicit val encoder: Encoder[Hotel] = deriveEncoder
  }

  case class Search(checkInDate: LocalDate,
                    checkOutDate: LocalDate,
                    numberOfAdults: Int) {
    def withId(id: Int) =
      Search.WithId(id, checkInDate, checkOutDate, numberOfAdults)
    def numberOfNights = DAYS.between(checkInDate, checkOutDate).toInt
  }

  object Search {
    case class WithId(searchId: Int,
                      checkInDate: LocalDate,
                      checkOutDate: LocalDate,
                      numberOfAdults: Int) {
      def withoutId = Search(checkInDate, checkOutDate, numberOfAdults)
    }

    import io.circe.java8.time.decodeLocalDate
    import io.circe.java8.time.encodeLocalDate
    implicit val decoder: Decoder[Search] = deriveDecoder
    implicit val encoder: Encoder[Search] = deriveEncoder
  }

  case class Result(searchId: Int,
                    hotelId: Int,
                    lowestPrice: Int,
                    comparisonSiteName: String)

  object Result {
    case class WithIdAndTimestamp(resultId: Int,
                                  searchId: Int,
                                  hotelId: Int,
                                  lowestPrice: Int,
                                  comparisonSiteName: String,
                                  timestamp: Instant) {
      def withoutIdAndTimestamp =
        Result(searchId, hotelId, lowestPrice, comparisonSiteName)
    }
  }

  case class User(emailAddress: String, searchId: Int, startDate: Instant)

  object User {
    implicit val decoder: Decoder[User] = deriveDecoder
    implicit val encoder: Encoder[User] = deriveEncoder
  }

}
