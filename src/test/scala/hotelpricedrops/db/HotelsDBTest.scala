package hotelpricedrops.db

import cats.effect.IO
import hotelpricedrops.Main
import hotelpricedrops.utils.Any
import org.scalactic.TypeCheckedTripleEquals
import org.scalatest.{Assertion, Matchers, WordSpec}

import scala.concurrent.ExecutionContext

class HotelsDBTest extends WordSpec with TypeCheckedTripleEquals with Matchers {
  "Hotels DB" should {
    "persist a new hotel" in Setup(HotelsDB.apply) { hotelsDb =>
      val hotel = Any.hotel
      for {
        _ <- hotelsDb.persistHotel(hotel)
        allHotels <- hotelsDb.allHotels
      } yield {
        allHotels should ===(List(hotel.withId(1)))
      }
    }
    "look up hotel by name" in Setup(HotelsDB.apply) { hotelsDb =>
      val hotel = Any.hotel
      for {
        _ <- hotelsDb.persistHotel(hotel)
        hotelFromDb <- hotelsDb.hotelByName(hotel.name)
      } yield {
        hotelFromDb should ===(Some(hotel.withId(1)))
      }
    }

    "Updated a hotel" in Setup(HotelsDB.apply) { hotelsDb =>
      val hotel = Any.hotel
      val newHotel = Any.hotel.copy(name = hotel.name)
      for {
        _ <- hotelsDb.persistHotel(hotel)
        _ <- hotelsDb.updateHotel(1, newHotel)
        allHotels <- hotelsDb.allHotels
      } yield {
        allHotels should ===(List(newHotel.withId(1)))
      }
    }
  }
}
