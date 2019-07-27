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
                    timeBetweenRuns: FiniteDuration,
                    emailOnPriceDecrease: Boolean,
                    emailOnPriceIncrease: Boolean,
                    emailOnPriceNoChange: Boolean,
                    screenshotOnError: Boolean)

  //TODO put into effect
  def apply() = {
    val config = ConfigFactory.load()
    Config(
      config.as[EmailerConfig]("emailerConfig"),
      config.as[String]("geckoDriverPath"),
      config.as[FiniteDuration]("timeBetweenRuns"),
      config.getAs[Boolean]("emailOnPriceDecrease").getOrElse(true),
      config.getAs[Boolean]("emailOnPriceIncrease").getOrElse(false),
      config.getAs[Boolean]("emailOnPriceNoChange").getOrElse(false),
      config.getAs[Boolean]("screenshotOnError").getOrElse(false)
    )
  }
}
