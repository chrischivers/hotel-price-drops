package hotelpricedrops

import cats.effect.IO
import hotelpricedrops.Model.{ComparisonSite, Hotel, PriceDetails}
import hotelpricedrops.pricefetchers.{KayakPriceFetcher, PriceFetcher}
import io.circe.syntax._
import io.circe.parser._
import cats.syntax.traverse._
import cats.instances.list._

import scala.io.Source

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

  def pricesForHotel(hotel: Hotel, priceFetchers: List[PriceFetcher])
    : IO[List[(ComparisonSite, PriceDetails)]] = {
    priceFetchers.traverse { fetcher =>
      fetcher.getPriceDetailsFor(hotel).map((fetcher.comparisonSite, _))
    }
  }
}
