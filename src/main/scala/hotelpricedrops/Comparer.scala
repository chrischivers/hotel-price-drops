package hotelpricedrops

import cats.effect.IO
import cats.syntax.flatMap._
import hotelpricedrops.Config._
import hotelpricedrops.Model.{Hotel, ReportedRateType, Result, Search, User}
import hotelpricedrops.db.ResultsDB
import hotelpricedrops.notifier.PriceNotification.PriceNotificationConfig
import hotelpricedrops.notifier.{Notifier, PriceNotification}
import hotelpricedrops.pricefetchers.PriceFetcher
import io.chrisdavenport.log4cats.Logger

trait Comparer {
  def compareAndNotify(hotel: Hotel.WithId,
                       search: Search.WithId,
                       user: User,
                       results: List[PriceFetcher.Results]): IO[Unit]
}

object Comparer {

  def apply(
    resultsDB: ResultsDB,
    notifier: Notifier,
    config: PriceNotificationConfig
  )(implicit logger: Logger[IO]): Comparer =
    new Comparer {
      override def compareAndNotify(
        hotel: Hotel.WithId,
        search: Search.WithId,
        user: User,
        results: List[PriceFetcher.Results]
      ): IO[Unit] = {

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
                  price = result.priceDetails.price / search.withoutId.numberOfNights
                )
              )
          }
        }
        val lowestPriceInResults =
          resultsAdjustedForRateType.sortBy(_.priceDetails.price).headOption

        lowestPriceInResults.fold[IO[Unit]](error("No price results found")) {
          result =>
            for {
              allTimeLowestPriceResult <- resultsDB.lowestPriceFor(
                search.searchId,
                hotel.hotelId,
                user.startDate
              )
              mostRecentLowestPriceResult <- resultsDB.mostRecentLowestPriceFor(
                search.searchId,
                hotel.hotelId
              )
              resultRecord = Result(
                search.searchId,
                hotel.hotelId,
                result.priceDetails.price,
                result.comparisonSite.name
              )

              _ <- mostRecentLowestPriceResult.fold(IO.unit) { previousResult =>
                val priceNotification = PriceNotification(
                  user.emailAddress,
                  hotel.name,
                  previousResult.lowestPrice,
                  result.priceDetails.price,
                  result.priceDetails.seller,
                  result.comparisonSite.name,
                  result.priceDetails.url,
                  allTimeLowestPriceResult.map(_.lowestPrice)
                )

                def maybeNotify(predicate: Boolean): IO[Boolean] = {
                  if (predicate)
                    notifier.priceNotify(priceNotification, result.screenshot) >> IO
                      .pure(predicate)
                  else IO.pure(predicate)
                }

                for {
                  _ <- logger.info(priceNotification.toText)
                  priceDropNotified <- maybeNotify(
                    result.priceDetails.price < previousResult.lowestPrice && config.emailOnAllPriceDecreases
                  )
                  _ <- maybeNotify(
                    !priceDropNotified &&
                      allTimeLowestPriceResult.exists(
                        _.lowestPrice > result.priceDetails.price && config.emailOnLowestPriceSinceCreated
                      )
                  )
                  _ <- maybeNotify(
                    result.priceDetails.price > previousResult.lowestPrice && config.emailOnAllPriceIncreases
                  )
                  _ <- maybeNotify(
                    result.priceDetails.price == previousResult.lowestPrice && config.emailOnPriceNoChange
                  )
                } yield ()
              }
              _ <- resultsDB.persistResult(resultRecord)
            } yield ()
        }
      }
    }

}
