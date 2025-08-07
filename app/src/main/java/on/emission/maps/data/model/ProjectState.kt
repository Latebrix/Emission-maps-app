package on.emission.maps.data.model

data class ProjectState(
    val gasType: String,
    val selectedCity: String,
    val selectedYear: String,
    val selectedParser: String,
    val cityFileUrl: String,
    val mapCenterLat: Double,
    val mapCenterLon: Double,
    val mapZoomLevel: Double,
    val colormapName: String,
    val colormapMin: Double?,
    val colormapMax: Double?,
    val useLogScale: Boolean
)
