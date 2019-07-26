package hotelpricedrops

import cats.effect.IO
import cats.syntax.flatMap._
import hotelpricedrops.Config._
import hotelpricedrops.Model.{Hotel, PriceDetails, ReportedRateType, Screenshot}
import hotelpricedrops.db.DB
import hotelpricedrops.notifier.Notifier
import hotelpricedrops.pricefetchers.PriceFetcher
import io.chrisdavenport.log4cats.Logger

trait Comparer {
  def compareAndNotify(hotel: Hotel,
                       results: List[PriceFetcher.Results]): IO[Unit]
}

object Comparer {

  def apply(db: DB, notifier: Notifier, config: Config)(
      implicit logger: Logger[IO]) =
    new Comparer {
      override def compareAndNotify(
          hotel: Hotel,
          results: List[PriceFetcher.Results]): IO[Unit] = {

        def error(reason: String): IO[Unit] = {
          logger.error(reason) >>
            IO.raiseError(new RuntimeException(reason))
        }

        val resultsAdjustedForRateType = results.map { result =>
          result.comparisonSite.reportedRateType match {
            case ReportedRateType.Nightly => result
            case ReportedRateType.Entirety =>
              result.copy(
                priceDetails = result.priceDetails.copy(
                  price = result.priceDetails.price / 7)) //todo handle number of nights somewhere
          }
        }
        val lowestPriceInResults =
          resultsAdjustedForRateType.sortBy(_.priceDetails.price).headOption

        lowestPriceInResults.fold[IO[Unit]](error("No price results found")) {
          result =>
            for {
              maybePreviousPrice <- db.fetch(hotel.name)
              _ <- maybePreviousPrice.fold(
                db.persist(hotel.name, result.priceDetails.price)) {
                previousPrice =>
                  if (result.priceDetails.price < previousPrice) {
                    val msg =
                      s"Price for hotel ${hotel.name} dropping from £$previousPrice to £${result.priceDetails.price} " +
                        s"\nSeller: ${result.priceDetails.seller}" +
                        s"\nFound on: ${result.comparisonSite.name}" +
                        s"\nUrl: ${result.priceDetails.url.renderString}"
                    logger.info(msg) >>
                      (if (config.emailOnPriceDecrease)
                         notifier.priceNotify(msg, result.screenshot)
                       else IO.unit) >>
                      db.persist(hotel.name, result.priceDetails.price)
                  } else if (result.priceDetails.price > previousPrice) {
                    val msg =
                      s"Price for hotel ${hotel.name} increasing from £$previousPrice to £${result.priceDetails.price} " +
                        s"\nSeller: ${result.priceDetails.seller}" +
                        s"\nFound on: ${result.comparisonSite.name}" +
                        s"\nUrl: ${result.priceDetails.url.renderString}"
                    logger.info(msg) >>
                      (if (config.emailOnPriceIncrease)
                         notifier.priceNotify(msg, result.screenshot)
                       else IO.unit)
                    db.persist(hotel.name, result.priceDetails.price)
                  } else {
                    val msg =
                      s"Price for hotel ${hotel.name} staying the same at £${result.priceDetails.price}"
                    (if (config.emailOnPriceNoChange)
                       notifier.priceNotify(msg, result.screenshot)
                     else IO.unit) >> logger.info(msg)
                  }

              }
            } yield ()
        }
      }
    }

}
