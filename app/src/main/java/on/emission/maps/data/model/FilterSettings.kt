package on.emission.maps.data.model

data class FilterSettings(
    val selectedCity: String,
    val selectedYear: String,
    val cityFileUrl: String,
    val gasType: String,
    val selectedParser: String,
    val colormapName: String,
    val colormapMin: Double?,
    val colormapMax: Double?,
    val useLogScale: Boolean
)
