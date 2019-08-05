package hotelpricedrops.db

import hotelpricedrops.utils.Any
import org.scalactic.TypeCheckedTripleEquals
import org.scalatest.{Matchers, WordSpec}

class SearchesDBTest
    extends WordSpec
    with TypeCheckedTripleEquals
    with Matchers {
  "Searches DB" should {
    "persist a new search and retrieve it" in Setup(SearchesDB.apply) {
      searchesDB =>
        val search = Any.search
        for {
          _ <- searchesDB.persistSearch(search)
          allSearches <- searchesDB.allSearches
        } yield {
          allSearches should ===(List(search.withId(1)))
        }
    }
  }

}
