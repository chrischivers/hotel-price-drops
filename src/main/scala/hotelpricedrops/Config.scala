package hotelpricedrops

import cats.effect.IO
import com.typesafe.config.ConfigFactory
import hotelpricedrops.notifier.EmailNotifier.EmailerConfig
import net.ceedubs.ficus.Ficus._
import net.ceedubs.ficus.readers.ArbitraryTypeReader._

object Config {

  case class Config(emailerConfig: EmailerConfig)

  def apply() = IO {
    val config = ConfigFactory.load()
    Config(
      config.as[EmailerConfig]("emailerConfig")
    )

  }
}
