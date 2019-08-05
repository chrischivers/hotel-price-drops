package hotelpricedrops

import java.util.UUID

import cats.effect.IO
import doobie.hikari.HikariTransactor
import org.scalatest.Assertion

import scala.concurrent.ExecutionContext

package object db {
  def h2DbConfig =
    DB.Config(
      driver = "org.h2.Driver",
      host =
        s"jdbc:h2:mem:${UUID.randomUUID()};MODE=PostgreSQL;DB_CLOSE_DELAY=-1;MODE=PostgreSQL",
      5432,
      "",
      "",
      "hpd"
    )

  object Setup {
    implicit val executionContext = ExecutionContext.global
    implicit val contextShift = IO.contextShift(executionContext)
    implicit val logger = Main.logger

    def apply[T](tx: HikariTransactor[IO] => T)(f: T => IO[Assertion]) = {

      DB.transactorResource(h2DbConfig)
        .use { tr =>
          f(tx(tr))
        }
        .unsafeRunSync()
    }
  }
}
