package hotelpricedrops.db

import cats.effect.ContextShift
import cats.syntax.functor._
import doobie.util.ExecutionContexts

object DB {

  case class Config(driver: String,
                    host: String,
                    port: Int,
                    user: String,
                    password: String,
                    dbName: String,
                    maximumPoolSize: Int = 2)

  import cats.effect.{IO, Resource}
  import doobie.hikari.HikariTransactor
  import HikariTransactor.newHikariTransactor
  import org.flywaydb.core.Flyway

  def transactorResource(config: Config)(
    implicit contextShift: ContextShift[IO]
  ): Resource[IO, HikariTransactor[IO]] =
    for {
      connectEC <- ExecutionContexts.fixedThreadPool[IO](32) // connect EC
      transactEC <- ExecutionContexts.cachedThreadPool[IO] // transaction EC
      url <- Resource.liftF(config.driver match {
        case "org.postgresql.Driver" =>
          IO(
            s"jdbc:postgresql://${config.host}:${config.port}/${config.dbName}"
          )
        case "org.h2.Driver" => IO(config.host)
        case other =>
          IO.raiseError(
            new RuntimeException(s"Unsupported database driver $other")
          )
      })
      transactor <- newHikariTransactor[IO](
        config.driver,
        url,
        config.user,
        config.password,
        connectEC,
        transactEC
      ).evalMap { tx =>
        tx.configure { dataSource =>
            IO {
              dataSource.setMaximumPoolSize(config.maximumPoolSize)
              val flyway = new Flyway()
              flyway.setDataSource(dataSource)
              flyway.setLocations("db/migration")
              flyway.migrate()
            }
          }
          .as(tx)
      }
    } yield transactor
}
