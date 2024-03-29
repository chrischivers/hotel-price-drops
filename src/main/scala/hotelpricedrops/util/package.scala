package hotelpricedrops

import cats.effect.{ContextShift, IO, Timer}
import cats.syntax.flatMap._
import io.chrisdavenport.log4cats.Logger

import scala.concurrent.CancellationException
import scala.concurrent.duration.FiniteDuration

package object util {
  implicit class IOOps[T](io: IO[T])(implicit logger: Logger[IO]) {
    def withRetry(attempts: Int): IO[T] = {
      io.attempt.flatMap {
        case Left(err) =>
          logger.error(err.getMessage) >>
            (if (attempts > 1) {
               logger.info(s"retrying another ${attempts - 1} times") >> withRetry(
                 attempts - 1
               )
             } else logger.info("No more retries") >> IO.raiseError(err))
        case Right(t) => IO.pure(t)
      }
    }

    def withTimeout(
      timeoutAfter: FiniteDuration
    )(implicit timer: Timer[IO], contextShift: ContextShift[IO]): IO[T] = {
      IO.race(io, timer.sleep(timeoutAfter)).flatMap {
        case Left(r) => IO.pure(r)
        case Right(_) =>
          IO.raiseError(
            new CancellationException(s"IO timed out after $timeoutAfter")
          )
      }
    }
  }
}
