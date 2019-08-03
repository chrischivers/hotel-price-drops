package hotelpricedrops.utils

import cats.effect.IO
import hotelpricedrops.selenium.WebElement
import org.scalatest.Assertions._

object MockWebElement {
  def apply(textOpt: Option[String] = None,
            attributes: Map[String, String] = Map.empty,
            elementsByClassName: Map[String, List[WebElement]] = Map.empty) =
    new WebElement {
      override def text: IO[String] = IO(textOpt.getOrElse(fail()))

      override def getAttribute(attrName: String): IO[String] =
        IO(attributes.getOrElse(attrName, fail()))

      override def findElementByClassName(className: String): IO[WebElement] =
        IO(elementsByClassName.getOrElse(className, fail()).head)

      override def findElementsByClassName(
        className: String
      ): IO[List[WebElement]] =
        IO(elementsByClassName.getOrElse(className, List.empty))
    }
}
