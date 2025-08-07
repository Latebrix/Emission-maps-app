package on.emission.maps.data

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.jsoup.Jsoup
import java.io.IOException
import java.net.HttpURLConnection
import java.net.URL

class Repository {

    private val citiesCache = mutableMapOf<String, Result<List<String>>>()
    private val yearsCache = mutableMapOf<String, Result<List<Int>>>()
    private val dataCache = mutableMapOf<String, Result<List<Triple<Double, Double, Double>>>>()

    suspend fun fetchCityData(urlString: String): Result<List<Int>> {
        if (yearsCache.containsKey(urlString)) {
            return yearsCache[urlString]!!
        }
        return withContext(Dispatchers.IO) {
            var newURL = urlString
            try {
                val document = Jsoup.connect(urlString).get()
                val links = document.select("a[href$=.txt]")
                val txtLink = links.firstOrNull()?.absUrl("href")
                if (txtLink != null) {
                    newURL = txtLink
                }
            } catch (e: IOException) {
                e.printStackTrace()
            }

            try {
                val url = URL(newURL)
                val connection = url.openConnection() as HttpURLConnection
                connection.requestMethod = "GET"

                val responseCode = connection.responseCode
                if (responseCode == HttpURLConnection.HTTP_OK) {
                    val inputStream = connection.inputStream.bufferedReader()
                    val content = inputStream.use { it.readLines() }

                    val years = content.filter { !it.startsWith("#") }
                        .mapNotNull { line ->
                            val columns = line.split("\\s+".toRegex())
                            columns.getOrNull(1)?.toIntOrNull()
                        }.distinct().sorted()
                    val result = Result.success(years)
                    yearsCache[urlString] = result
                    result
                } else {
                    Result.failure(Exception("Failed to fetch data. Response code: $responseCode"))
                }
            } catch (e: Exception) {
                Result.failure(e)
            }
        }
    }

    suspend fun fetchCities(baseUrl: String): Result<List<String>> {
        if (citiesCache.containsKey(baseUrl)) {
            return citiesCache[baseUrl]!!
        }
        return withContext(Dispatchers.IO) {
            try {
                val doc = Jsoup.connect(baseUrl).get()
                val cities = doc.select("span.mediumred.font-weight-bold")
                    .map { it.text() }
                    .filter { it.isNotBlank() }
                val result = Result.success(cities)
                citiesCache[baseUrl] = result
                result
            } catch (e: Exception) {
                Result.failure(e)
            }
        }
    }

    suspend fun fetchDataFilesForCity(baseUrl: String, cityName: String): Result<List<String>> {
        return withContext(Dispatchers.IO) {
            try {
                val doc = Jsoup.connect(baseUrl).get()
                val cityElements = doc.select("tr")
                val cityFileLinks = mutableListOf<String>()

                for (element in cityElements) {
                    val cityText = element.select("span.mediumred.font-weight-bold").text()
                    val fileLink = element.select("a[href]").attr("href")

                    if (cityText.contains(cityName, ignoreCase = true) && fileLink.isNotEmpty()) {
                        cityFileLinks.add(fileLink)
                    }
                }
                Result.success(cityFileLinks)
            } catch (e: Exception) {
                Result.failure(e)
            }
        }
    }

    suspend fun analyzeData(
        urlString: String,
        selectedYear: String
    ): Result<List<Triple<Double, Double, Double>>> {
        val cacheKey = "$urlString-$selectedYear"
        if (dataCache.containsKey(cacheKey)) {
            return dataCache[cacheKey]!!
        }
        return withContext(Dispatchers.IO) {
            try {
                val url = URL(urlString)
                val connection = url.openConnection() as HttpURLConnection
                connection.requestMethod = "GET"

                val responseCode = connection.responseCode
                if (responseCode == HttpURLConnection.HTTP_OK) {
                    val inputStream = connection.inputStream.bufferedReader()
                    val content = inputStream.use { it.readText() }

                    val lines = content.split("\n").filter { !it.startsWith("#") }
                    val resultList = mutableListOf<Triple<Double, Double, Double>>()

                    for (line in lines) {
                        val parts = line.split("\\s+".toRegex())
                        if (parts.size >= 13 && parts[1] == selectedYear) {
                            val latitude = parts[11].toDoubleOrNull()
                            val longitude = parts[12].toDoubleOrNull()
                            val value = parts[10].toDoubleOrNull()
                            if (latitude != null && longitude != null && value != null) {
                                resultList.add(Triple(latitude, longitude, value))
                            }
                        }
                    }
                    val result = Result.success(resultList)
                    dataCache[cacheKey] = result
                    result
                } else {
                    Result.failure(Exception("Failed to fetch data. Response code: $responseCode"))
                }
            } catch (e: Exception) {
                Result.failure(e)
            }
        }
    }
}
