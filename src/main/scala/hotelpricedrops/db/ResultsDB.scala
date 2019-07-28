package hotelpricedrops.db

import cats.effect.IO
import hotelpricedrops.Model.{Result, Search}
import cats.effect.IO
import cats.syntax.functor._
import doobie.hikari.HikariTransactor
import hotelpricedrops.Model
import hotelpricedrops.Model.{Hotel, Search}
import doobie.implicits._

trait ResultsDB {
  def persistResult(result: Result): IO[Unit]
  def lowestPriceFor(searchId: Int,
                     hotelId: Int): IO[Option[Result.WithIdAndTimestamp]]
}

object ResultsDB {
  def apply(transactor: HikariTransactor[IO]): ResultsDB = new ResultsDB {

    override def persistResult(result: Result): IO[Unit] = {
      sql"""INSERT INTO results (search_id, hotel_id, lowest_price, comparison_site_name) VALUES
           |(${result.searchId}, ${result.hotelId}, ${result.lowestPrice}, ${result.comparisonSiteName})""".stripMargin.update.run
        .transact(transactor)
        .void
    }

    override def lowestPriceFor(
        searchId: Int,
        hotelId: Int): IO[Option[Result.WithIdAndTimestamp]] = {
      sql"""SELECT id, search_id, hotel_id, lowest_price, comparison_site_name, timestamp 
           |FROM results
           |WHERE search_id = ${searchId} AND hotel_id = ${hotelId}
           |ORDER BY lowest_price ASC
           |LIMIT 1
           |""".stripMargin
        .query[Result.WithIdAndTimestamp]
        .option
        .transact(transactor)
    }
  }
}
