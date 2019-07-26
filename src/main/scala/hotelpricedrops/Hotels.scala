package hotelpricedrops

import cats.effect.IO
import cats.instances.list._
import cats.syntax.traverse._
import hotelpricedrops.Model.Hotel
import hotelpricedrops.pricefetchers.PriceFetcher
import hotelpricedrops.util._
import io.chrisdavenport.log4cats.Logger
import io.circe.parser._

import scala.io.Source
import cats.syntax.flatMap._

object Hotels {

  def getHotelsFromFile(fileName: String): IO[List[Hotel]] = {
    IO.fromEither {
      val rawJson = Source
        .fromResource(fileName)
        .getLines()
        .toList
        .mkString("\n")
      parse(rawJson).flatMap(_.as[List[Hotel]])
    }
  }

  def pricesForHotel(hotel: Hotel, priceFetchers: List[PriceFetcher])(
      implicit logger: Logger[IO]): IO[List[PriceFetcher.Results]] = {
    priceFetchers
      .traverse { fetcher =>
        fetcher
          .getPriceDetailsFor(hotel)
          .withRetry(3)
          .handleErrorWith { err =>
            logger.error(
              s"Error after retries for ${hotel.name} on ${fetcher.comparisonSite.name}. Error [$err]") >>
              IO.pure(None)
          }
      }
      .map(_.flatten)
  }
}
