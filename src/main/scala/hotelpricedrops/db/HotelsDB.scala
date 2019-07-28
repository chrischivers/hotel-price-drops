package hotelpricedrops.db

import cats.effect.IO
import cats.syntax.functor._
import doobie.hikari.HikariTransactor
import hotelpricedrops.Model.{Hotel, Search}
import doobie.implicits._
import doobie.util.Meta
import hotelpricedrops.Model
import org.http4s.Uri

trait HotelsDB {
  def persistHotel(hotel: Hotel): IO[Unit]
  def hotelByName(hotelName: String): IO[Option[Hotel.WithId]]
  def allHotels: IO[List[Hotel.WithId]]
}

object HotelsDB {

  implicit private val uriMeta: Meta[Uri] =
    Meta[String].timap(Uri.unsafeFromString)(_.renderString)

  def apply(transactor: HikariTransactor[IO]): HotelsDB = new HotelsDB {
    def persistHotel(hotel: Hotel): IO[Unit] = {
      sql"""INSERT INTO hotels (hotel_name, kayak_url, skyscanner_url) VALUES
           |(${hotel.name}, ${hotel.kayakUrl}, ${hotel.skyscannerUrl})""".stripMargin.update.run
        .transact(transactor)
        .void
    }

    def hotelByName(hotelName: String): IO[Option[Hotel.WithId]] = {
      sql"""SELECT id, hotel_name, kayak_url, skyscanner_url FROM hotels
           |WHERE hotel_name = $hotelName
           |""".stripMargin.query[Hotel.WithId].option.transact(transactor)
    }

    override def allHotels: IO[List[Hotel.WithId]] = {
      sql"""SELECT id, hotel_name, kayak_url, skyscanner_url FROM hotels
           |""".stripMargin.query[Hotel.WithId].to[List].transact(transactor)
    }
  }
}
