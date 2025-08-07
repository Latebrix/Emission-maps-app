package on.emission.maps.widget

import android.widget.TextView
import com.github.mikephil.charting.charts.LineChart
import com.github.mikephil.charting.data.Entry
import com.github.mikephil.charting.data.LineData
import com.github.mikephil.charting.data.LineDataSet
import on.emission.maps.R
import on.emission.maps.data.model.TimeSeriesData
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.Marker
import org.osmdroid.views.overlay.infowindow.InfoWindow

class ChartInfoWindow(
    mapView: MapView,
    private val timeSeriesData: TimeSeriesData,
    private val sourceUrl: String
) : InfoWindow(R.layout.layout_info_window, mapView) {

    override fun onOpen(item: Any?) {
        val marker = item as Marker
        val titleView = mView.findViewById<TextView>(R.id.bubble_title)
        val descriptionView = mView.findViewById<TextView>(R.id.bubble_description)
        val sourceUrlView = mView.findViewById<TextView>(R.id.bubble_source_url)
        val chartView = mView.findViewById<LineChart>(R.id.bubble_chart)

        titleView.text = marker.title
        descriptionView.text = "Lat: ${marker.position.latitude}, Lon: ${marker.position.longitude}"
        sourceUrlView.text = sourceUrl

        setupChart(chartView)
    }

    private fun setupChart(chart: LineChart) {
        val entries = mutableListOf<Entry>()
        timeSeriesData.points.forEachIndexed { index, point ->
            entries.add(Entry(index.toFloat(), point.value.toFloat()))
        }

        val dataSet = LineDataSet(entries, "CO2 Concentration")
        val lineData = LineData(dataSet)
        chart.data = lineData
        chart.description.text = "Full History"
        chart.invalidate()
    }

    override fun onClose() {
        // No special action needed
    }
}
