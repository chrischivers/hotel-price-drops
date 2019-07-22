package hotelpricedrops.db

import cats.effect.{ContextShift, IO, Resource}
import dev.profunktor.redis4cats.algebra.RedisCommands
import dev.profunktor.redis4cats.connection.{RedisClient, RedisURI}
import dev.profunktor.redis4cats.domain.RedisCodec
import dev.profunktor.redis4cats.effect.Log
import dev.profunktor.redis4cats.interpreter.Redis

object RedisDB {

  private val stringCodec
    : RedisCodec[String, String] = RedisCodec.Utf8 //todo make int

  def redisClientResource(implicit cs: ContextShift[IO], log: Log[IO]) =
    for {
      uri <- Resource.liftF(RedisURI.make[IO]("redis://localhost"))
      client <- RedisClient[IO](uri)
      redis <- Redis[IO, String, String](client, stringCodec, uri)
    } yield redis

  def apply(redisCommands: RedisCommands[IO, String, String]) = new DB {
    override def persist(hotelName: String, lowestPrice: Int): IO[Unit] =
      redisCommands.set(hotelName, lowestPrice.toString)

    override def fetch(hotelName: String): IO[Option[Int]] =
      redisCommands.get(hotelName).map(_.map(_.toInt))
  }
}
