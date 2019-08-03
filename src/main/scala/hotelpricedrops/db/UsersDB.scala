package hotelpricedrops.db

import cats.effect.IO
import cats.syntax.functor._
import doobie.hikari.HikariTransactor
import doobie.implicits._
import hotelpricedrops.Model.User

trait UsersDB {
  def persistUser(user: User): IO[Unit]
  def usersFor(searchId: Int): IO[List[User]]
  def usersFor(emailAddress: String, searchId: Int): IO[List[User]]
}
object UsersDB {
  def apply(transactor: HikariTransactor[IO]): UsersDB = new UsersDB {

    override def persistUser(user: User): IO[Unit] = {
      sql"""INSERT INTO users (email_address, search_id, start_date) VALUES
           |(${user.emailAddress}, ${user.searchId}, ${user.startDate})""".stripMargin.update.run
        .transact(transactor)
        .void
    }

    override def usersFor(searchId: Int): IO[List[User]] = {
      sql"""SELECT email_address, search_id, start_date FROM users
           |WHERE search_id = $searchId
           |""".stripMargin.query[User].to[List].transact(transactor)
    }

    override def usersFor(emailAddress: String,
                          searchId: Int): IO[List[User]] = {
      sql"""SELECT email_address, search_id, start_date FROM users
           |WHERE email_address = $emailAddress
           |AND search_id = $searchId
           |""".stripMargin.query[User].to[List].transact(transactor)
    }
  }
}
