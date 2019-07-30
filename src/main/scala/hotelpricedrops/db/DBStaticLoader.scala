package hotelpricedrops.db

import cats.effect.IO
import cats.syntax.traverse._
import cats.syntax.functor._
import cats.instances.list._
import hotelpricedrops.Model.{Hotel, Search, User}
import io.circe.Decoder
import io.circe.parser.parse

import scala.io.Source

object DBStaticLoader {

  private val hotelsFileName = "hotel-list.json"
  private val searchesFileName = "searches-list.json"
  private val userFileName = "user-list.json"

  private def getFromFile[T](
    fileName: String
  )(implicit decoder: Decoder[T]): IO[List[T]] = {
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
          _ <- existing.fold(hotelsDB.persistHotel(hotel))(
            e => hotelsDB.updateHotel(e.hotelId, hotel)
          )
        } yield ()
      })
      .void
  }

  def populateUsers(usersDB: UsersDB): IO[Unit] = {

    getFromFile[User](userFileName)
      .flatMap(_.traverse { user =>
        for {
          existing <- usersDB.usersFor(user.emailAddress, user.searchId)
          _ <- if (existing.nonEmpty) IO.unit
          else usersDB.persistUser(user)
        } yield ()

      })
      .void
  }

}
