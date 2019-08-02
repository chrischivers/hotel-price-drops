package hotelpricedrops

import cats.effect.IO
import cats.instances.list._
import cats.syntax.flatMap._
import cats.syntax.traverse._
import hotelpricedrops.Model.Hotel
import hotelpricedrops.pricefetchers.PriceFetcher
import hotelpricedrops.util._
import io.chrisdavenport.log4cats.Logger

object Hotels {

  def pricesForHotel(
    hotel: Hotel,
    priceFetchers: List[PriceFetcher],
    nights: Int
  )(implicit logger: Logger[IO]): IO[List[PriceFetcher.Results]] = {
    priceFetchers
      .traverse { fetcher =>
        fetcher
          .getPriceDetailsFor(hotel, nights)
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
