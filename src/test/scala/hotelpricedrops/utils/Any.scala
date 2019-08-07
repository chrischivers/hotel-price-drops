package hotelpricedrops.utils

import java.time.{Instant, LocalDate}

import hotelpricedrops.Model.{Hotel, Result, Screenshot, Search, User}
import org.http4s.Uri

import scala.util.Random

object Any {
  def str = Random.alphanumeric.take(10).mkString
  def from[T](values: Seq[T]) = values(Random.nextInt(values.length))
  def uri = Uri.unsafeFromString(s"http://$str.com")
  def date: LocalDate =
    LocalDate.of(Any.from(1970 to 2020), Any.from(1 to 12), Any.from(1 to 28))
  def hotel = Hotel(str, Some(uri), Some(uri), Some(uri))
  def user = User(str, 1, Instant.now)
  def search = Search(date, date.plusDays(from(1 to 7).toLong), from(1 to 5))
  def result = Result(1, 1, from(50 to 150), "Kayak")
  def screenshot = Screenshot(Array.empty[Byte])
}
