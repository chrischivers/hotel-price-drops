package hotelpricedrops.db

import java.time.Instant

import cats.effect.IO
import cats.syntax.functor._
import doobie.hikari.HikariTransactor
import doobie.implicits._
import hotelpricedrops.Model.Result

trait ResultsDB {
  def persistResult(result: Result): IO[Unit]
  def lowestPriceSince(searchId: Int,
                       hotelId: Int,
                       since: Instant): IO[Option[Result.WithIdAndTimestamp]]
  def mostRecentPriceFor(searchId: Int,
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

    override def lowestPriceSince(
      searchId: Int,
      hotelId: Int,
      since: Instant
    ): IO[Option[Result.WithIdAndTimestamp]] = {
      sql"""SELECT id, search_id, hotel_id, lowest_price, comparison_site_name, timestamp 
           |FROM results
           |WHERE search_id = ${searchId} 
           |AND hotel_id = ${hotelId}
           |AND timestamp >= ${since}
           |ORDER BY lowest_price ASC
           |LIMIT 1
           |""".stripMargin
        .query[Result.WithIdAndTimestamp]
        .option
        .transact(transactor)
    }

    override def mostRecentPriceFor(
      searchId: Int,
      hotelId: Int
    ): IO[Option[Result.WithIdAndTimestamp]] = {
      sql"""SELECT id, search_id, hotel_id, lowest_price, comparison_site_name, timestamp 
           |FROM results
           |WHERE search_id = ${searchId} AND hotel_id = ${hotelId}
           |ORDER BY timestamp DESC
           |LIMIT 1
           |""".stripMargin
        .query[Result.WithIdAndTimestamp]
        .option
        .transact(transactor)
    }
  }
}
