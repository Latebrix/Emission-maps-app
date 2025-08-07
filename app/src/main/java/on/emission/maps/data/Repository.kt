package on.emission.maps.data

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import on.emission.maps.data.model.TimeSeriesData
import on.emission.maps.data.model.MapDataPoint
import on.emission.maps.data.model.TimeSeriesDataPoint
import org.jsoup.Jsoup
import java.io.IOException
import java.net.HttpURLConnection
import java.net.URL

class Repository {

    private val citiesCache = mutableMapOf<String, Result<List<String>>>()
    private val yearsCache = mutableMapOf<String, Result<List<Int>>>()
    private val dataCache = mutableMapOf<String, Result<List<MapDataPoint>>>()
    private val parsedUrls = mutableSetOf<String>()

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
                    parsedUrls.add(newURL)
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

    suspend fun fetchTimeSeriesData(urlString: String): Result<TimeSeriesData> {
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
                    val allLines = inputStream.use { it.readLines() }

                    var latitude: Double? = null
                    var longitude: Double? = null

                    val headerLines = allLines.filter { it.startsWith("#") }
                    for (line in headerLines) {
                        if (line.contains("Latitude:", ignoreCase = true)) {
                            latitude = line.split(":").getOrNull(1)?.trim()?.toDoubleOrNull()
                        }
                        if (line.contains("Longitude:", ignoreCase = true)) {
                            longitude = line.split(":").getOrNull(1)?.trim()?.toDoubleOrNull()
                        }
                    }

                    if (latitude == null || longitude == null) {
                        return@withContext Result.failure(Exception("Could not determine location from file header."))
                    }

                    val timePoints = mutableListOf<TimeSeriesDataPoint>()
                    val dataLines = allLines.filter { !it.startsWith("#") }

                    for (line in dataLines) {
                        val parts = line.split("\\s+".toRegex())
                        if (parts.size >= 11) {
                            val year = parts.getOrNull(1)?.toIntOrNull()
                            val month = parts.getOrNull(2)?.toIntOrNull()
                            val day = parts.getOrNull(3)?.toIntOrNull()
                            val value = parts.getOrNull(10)?.toDoubleOrNull()

                            if (year != null && month != null && day != null && value != null) {
                                timePoints.add(TimeSeriesDataPoint(year, month, day, value))
                            }
                        }
                    }
                    val result = Result.success(TimeSeriesData(latitude, longitude, timePoints))
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
        selectedYear: String,
        parser: String
    ): Result<List<MapDataPoint>> {
        return when (parser) {
            "Automatic" -> {
                val standardResult = analyzeDataInternal(urlString, selectedYear, "Standard")
                if (standardResult.isSuccess && standardResult.getOrThrow().isNotEmpty()) {
                    standardResult
                } else {
                    analyzeDataInternal(urlString, selectedYear, "Alternative")
                }
            }
            else -> analyzeDataInternal(urlString, selectedYear, parser)
        }
    }

    private suspend fun analyzeDataInternal(
        urlString: String,
        selectedYear: String,
        parser: String
    ): Result<List<MapDataPoint>> {
        val cacheKey = "$urlString-$selectedYear-$parser"
        if (dataCache.containsKey(cacheKey)) {
            return dataCache[cacheKey]!!
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
                    val allLines = inputStream.use { it.readLines() }

                    var latitude: Double? = null
                    var longitude: Double? = null

                    val headerLines = allLines.filter { it.startsWith("#") }
                    for (line in headerLines) {
                        if (line.contains("Latitude:", ignoreCase = true)) {
                            latitude = line.split(":").getOrNull(1)?.trim()?.toDoubleOrNull()
                        }
                        if (line.contains("Longitude:", ignoreCase = true)) {
                            longitude = line.split(":").getOrNull(1)?.trim()?.toDoubleOrNull()
                        }
                    }

                    if (latitude == null || longitude == null) {
                        val firstDataLine = allLines.firstOrNull { !it.startsWith("#") }
                        if (firstDataLine != null) {
                             val parts = firstDataLine.split("\\s+".toRegex())
                             if (parts.size >= 13) {
                                latitude = parts[11].toDoubleOrNull()
                                longitude = parts[12].toDoubleOrNull()
                             }
                        }
                    }

                    if (latitude == null || longitude == null) {
                        return@withContext Result.failure(Exception("Could not determine location from file header."))
                    }

                    val resultList = mutableListOf<MapDataPoint>()
                    val dataLines = allLines.filter { !it.startsWith("#") }

                    for (line in dataLines) {
                        val parts = line.split("\\s+".toRegex())
                        if (parts.size >= 11 && parts[1] == selectedYear) {
                            val value = parts[10].toDoubleOrNull()
                            if (value != null) {
                                resultList.add(MapDataPoint(latitude, longitude, value, newURL))
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

    fun getParsedUrls(): List<String> {
        return parsedUrls.toList()
    }
}
