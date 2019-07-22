package hotelpricedrops

import cats.effect.IO
import com.typesafe.config.ConfigFactory
import hotelpricedrops.notifier.EmailNotifier.EmailerConfig
import net.ceedubs.ficus.Ficus._
import net.ceedubs.ficus.readers.ArbitraryTypeReader._

import scala.concurrent.duration.FiniteDuration

object Config {

  case class Config(emailerConfig: EmailerConfig,
                    geckoDriverPath: String,
                    firefoxBinaryPath: Option[String],
                    timeBetweenRuns: FiniteDuration,
                    pageLoadWaitTime: FiniteDuration)

  //TODO put into effect
  def apply() = {
    val config = ConfigFactory.load()
    Config(
      config.as[EmailerConfig]("emailerConfig"),
      config.as[String]("geckoDriverPath"),
      config.getAs[String]("firefoxBinaryPath"),
      config.as[FiniteDuration]("timeBetweenRuns"),
      config.as[FiniteDuration]("pageLoadWaitTime")
    )
  }
}
