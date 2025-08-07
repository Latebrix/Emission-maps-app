package on.emission.maps.data.model

data class TimeSeriesData(
    val latitude: Double,
    val longitude: Double,
    val points: List<TimeSeriesDataPoint>
)

data class TimeSeriesDataPoint(
    val year: Int,
    val month: Int,
    val day: Int,
    val value: Double
)
