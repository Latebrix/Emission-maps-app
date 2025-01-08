package on.emission.maps.parser.data

import android.content.Context
import android.widget.Toast
import kotlinx.coroutines.*
import java.net.HttpURLConnection
import java.net.URL

class DataAnalyzer(private val context: Context) {

    fun analyzeData(urlString: String, gasType: String, selectedYear: String, callback: (List<Triple<Double, Double, Double>>?) -> Unit) {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val url = URL(urlString)
                val connection = url.openConnection() as HttpURLConnection
                connection.requestMethod = "GET"

                val responseCode = connection.responseCode
                if (responseCode == HttpURLConnection.HTTP_OK) {
                    val inputStream = connection.inputStream.bufferedReader()
                    val content = inputStream.use { it.readText() }

                    val lines = content.split("\n").filter { !it.startsWith("#") }
                    val result = mutableListOf<Triple<Double, Double, Double>>()

                    for (line in lines) {
                        val parts = line.split("\\s+".toRegex())
                        if (parts.size >= 13 && parts[1] == selectedYear) {
                            val latitude = parts[11].toDoubleOrNull()
                            val longitude = parts[12].toDoubleOrNull()
                            val value = parts[10].toDoubleOrNull()
                            if (latitude != null && longitude != null && value != null) {
                                result.add(Triple(latitude, longitude, value))
                            }
                        }
                    }

                    withContext(Dispatchers.Main) {
                        callback(result)
                    }
                } else {
                    showToast("Error: Failed to fetch data. Response code: $responseCode")
                    withContext(Dispatchers.Main) { callback(null) }
                }
            } catch (e: Exception) {
                showToast("Error: ${e.message}")
                e.printStackTrace()
                withContext(Dispatchers.Main) { callback(null) }
            }
        }
    }


    private fun showToast(message: String) {
        CoroutineScope(Dispatchers.Main).launch {
            Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
        }
    }
}
