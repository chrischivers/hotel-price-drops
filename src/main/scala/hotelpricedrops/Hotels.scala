package hotelpricedrops

import cats.effect.{ContextShift, IO, Timer}
import cats.instances.list._
import cats.syntax.flatMap._
import cats.syntax.traverse._
import hotelpricedrops.Model.Hotel
import hotelpricedrops.pricefetchers.PriceFetcher
import hotelpricedrops.util._
import io.chrisdavenport.log4cats.Logger

import scala.concurrent.duration._

object Hotels {

  def pricesForHotel(
    hotel: Hotel,
    priceFetchers: List[PriceFetcher],
    nights: Int
  )(implicit logger: Logger[IO],
    timer: Timer[IO],
    contextShift: ContextShift[IO]): IO[List[PriceFetcher.Result]] = {
    priceFetchers
      .traverse { fetcher =>
        fetcher
          .getPriceDetailsFor(hotel, nights)
          .withTimeout(10.minutes)
          .withRetry(3)
          .handleErrorWith { err =>
            logger.error(
              s"Error after retries for ${hotel.name} on ${fetcher.comparisonSite.name}. Error [$err]"
            ) >>
              IO.pure(None)
          }
      }
      .map(_.flatten)
  }
}
