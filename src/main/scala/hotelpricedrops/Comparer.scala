package hotelpricedrops

import cats.effect.IO
import hotelpricedrops.Model.{ComparisonSite, Hotel, PriceDetails}
import hotelpricedrops.db.DB
import cats.syntax.traverse._
import cats.syntax.functor._
import cats.syntax.flatMap._
import cats.instances.list._
import dev.profunktor.redis4cats.effect.Log
import hotelpricedrops.notifier.Notifier
import io.chrisdavenport.log4cats.Logger

object Comparer {
  def compare(
      db: DB,
      notifier: Notifier,
      fetchResults: List[(Hotel, List[(ComparisonSite, PriceDetails)])])(
      implicit logger: Logger[IO]): IO[Unit] = {

    def error(reason: String): IO[Unit] = {
      logger.error(reason) >>
        IO.raiseError(new RuntimeException(reason))
    }

    fetchResults.traverse {
      case (hotel, results) =>
        val lowestPriceInResults = results.sortBy(_._2.price).headOption
        lowestPriceInResults.fold[IO[Unit]](error("No price results found")) {
          case (comparisonSite, details) =>
            for {
              maybePreviousPrice <- db.fetch(hotel.name)
              _ <- maybePreviousPrice.fold(
                db.persist(hotel.name, details.price)) { previousPrice =>
                if (details.price < previousPrice) {
                  val msg =
                    s"Price for hotel ${hotel.name} dropping from #$previousPrice to £${details.price} \n(found on ${comparisonSite.name}"
                  logger.info(msg) >>
                    notifier.notify(msg) >>
                    db.persist(hotel.name, details.price)
                } else if (details.price > previousPrice) {
                  val msg =
                    s"Price for hotel ${hotel.name} increasing from £$previousPrice to £${details.price} \n(found on ${comparisonSite.name}"
                  logger.info(msg) >>
                    notifier.notify(msg) >>
                    db.persist(hotel.name, details.price)
                } else {
                  val msg =
                    s"Price for hotel ${hotel.name} staying the same at £${details.price}"
                  logger.info(msg)
                }

              }
            } yield ()
        }
    }.void
  }

}
