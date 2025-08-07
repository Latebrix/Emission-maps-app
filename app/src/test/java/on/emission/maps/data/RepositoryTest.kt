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
}
