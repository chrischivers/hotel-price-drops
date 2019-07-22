package hotelpricedrops.db

import cats.effect.IO

trait DB {
  def persist(hotelName: String, lowestPrice: Int): IO[Unit]
  def fetch(hotelName: String): IO[Option[Int]]
}
