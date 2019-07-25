package hotelpricedrops

import cats.effect.IO
import hotelpricedrops.Model.{ComparisonSite, Hotel, PriceDetails}
import hotelpricedrops.db.DB
import cats.syntax.traverse._
import cats.syntax.functor._
import cats.syntax.flatMap._
import cats.instances.list._
import dev.profunktor.redis4cats.effect.Log
import hotelpricedrops.Config._
import hotelpricedrops.notifier.Notifier
import io.chrisdavenport.log4cats.Logger

trait Comparer {
  def compareAndNotify(hotel: Hotel,
                       results: List[(ComparisonSite, PriceDetails)]): IO[Unit]
}

object Comparer {

  def apply(db: DB, notifier: Notifier, config: Config)(
      implicit logger: Logger[IO]) =
    new Comparer {
      override def compareAndNotify(
          hotel: Hotel,
          results: List[(ComparisonSite, PriceDetails)]): IO[Unit] = {

        def error(reason: String): IO[Unit] = {
          logger.error(reason) >>
            IO.raiseError(new RuntimeException(reason))
        }

        val lowestPriceInResults = results.sortBy(_._2.price).headOption
        lowestPriceInResults.fold[IO[Unit]](error("No price results found")) {
          case (comparisonSite, details) =>
            for {
              maybePreviousPrice <- db.fetch(hotel.name)
              _ <- maybePreviousPrice.fold(
                db.persist(hotel.name, details.price)) { previousPrice =>
                if (details.price < previousPrice) {
                  val msg =
                    s"Price for hotel ${hotel.name} dropping from £$previousPrice to £${details.price} \n(found on ${comparisonSite.name}"
                  logger.info(msg) >>
                    (if (config.emailOnPriceDecrease)
                       notifier.notify(msg, details.screenshot)
                     else IO.unit) >>
                    db.persist(hotel.name, details.price)
                } else if (details.price > previousPrice) {
                  val msg =
                    s"Price for hotel ${hotel.name} increasing from £$previousPrice to £${details.price} \n(found on ${comparisonSite.name}"
                  logger.info(msg) >>
                    (if (config.emailOnPriceIncrease)
                       notifier.notify(msg, details.screenshot)
                     else IO.unit)
                  db.persist(hotel.name, details.price)
                } else {
                  val msg =
                    s"Price for hotel ${hotel.name} staying the same at £${details.price}"
                  (if (config.emailOnPriceNoChange)
                     notifier.notify(msg, details.screenshot)
                   else IO.unit) >> logger.info(msg)
                }

              }
            } yield ()
        }
      }
    }

}
