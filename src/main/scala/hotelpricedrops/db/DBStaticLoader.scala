package hotelpricedrops.db

import cats.effect.IO
import cats.syntax.traverse._
import cats.syntax.functor._
import cats.instances.list._
import hotelpricedrops.Model.{Hotel, Search}
import io.circe.Decoder
import io.circe.parser.parse

import scala.io.Source

object DBStaticLoader {

  private val hotelsFileName = "hotel-list.json"
  private val searchesFileName = "searches-list.json"

  private def getFromFile[T](fileName: String)(
      implicit decoder: Decoder[T]): IO[List[T]] = {
    IO.fromEither {
      val rawJson = Source
        .fromResource(fileName)
        .getLines()
        .toList
        .mkString("\n")
      parse(rawJson).flatMap(_.as[List[T]])
    }
  }

  def populateSearches(searchesDB: SearchesDB): IO[Unit] = {
    getFromFile[Search](searchesFileName).flatMap { searchesFromFile =>
      searchesDB.allSearches.flatMap { existingSearchesWithId =>
        val existingSearchesWithoutId = existingSearchesWithId.map(_.withoutId)
        searchesFromFile.traverse { search =>
          for {
            _ <- if (existingSearchesWithoutId.contains(search)) IO.unit
            else searchesDB.persistSearch(search)
          } yield ()
        }.void
      }
    }
  }

  def populateHotels(hotelsDB: HotelsDB): IO[Unit] = {

    getFromFile[Hotel](hotelsFileName)
      .flatMap(_.traverse { hotel =>
        for {
          existing <- hotelsDB.hotelByName(hotel.name)
          _ <- if (existing.isDefined) IO.unit
          else hotelsDB.persistHotel(hotel)
        } yield ()

      })
      .void

  }

}