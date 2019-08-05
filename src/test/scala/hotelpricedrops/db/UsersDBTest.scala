package hotelpricedrops.db

import cats.effect.IO
import doobie.hikari.HikariTransactor
import hotelpricedrops.utils.Any
import org.h2.jdbc.JdbcSQLIntegrityConstraintViolationException
import org.scalactic.TypeCheckedTripleEquals
import org.scalatest.{Matchers, WordSpec}

class UsersDBTest extends WordSpec with TypeCheckedTripleEquals with Matchers {
  "Users DB" should {

    def setupFunction: HikariTransactor[IO] => (UsersDB, SearchesDB) =
      tx => (UsersDB.apply(tx), SearchesDB.apply(tx))

    "throw an sql exception on persisting if no search exists with the corresponding search id (foreign key)" in {

      assertThrows[JdbcSQLIntegrityConstraintViolationException] {
        Setup(setupFunction) {
          case (usersDB, searchesDB) =>
            val user = Any.user
            for {
              result <- usersDB.persistUser(user)
            } yield {
              result should ===(())
            }
        }
      }
    }
    "persist a new user and retrieve it by search id" in Setup(setupFunction) {
      case (usersDB, searchesDB) =>
        val user = Any.user
        for {
          _ <- searchesDB.persistSearch(Any.search)
          _ <- usersDB.persistUser(user)
          userFromDB <- usersDB.usersFor(user.searchId)
        } yield {
          userFromDB should ===(List(user))
        }
    }
    "look up user by search id and email address" in Setup(setupFunction) {
      case (usersDB, searchesDB) =>
        val user = Any.user
        for {
          _ <- searchesDB.persistSearch(Any.search)
          _ <- usersDB.persistUser(user)
          userFromDB <- usersDB.usersFor(user.emailAddress, user.searchId)
        } yield {
          userFromDB should ===(List(user))
        }
    }
  }
}
