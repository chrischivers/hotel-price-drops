package hotelpricedrops

import com.typesafe.config.ConfigFactory
import hotelpricedrops.db.DB
import hotelpricedrops.notifier.EmailNotifier.EmailerConfig
import hotelpricedrops.notifier.PriceNotification.PriceNotificationConfig
import net.ceedubs.ficus.Ficus._
import net.ceedubs.ficus.readers.ArbitraryTypeReader._

import scala.concurrent.duration.FiniteDuration

object Config {

  case class Config(emailerConfig: EmailerConfig,
                    dbConfig: DB.Config,
                    geckoDriverPath: String,
                    timeBetweenRuns: FiniteDuration,
                    priceNotificationConfig: PriceNotificationConfig)

  //TODO put into effect
  def apply() = {
    val config = ConfigFactory.load()
    Config(
      config.as[EmailerConfig]("emailerConfig"),
      config.as[DB.Config]("dbConfig"),
      config.as[String]("geckoDriverPath"),
      config.as[FiniteDuration]("timeBetweenRuns"),
      PriceNotificationConfig(
        config
          .getAs[Boolean]("priceNotification.emailOnAllPriceDecreases")
          .getOrElse(false),
        config
          .getAs[Boolean]("priceNotification.emailOnAllPriceIncreases")
          .getOrElse(false),
        config
          .getAs[Boolean]("priceNotification.emailOnPriceNoChange")
          .getOrElse(false),
        config
          .getAs[Boolean]("priceNotification.emailOnLowestPriceSinceCreated")
          .getOrElse(true),
        config
          .getAs[Boolean]("priceNotification.emailScreenshotOnError")
          .getOrElse(false)
      )
    )
  }
}
