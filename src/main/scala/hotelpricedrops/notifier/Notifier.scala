package hotelpricedrops.notifier

import cats.effect.IO

trait Notifier {
  def notify(message: String, screenshot: Array[Byte]): IO[Unit]
}
