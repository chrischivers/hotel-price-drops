package hotelpricedrops.utils

import cats.effect.IO
import cats.effect.concurrent.Ref
import hotelpricedrops.Model
import hotelpricedrops.Model.Screenshot
import hotelpricedrops.selenium.{WebDriver, WebElement}
import org.http4s.Uri
import org.scalatest.Assertions._
import cats.syntax.traverse._
import cats.instances.list._

object MockWebDriver {

  def apply(elementsByClassName: Map[String, List[WebElement]] = Map.empty,
            elementsByXPath: Map[String, List[WebElement]] = Map.empty,
            urlRef: Ref[IO, Option[Uri]] =
              Ref.of[IO, Option[Uri]](None).unsafeRunSync()) = new WebDriver {

    override def setUrl(uri: Uri): IO[Unit] = urlRef.set(Some(uri))

    override def findElementsByClassName(
      className: String
    ): IO[List[WebElement]] =
      (elementsByClassName.values.flatten.toList ++ elementsByXPath.values.flatten.toList)
        .flatTraverse(_.findElementsByClassName(className))
        .map(_ ++ elementsByClassName.getOrElse(className, List.empty))

    override def findElementsByXPath(xPath: String): IO[List[WebElement]] =
      IO(elementsByXPath.getOrElse(xPath, List.empty))

    override def getCurrentUrl: IO[Uri] = urlRef.get.map(_.getOrElse(fail()))

    override def takeScreenshot: IO[Model.Screenshot] =
      IO(Screenshot(Array.empty[Byte]))
  }
}
