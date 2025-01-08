package on.emission.maps.parser.data

import kotlinx.coroutines.*
import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import org.jsoup.select.Elements
import java.io.IOException
import java.net.HttpURLConnection
import java.net.URL

class DataFetcher {

    fun fetchCityData(urlString: String, callback: (List<Int>?) -> Unit) {
        CoroutineScope(Dispatchers.IO).launch {
            var newURl= urlString.toString()
            try {

                val document: Document = Jsoup.connect(urlString.toString()).get()
                val links: Elements = document.select("a[href$=.txt]")
                val txtLink: String? = links.firstOrNull()?.absUrl("href")
                withContext(Dispatchers.Main) {
                    newURl= txtLink.toString()
                }
            } catch (e: IOException) {
                e.printStackTrace()
                withContext(Dispatchers.Main) {
                    newURl= urlString.toString()
                }
            }

            try {
                val url = URL(newURl)
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

                    withContext(Dispatchers.Main) {
                        callback(years)
                    }
                } else {
                    withContext(Dispatchers.Main) {
                        callback(null)
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
                withContext(Dispatchers.Main) {
                    callback(null)
                }
            }
        }
    }

    fun fetchCities(baseUrl: String, callback: (List<String>?) -> Unit) {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val doc = Jsoup.connect(baseUrl).get()
                val cities = doc.select("span.mediumred.font-weight-bold")
                    .map { it.text() }
                    .filter { it.isNotBlank() }
                withContext(Dispatchers.Main) {
                    callback(cities)
                }
            } catch (e: Exception) {
                e.printStackTrace()
                withContext(Dispatchers.Main) {
                    callback(null)
                }
            }
        }
    }

    fun fetchDataFilesForCity(baseUrl: String, cityName: String, callback: (List<String>?) -> Unit) {
        CoroutineScope(Dispatchers.IO).launch {
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

                withContext(Dispatchers.Main) {
                    callback(cityFileLinks)
                }
            } catch (e: Exception) {
                e.printStackTrace()
                withContext(Dispatchers.Main) {
                    callback(null)
                }
            }
        }
    }

}
