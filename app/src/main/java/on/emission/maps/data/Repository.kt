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
    private var customParser: CustomParser? = null

    fun setCustomParser(parser: CustomParser) {
        this.customParser = parser
    }

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
                connection.connectTimeout = 30000 // 30 seconds
                connection.readTimeout = 30000 // 30 seconds
                connection.requestMethod = "GET"

                val responseCode = connection.responseCode
                if (responseCode == HttpURLConnection.HTTP_OK) {
                    parsedUrls.add(newURL)
                    val inputStream = connection.inputStream.bufferedReader()
                    val allLines = inputStream.use { it.readLines() }
                    val years = allLines.mapNotNull { line ->
                        if (!line.startsWith("#")) {
                            line.split("\\s+".toRegex()).getOrNull(1)?.toIntOrNull()
                        } else {
                            null
                        }
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
                connection.connectTimeout = 30000 // 30 seconds
                connection.readTimeout = 30000 // 30 seconds
                connection.requestMethod = "GET"

                val responseCode = connection.responseCode
                if (responseCode == HttpURLConnection.HTTP_OK) {
                    val inputStream = connection.inputStream.bufferedReader()
                    val allLines = inputStream.use { it.readLines() }

                    var latitude: Double? = null
                    var longitude: Double? = null
                    val timePoints = mutableListOf<TimeSeriesDataPoint>()
                    val dataLines = allLines.filter { !it.startsWith("#") }

                    if (dataLines.isNotEmpty()) {
                        val firstLineParts = dataLines[0].split("\\s+".toRegex())
                        if (firstLineParts.size >= 13) {
                            latitude = firstLineParts[11].toDoubleOrNull()
                            longitude = firstLineParts[12].toDoubleOrNull()
                        }
                    }

                    if (latitude == null || longitude == null) {
                        return@withContext Result.failure(Exception("Could not determine location from file."))
                    }

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
                } else if (customParser != null) {
                    analyzeDataInternal(urlString, selectedYear, "Alternative")
                } else {
                    standardResult // or return a specific error
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
                connection.connectTimeout = 30000 // 30 seconds
                connection.readTimeout = 30000 // 30 seconds
                connection.requestMethod = "GET"

                val responseCode = connection.responseCode
                if (responseCode == HttpURLConnection.HTTP_OK) {
                    val inputStream = connection.inputStream.bufferedReader()
                    val allText = inputStream.use { it.readText() }

                    if (parser == "Alternative" && customParser != null) {
                        return@withContext customParser!!.parse(allText)
                    }

                    val allLines = allText.lines()
                    val resultList = mutableListOf<MapDataPoint>()
                    val dataLines = allLines.filter { !it.startsWith("#") }

                    for (line in dataLines) {
                        val parts = line.split("\\s+".toRegex())
                        if (parts.size >= 13 && parts[1] == selectedYear) {
                            val latitude = parts[11].toDoubleOrNull()
                            val longitude = parts[12].toDoubleOrNull()
                            val value = parts[10].toDoubleOrNull()

                            if (latitude != null && longitude != null && value != null) {
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
