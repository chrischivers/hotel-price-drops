package hotelpricedrops

import cats.effect.{IO, Resource}
import org.openqa.selenium.firefox.{FirefoxDriver, FirefoxOptions}

object WebDriver {

  private def firefoxDriverResource(headless: Boolean) = {
    Resource.make[IO, FirefoxDriver](IO {
      val options = new FirefoxOptions()
      options.setHeadless(headless)
      new FirefoxDriver(options)
    })(driver => IO(driver.close()))
  }

  def apply(headless: Boolean = false) = firefoxDriverResource(headless)

}
