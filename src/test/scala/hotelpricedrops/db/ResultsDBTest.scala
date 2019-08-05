package hotelpricedrops.db

import java.time.Instant

import cats.effect.IO
import doobie.hikari.HikariTransactor
import hotelpricedrops.utils.Any
import org.h2.jdbc.JdbcSQLIntegrityConstraintViolationException
import org.scalactic.TypeCheckedTripleEquals
import org.scalatest.{Matchers, WordSpec}

class ResultsDBTest
    extends WordSpec
    with TypeCheckedTripleEquals
    with Matchers {
  "Results DB" should {

    def setupFunction
      : HikariTransactor[IO] => (ResultsDB, SearchesDB, HotelsDB) =
      tx => (ResultsDB.apply(tx), SearchesDB.apply(tx), HotelsDB.apply(tx))

    "throw an sql exception on persisting if no search exists with the corresponding search id (foreign key)" in {
      assertThrows[JdbcSQLIntegrityConstraintViolationException] {
        Setup(setupFunction) {
          case (resultsDB, searchesDB, hotelsDB) =>
            val result = Any.result
            for {
              _ <- hotelsDB.persistHotel(Any.hotel)
              result <- resultsDB.persistResult(result)
            } yield {
              result should ===(())
            }
        }
      }
    }

    "throw an sql exception on persisting if no hotel exists with the corresponding hotel id (foreign key)" in {
      assertThrows[JdbcSQLIntegrityConstraintViolationException] {
        Setup(setupFunction) {
          case (resultsDB, searchesDB, hotelsDB) =>
            val result = Any.result
            for {
              _ <- searchesDB.persistSearch(Any.search)
              result <- resultsDB.persistResult(result)
            } yield {
              result should ===(())
            }
        }
      }
    }

    "persist a new result and retrieve it by using mostRecentPrice" in Setup(
      setupFunction
    ) {
      case (resultsDB, searchesDB, hotelsDB) =>
        val result = Any.result
        val now = Instant.now()
        for {
          _ <- searchesDB.persistSearch(Any.search)
          _ <- hotelsDB.persistHotel(Any.hotel)
          _ <- resultsDB.persistResult(result)
          resultFromDB <- resultsDB.mostRecentPriceFor(1, 1)
        } yield {
          resultFromDB.map(_.withoutIdAndTimestamp) should ===(Some(result))
        }
    }

    "mostRecentPrice chooses the most recent" in Setup(setupFunction) {
      case (resultsDB, searchesDB, hotelsDB) =>
        val result1 = Any.result
        val result2 = Any.result
        val now = Instant.now()
        for {
          _ <- searchesDB.persistSearch(Any.search)
          _ <- hotelsDB.persistHotel(Any.hotel)
          _ <- resultsDB.persistResult(result2)
          _ <- resultsDB.persistResult(result1)
          resultFromDB <- resultsDB.mostRecentPriceFor(1, 1)
        } yield {
          resultFromDB.map(_.withoutIdAndTimestamp) should ===(Some(result1))
        }
    }

    "lowestPriceSince chooses the lowest price" in Setup(setupFunction) {
      case (resultsDB, searchesDB, hotelsDB) =>
        val result1 = Any.result.copy(lowestPrice = 200)
        val result2 = Any.result.copy(lowestPrice = 100)
        val now = Instant.now()
        for {
          _ <- searchesDB.persistSearch(Any.search)
          _ <- hotelsDB.persistHotel(Any.hotel)
          _ <- resultsDB.persistResult(result2)
          _ <- resultsDB.persistResult(result1)
          resultFromDB <- resultsDB.lowestPriceSince(1, 1, now)
        } yield {
          resultFromDB.map(_.withoutIdAndTimestamp) should ===(Some(result2))
        }
    }

    "lowestPriceSince chooses the lowest price excluding lower prices before given date" in Setup(
      setupFunction
    ) {
      case (resultsDB, searchesDB, hotelsDB) =>
        val result1 = Any.result.copy(lowestPrice = 100)
        val result2 = Any.result.copy(lowestPrice = 200)
        val result3 = Any.result.copy(lowestPrice = 300)

        for {
          _ <- searchesDB.persistSearch(Any.search)
          _ <- hotelsDB.persistHotel(Any.hotel)
          _ <- resultsDB.persistResult(result1)
          now = Instant.now()
          _ <- resultsDB.persistResult(result2)
          _ <- resultsDB.persistResult(result3)
          resultFromDB <- resultsDB.lowestPriceSince(1, 1, now)
        } yield {
          resultFromDB.map(_.withoutIdAndTimestamp) should ===(Some(result2))
        }
    }
  }
}
