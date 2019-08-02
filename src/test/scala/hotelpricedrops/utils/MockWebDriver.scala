package hotelpricedrops.utils

import cats.effect.IO
import hotelpricedrops.Model
import hotelpricedrops.selenium.{WebDriver, WebElement}
import org.http4s.Uri

object MockWebDriver {
  def apply() = new WebDriver {
    override def setUrl(uri: Uri): IO[Unit] = ???

    override def findElementsByClassName(
      className: String
    ): IO[List[WebElement]] = ???

    override def findElementsByXPath(xPath: String): IO[List[WebElement]] = ???

    override def getCurrentUrl: IO[Uri] = ???

    override def takeScreenshot: IO[Model.Screenshot] = ???
  }
}
