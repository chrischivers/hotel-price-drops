package hotelpricedrops

import cats.effect.IO
import cats.syntax.flatMap._
import io.chrisdavenport.log4cats.Logger

package object util {
  implicit class IOOps[T](io: IO[T])(implicit logger: Logger[IO]) {
    def withRetry(attempts: Int): IO[T] = {
      io.attempt.flatMap {
        case Left(err) =>
          logger.error(err.getMessage) >>
            (if (attempts > 1) {
               logger.info(s"retrying another ${attempts - 1} times") >> withRetry(
                 attempts - 1)
             } else logger.info("No more retries") >> IO.raiseError(err))
        case Right(t) => IO.pure(t)
      }
    }
  }
}
