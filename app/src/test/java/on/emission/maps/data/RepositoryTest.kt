package on.emission.maps.data

import kotlinx.coroutines.runBlocking
import on.emission.maps.util.Config
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class RepositoryTest {

    @Test
    fun testFetchCities() {
        val repository = Repository()
        runBlocking {
            val cities = repository.fetchCities(Config.CO2_URL)
            assertNotNull(cities)
            assertTrue(cities.isSuccess)
            assertTrue(cities.getOrNull()!!.isNotEmpty())
        }
    }

    @Test
    fun testGetParsedUrls() {
        val repository = Repository()
        runBlocking {
            // This is a weak test, as it depends on the state from other tests.
            // A better test would use a mock server.
            repository.fetchCityData(on.emission.maps.util.Config.CO2_URL)
            val urls = repository.getParsedUrls()
            assertTrue(urls.isNotEmpty())
        }
    }
}
