package hotelpricedrops.db

import cats.effect.IO
import cats.syntax.functor._
import doobie.hikari.HikariTransactor
import doobie.implicits._
import hotelpricedrops.Model
import hotelpricedrops.Model.Search

trait SearchesDB {
  def persistSearch(search: Search): IO[Unit]
  def allSearches: IO[List[Search.WithId]]
}

object SearchesDB {
  def apply(transactor: HikariTransactor[IO]): SearchesDB = new SearchesDB {
    override def persistSearch(search: Model.Search): IO[Unit] = {
      sql"""INSERT INTO searches (check_in_date, check_out_date, number_of_adults) VALUES
             |(${search.checkInDate}, ${search.checkOutDate}, ${search.numberOfAdults})""".stripMargin.update.run
        .transact(transactor)
        .void
    }

    override def allSearches: IO[List[Search.WithId]] = {
      sql"""SELECT id, check_in_date, check_out_date, number_of_adults FROM searches
             |""".stripMargin.query[Search.WithId].to[List].transact(transactor)
    }
  }
}
