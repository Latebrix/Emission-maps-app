package on.emission.maps.ui.home

import FilterBottomSheetFragment
import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import on.emission.maps.R
import on.emission.maps.data.MainViewModel
import on.emission.maps.databinding.FragmentHomeBinding
import on.emission.maps.databinding.SubBarBinding
import org.osmdroid.api.IMapController
import org.osmdroid.config.Configuration
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.Marker
// import org.osmdroid.events.MapEventsOverlay
// import org.osmdroid.events.MapEventsReceiver
// import org.osmdroid.views.overlay.Polygon
import org.osmdroid.views.overlay.ScaleBarOverlay
import android.animation.ObjectAnimator
import android.graphics.Bitmap
import android.graphics.PorterDuff
import android.graphics.PorterDuffColorFilter
import android.graphics.drawable.ShapeDrawable
import android.graphics.drawable.shapes.OvalShape
import on.emission.maps.util.color.ColormapUtil
import org.osmdroid.views.overlay.compass.CompassOverlay
import android.net.Uri
import androidx.activity.result.contract.ActivityResultContracts
import com.google.gson.Gson
import on.emission.maps.data.model.ProjectState
import on.emission.maps.widget.ChartInfoWindow
import org.osmdroid.views.overlay.infowindow.InfoWindow
import org.osmdroid.views.overlay.mylocation.GpsMyLocationProvider
import org.osmdroid.views.overlay.mylocation.MyLocationNewOverlay
import java.io.BufferedReader
import java.io.FileOutputStream
import java.io.InputStreamReader
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

class MainFragment : Fragment() {

    private val saveProjectLauncher = registerForActivityResult(
        ActivityResultContracts.CreateDocument("application/json")
    ) { uri: Uri? ->
        uri?.let { saveProjectToFile(it) }
    }

    private val loadProjectLauncher = registerForActivityResult(
        ActivityResultContracts.OpenDocument()
    ) { uri: Uri? ->
        uri?.let { loadProjectFromFile(it) }
    }

    private val importDataLauncher = registerForActivityResult(
        ActivityResultContracts.OpenDocument()
    ) { uri: Uri? ->
        uri?.let { showColumnMappingDialog(it) }
    }

    private val exportDataLauncher = registerForActivityResult(
        ActivityResultContracts.CreateDocument("text/csv")
    ) { uri: Uri? ->
        uri?.let { exportDataToFile(it) }
    }

    private val exportImageLauncher = registerForActivityResult(
        ActivityResultContracts.CreateDocument("image/png")
    ) { uri: Uri? ->
        uri?.let { exportImageToFile(it) }
    }

    private lateinit var binding: FragmentHomeBinding
    private lateinit var map: MapView
    private lateinit var controller: IMapController
    private val viewModel: MainViewModel by viewModels()
    private lateinit var bindingBar: SubBarBinding
    private var cities: List<String> = emptyList()
    private var currentProjectState: ProjectState? = null
    private var isFabMenuOpen = false
    private var currentColormapName: String = "Simple"
    private var currentColormapMin: Double? = null
    private var currentColormapMax: Double? = null
    private var currentUseLogScale: Boolean = false
    private var animationTimeSeriesData: on.emission.maps.data.model.TimeSeriesData? = null
    private var timeSeriesJob: kotlinx.coroutines.Job? = null
    private var markerForInfoWindow: Marker? = null
    private var urlForInfoWindow: String? = null
    private var datasetA: List<on.emission.maps.data.model.MapDataPoint>? = null

    private lateinit var locationOverlay: MyLocationNewOverlay

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentHomeBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        inflateMyViews()
        setupMap()
        observeViewModel()

        viewModel.fetchCities(on.emission.maps.util.Config.CO2_URL)

        binding.toolFab.setOnClickListener {
            getCurrentLocation()
        }

        binding.fab.setOnClickListener {
            if (isFabMenuOpen) {
                closeFabMenu()
            } else {
                openFabMenu()
            }
        }
        binding.fab.setOnLongClickListener {
            if (animationTimeSeriesData != null) {
                toggleTimeSeriesPlay()
            } else {
                currentProjectState?.cityFileUrl?.let {
                    viewModel.fetchAnimationTimeSeries(it)
                } ?: Toast.makeText(requireContext(), "Apply a filter first to select a location.", Toast.LENGTH_SHORT).show()
            }
            true
        }

        binding.saveProjectFab.setOnClickListener {
            saveProjectLauncher.launch("project.json")
        }

        binding.loadProjectFab.setOnClickListener {
            loadProjectLauncher.launch(arrayOf("application/json"))
        }

        binding.importDataFab.setOnClickListener {
            importDataLauncher.launch(arrayOf("text/csv", "text/plain"))
        }

        binding.exportDataFab.setOnClickListener {
            exportDataLauncher.launch("exported_data.csv")
        }

        binding.exportImageFab.setOnClickListener {
            exportImageLauncher.launch("map_export.png")
        }

        // ROI feature removed due to build issues.

        binding.setDatasetAFab.setOnClickListener {
            datasetA = viewModel.data.value?.getOrNull() ?: viewModel.customData.value
            if (datasetA != null) {
                Toast.makeText(requireContext(), "Dataset A set.", Toast.LENGTH_SHORT).show()
            } else {
                Toast.makeText(requireContext(), "No data to set as Dataset A.", Toast.LENGTH_SHORT).show()
            }
        }

        binding.compareWithAFab.setOnClickListener {
            compareWithDatasetA()
        }

        setupTimeSeriesControls()
    }

    private fun setupMap() {
        map = binding.map
        Configuration.getInstance().load(
            requireContext(),
            requireActivity().getSharedPreferences("osmdroid", AppCompatActivity.MODE_PRIVATE)
        )
        map.setMultiTouchControls(true)
        map.setMinZoomLevel(3.0)
        map.setTileSource(org.osmdroid.tileprovider.tilesource.TileSourceFactory.MAPNIK)

        controller = map.controller
        controller.setZoom(5.0)
        controller.setCenter(GeoPoint(40.0, -88.0))
        addScaleBar()
        addCompass()
    }

    private fun observeViewModel() {
        viewModel.cities.observe(viewLifecycleOwner) { result ->
            result.onSuccess {
                cities = it
            }.onFailure {
                Toast.makeText(requireContext(), it.message, Toast.LENGTH_SHORT).show()
            }
        }

        viewModel.customData.observe(viewLifecycleOwner) { data ->
            map.overlays.clear()
            addScaleBar()
            addCompass()

            if (data.isNotEmpty()) {
                val values = data.map { it.value }
                val min = currentColormapMin ?: values.minOrNull() ?: 0.0
                val max = currentColormapMax ?: values.maxOrNull() ?: 1.0

                for (point in data) {
                    val marker = Marker(map)
                    marker.position = GeoPoint(point.latitude, point.longitude)
                    marker.title = "${point.value}"

                    val color = ColormapUtil.getColor(point.value, min, max, currentColormapName, currentUseLogScale)
                    val circle = ShapeDrawable(OvalShape())
                    circle.intrinsicHeight = 40
                    circle.intrinsicWidth = 40
                    circle.paint.colorFilter = PorterDuffColorFilter(color, PorterDuff.Mode.SRC_IN)
                    marker.icon = circle

                    marker.setOnMarkerClickListener { clickedMarker, _ ->
                        // For custom data, we don't have a URL to fetch full time series,
                        // so we can't show a chart. Just show the default window.
                        if (!clickedMarker.isInfoWindowShown) {
                            clickedMarker.showInfoWindow()
                        } else {
                            clickedMarker.closeInfoWindow()
                        }
                        true
                    }

                    map.overlays.add(marker)
                }
            }

            map.invalidate()
            Toast.makeText(requireContext(), "Displayed custom data.", Toast.LENGTH_SHORT).show()
        }

        viewModel.data.observe(viewLifecycleOwner) { result ->
            result.onSuccess { data ->
                binding.timeSeriesControls.visibility = View.GONE
                timeSeriesJob?.cancel()
                map.overlays.clear()
                addScaleBar()
                addCompass()

                if (data.isNotEmpty()) {
                    val values = data.map { it.value }
                    val min = currentColormapMin ?: values.minOrNull() ?: 0.0
                    val max = currentColormapMax ?: values.maxOrNull() ?: 1.0

                    for (point in data) {
                        val marker = Marker(map)
                        marker.position = GeoPoint(point.latitude, point.longitude)
                        marker.title = "Value: ${point.value}"

                        val color = ColormapUtil.getColor(point.value, min, max, currentColormapName, currentUseLogScale)
                        val circle = ShapeDrawable(OvalShape())
                        circle.intrinsicHeight = 40
                        circle.intrinsicWidth = 40
                        circle.paint.colorFilter = PorterDuffColorFilter(color, PorterDuff.Mode.SRC_IN)
                        marker.icon = circle

                        marker.setOnMarkerClickListener { clickedMarker, _ ->
                            this.markerForInfoWindow = clickedMarker
                            this.urlForInfoWindow = point.cityFileUrl
                            viewModel.fetchProbeTimeSeries(point.cityFileUrl)
                            true
                        }

                        map.overlays.add(marker)
                    }
                }

                map.invalidate()
            }.onFailure {
                Toast.makeText(requireContext(), it.message, Toast.LENGTH_SHORT).show()
            }
        }

        viewModel.animationTimeSeriesData.observe(viewLifecycleOwner) { result ->
            result.onSuccess { data ->
                animationTimeSeriesData = data
                binding.timeSeriesControls.visibility = View.VISIBLE
                binding.timeSeriesSeekbar.max = data.points.size - 1
                updateMapForTimeSeries(0)
                Toast.makeText(requireContext(), "Time series data loaded. Long-press Tools to play/pause.", Toast.LENGTH_LONG).show()
            }.onFailure {
                binding.timeSeriesControls.visibility = View.GONE
                Toast.makeText(requireContext(), "Error loading time series: ${it.message}", Toast.LENGTH_SHORT).show()
            }
        }

        viewModel.probeTimeSeriesData.observe(viewLifecycleOwner) { result ->
            result.onSuccess { data ->
                markerForInfoWindow?.let { marker ->
                    urlForInfoWindow?.let { url ->
                        val chartInfoWindow = ChartInfoWindow(map, data, url)
                        marker.infoWindow = chartInfoWindow
                        marker.showInfoWindow()
                    }
                }
            }.onFailure {
                Toast.makeText(requireContext(), "Error loading chart data: ${it.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun getCurrentLocation() {
        if (ActivityCompat.checkSelfPermission(
                requireContext(),
                Manifest.permission.ACCESS_FINE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            ActivityCompat.requestPermissions(
                requireActivity(),
                arrayOf(Manifest.permission.ACCESS_FINE_LOCATION),
                LOCATION_PERMISSION_REQUEST_CODE
            )
            return
        }

        locationOverlay = MyLocationNewOverlay(GpsMyLocationProvider(requireContext()), map)
        locationOverlay.enableMyLocation()
        map.overlays.add(locationOverlay)

        locationOverlay.runOnFirstFix {
            activity?.runOnUiThread {
                controller.setCenter(locationOverlay.myLocation)
            }
        }
    }

    private fun showFilterBottomSheet() {
        val bottomSheetFragment =
            FilterBottomSheetFragment(cities) { selectedCity, selectedYear, cityFileUrl, gasType, selectedParser, colormapName, colormapMin, colormapMax, useLogScale ->

                this.currentColormapName = colormapName
                this.currentColormapMin = colormapMin
                this.currentColormapMax = colormapMax
                this.currentUseLogScale = useLogScale

                currentProjectState = ProjectState(
                    gasType = gasType,
                    selectedCity = selectedCity,
                    selectedYear = selectedYear,
                    selectedParser = selectedParser,
                    cityFileUrl = cityFileUrl,
                    mapCenterLat = map.mapCenter.latitude,
                    mapCenterLon = map.mapCenter.longitude,
                    mapZoomLevel = map.zoomLevelDouble,
                    colormapName = colormapName,
                    colormapMin = colormapMin,
                    colormapMax = colormapMax,
                    useLogScale = useLogScale
                )
                viewModel.fetchData(cityFileUrl, selectedYear, selectedParser)
            }
        bottomSheetFragment.show(childFragmentManager, bottomSheetFragment.tag)
    }

    private fun inflateMyViews() {
        bindingBar = SubBarBinding.inflate(LayoutInflater.from(requireContext()), binding.bar, true)
        bindingBar.root.setOnClickListener {
            showFilterBottomSheet()
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == LOCATION_PERMISSION_REQUEST_CODE) {
            if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                getCurrentLocation()
            } else {
                Toast.makeText(requireContext(), "Location permission denied", Toast.LENGTH_SHORT)
                    .show()
            }
        }
    }

    private fun addScaleBar() {
        val scaleBarOverlay = ScaleBarOverlay(map)
        scaleBarOverlay.setAlignRight(true)
        scaleBarOverlay.setAlignBottom(true)

        val bottomPadding = 170
        val rightPadding = 380
        scaleBarOverlay.setScaleBarOffset(rightPadding, bottomPadding)

        map.overlays.add(scaleBarOverlay)
    }

    private fun addCompass() {
        val compassOverlay = CompassOverlay(requireContext(), map)
        compassOverlay.enableCompass()

        val topPadding = 200
        val rightPadding = 150
        compassOverlay.setCompassCenter(map.width - rightPadding.toFloat(), topPadding.toFloat())

        map.overlays.add(compassOverlay)
    }

    companion object {
        private const val LOCATION_PERMISSION_REQUEST_CODE = 1000
    }

    private fun saveProjectToFile(uri: Uri) {
        if (currentProjectState == null) {
            Toast.makeText(requireContext(), "Please apply a filter first to create a savable state.", Toast.LENGTH_LONG).show()
            return
        }

        val stateToSave = currentProjectState!!.copy(
            mapCenterLat = map.mapCenter.latitude,
            mapCenterLon = map.mapCenter.longitude,
            mapZoomLevel = map.zoomLevelDouble
        )

        val gson = Gson()
        val jsonString = gson.toJson(stateToSave)

        try {
            requireContext().contentResolver.openFileDescriptor(uri, "w")?.use {
                FileOutputStream(it.fileDescriptor).use { fos ->
                    fos.write(jsonString.toByteArray())
                }
            }
            Toast.makeText(requireContext(), "Project saved successfully.", Toast.LENGTH_SHORT).show()
        } catch (e: Exception) {
            Toast.makeText(requireContext(), "Error saving project: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }

    private fun loadProjectFromFile(uri: Uri) {
        try {
            val inputStream = requireContext().contentResolver.openInputStream(uri)
            val reader = BufferedReader(InputStreamReader(inputStream))
            val gson = Gson()
            val state = gson.fromJson(reader, ProjectState::class.java)
            applyProjectState(state)
            Toast.makeText(requireContext(), "Project loaded successfully.", Toast.LENGTH_SHORT).show()
        } catch (e: Exception) {
            Toast.makeText(requireContext(), "Error loading project: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }

    private fun applyProjectState(state: ProjectState) {
        currentProjectState = state
        controller.setZoom(state.mapZoomLevel)
        controller.setCenter(GeoPoint(state.mapCenterLat, state.mapCenterLon))

        viewModel.fetchData(
            state.cityFileUrl,
            state.selectedYear,
            state.selectedParser
        )
        Toast.makeText(requireContext(), "Project state restored and data refreshed.", Toast.LENGTH_LONG).show()
    }

    private fun showColumnMappingDialog(uri: Uri) {
        val dialogView = LayoutInflater.from(requireContext()).inflate(R.layout.dialog_column_mapping, null)
        val latEditText = dialogView.findViewById<android.widget.EditText>(R.id.edit_text_lat_col)
        val lonEditText = dialogView.findViewById<android.widget.EditText>(R.id.edit_text_lon_col)
        val valEditText = dialogView.findViewById<android.widget.EditText>(R.id.edit_text_val_col)
        val headerCheckbox = dialogView.findViewById<android.widget.CheckBox>(R.id.checkbox_has_header)

        androidx.appcompat.app.AlertDialog.Builder(requireContext())
            .setTitle("Configure CSV Import")
            .setView(dialogView)
            .setPositiveButton("Import") { _, _ ->
                val latCol = latEditText.text.toString().toIntOrNull() ?: 0
                val lonCol = lonEditText.text.toString().toIntOrNull() ?: 1
                val valCol = valEditText.text.toString().toIntOrNull() ?: 2
                val hasHeader = headerCheckbox.isChecked
                importDataFromFile(uri, latCol, lonCol, valCol, hasHeader)
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun importDataFromFile(uri: Uri, latCol: Int, lonCol: Int, valCol: Int, hasHeader: Boolean) {
        val data = mutableListOf<on.emission.maps.data.model.MapDataPoint>()
        try {
            val inputStream = requireContext().contentResolver.openInputStream(uri)
            val reader = BufferedReader(InputStreamReader(inputStream))

            if (hasHeader) {
                reader.readLine()
            }

            var line: String?
            while (reader.readLine().also { line = it } != null) {
                val parts = line!!.split(",")
                val lat = parts.getOrNull(latCol)?.toDoubleOrNull()
                val lon = parts.getOrNull(lonCol)?.toDoubleOrNull()
                val value = parts.getOrNull(valCol)?.toDoubleOrNull()

                if (lat != null && lon != null && value != null) {
                    data.add(on.emission.maps.data.model.MapDataPoint(lat, lon, value, uri.toString()))
                }
            }
            viewModel.displayCustomData(data)
            Toast.makeText(requireContext(), "Successfully imported ${data.size} data points.", Toast.LENGTH_SHORT).show()
        } catch (e: Exception) {
            Toast.makeText(requireContext(), "Error importing data: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }

    private fun exportDataToFile(uri: Uri) {
        val dataToExport = viewModel.data.value?.getOrNull()

        if (dataToExport == null || dataToExport.isEmpty()) {
            Toast.makeText(requireContext(), "No data to export.", Toast.LENGTH_SHORT).show()
            return
        }

        val csvContent = StringBuilder()
        csvContent.append("latitude,longitude,value\n")
        for (point in dataToExport) {
            csvContent.append("${point.latitude},${point.longitude},${point.value}\n")
        }

        try {
            requireContext().contentResolver.openFileDescriptor(uri, "w")?.use {
                FileOutputStream(it.fileDescriptor).use { fos ->
                    fos.write(csvContent.toString().toByteArray())
                }
            }
            Toast.makeText(requireContext(), "Data exported successfully.", Toast.LENGTH_SHORT).show()
        } catch (e: Exception) {
            Toast.makeText(requireContext(), "Error exporting data: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }

    private fun exportImageToFile(uri: Uri) {
        map.isDrawingCacheEnabled = true
        val bitmap = map.drawingCache
        try {
            requireContext().contentResolver.openFileDescriptor(uri, "w")?.use {
                FileOutputStream(it.fileDescriptor).use { fos ->
                    bitmap.compress(Bitmap.CompressFormat.PNG, 100, fos)
                }
            }
            Toast.makeText(requireContext(), "Image exported successfully.", Toast.LENGTH_SHORT).show()
        } catch (e: Exception) {
            Toast.makeText(requireContext(), "Error exporting image: ${e.message}", Toast.LENGTH_SHORT).show()
        } finally {
            map.isDrawingCacheEnabled = false
        }
    }

    private fun openFabMenu() {
        isFabMenuOpen = true
        ObjectAnimator.ofFloat(binding.fab, "rotation", 0f, 45f).start()
        showFab(binding.exportImageFab, -200f)
        showFab(binding.exportDataFab, -350f)
        showFab(binding.importDataFab, -500f)
        showFab(binding.loadProjectFab, -650f)
        showFab(binding.saveProjectFab, -800f)
    }

    private fun closeFabMenu() {
        isFabMenuOpen = false
        ObjectAnimator.ofFloat(binding.fab, "rotation", 45f, 0f).start()
        hideFab(binding.exportImageFab)
        hideFab(binding.exportDataFab)
        hideFab(binding.importDataFab)
        hideFab(binding.loadProjectFab)
        hideFab(binding.saveProjectFab)
    }

    private fun showFab(fab: View, translationY: Float) {
        fab.visibility = View.VISIBLE
        fab.alpha = 0f
        fab.translationY = 0f
        fab.animate()
            .setDuration(300)
            .translationY(translationY)
            .alpha(1f)
            .start()
    }

    private fun hideFab(fab: View) {
        fab.animate()
            .setDuration(300)
            .translationY(0f)
            .alpha(0f)
            .withEndAction { fab.visibility = View.GONE }
            .start()
    }

    private fun setupTimeSeriesControls() {
        binding.playPauseButton.setOnClickListener {
            toggleTimeSeriesPlay()
        }

        binding.timeSeriesSeekbar.setOnSeekBarChangeListener(object : android.widget.SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: android.widget.SeekBar?, progress: Int, fromUser: Boolean) {
                if (fromUser) {
                    timeSeriesJob?.cancel()
                    binding.playPauseButton.setImageResource(android.R.drawable.ic_media_play)
                    updateMapForTimeSeries(progress)
                }
            }

            override fun onStartTrackingTouch(seekBar: android.widget.SeekBar?) {}
            override fun onStopTrackingTouch(seekBar: android.widget.SeekBar?) {}
        })
    }

    private fun toggleTimeSeriesPlay() {
        if (timeSeriesJob?.isActive == true) {
            timeSeriesJob?.cancel()
            binding.playPauseButton.setImageResource(android.R.drawable.ic_media_play)
        } else {
            timeSeriesJob = viewLifecycleOwner.lifecycleScope.launch {
                binding.playPauseButton.setImageResource(android.R.drawable.ic_media_pause)
                while (isActive) {
                    var nextProgress = binding.timeSeriesSeekbar.progress + 1
                    if (nextProgress >= binding.timeSeriesSeekbar.max) {
                        nextProgress = 0
                    }
                    binding.timeSeriesSeekbar.progress = nextProgress
                    updateMapForTimeSeries(nextProgress)
                    delay(500)
                }
            }
        }
    }

    private fun updateMapForTimeSeries(index: Int) {
        animationTimeSeriesData?.let { data ->
            if (index < data.points.size) {
                val point = data.points[index]
                map.overlays.clear()
                addScaleBar()
                addCompass()

                val values = data.points.map { it.value }
                val min = currentColormapMin ?: values.minOrNull() ?: 0.0
                val max = currentColormapMax ?: values.maxOrNull() ?: 1.0

                val marker = Marker(map)
                marker.position = GeoPoint(data.latitude, data.longitude)
                marker.title = "Value: ${point.value} on ${point.year}-${point.month}-${point.day}"

                val color = ColormapUtil.getColor(point.value, min, max, currentColormapName, currentUseLogScale)
                val circle = ShapeDrawable(OvalShape())
                circle.intrinsicHeight = 40
                circle.intrinsicWidth = 40
                circle.paint.colorFilter = PorterDuffColorFilter(color, PorterDuff.Mode.SRC_IN)
                marker.icon = circle

                map.overlays.add(marker)
                map.invalidate()
            }
        }
    }

    // ROI functions removed due to build issues

    private fun compareWithDatasetA() {
        val datasetB = viewModel.data.value?.getOrNull() ?: viewModel.customData.value
        if (datasetA == null || datasetB == null) {
            Toast.makeText(requireContext(), "Please set Dataset A and load a second dataset before comparing.", Toast.LENGTH_LONG).show()
            return
        }

        val aMap = datasetA!!.associateBy { GeoPoint(it.latitude, it.longitude) }
        val bMap = datasetB.associateBy { GeoPoint(it.latitude, it.longitude) }

        val diffData = mutableListOf<on.emission.maps.data.model.MapDataPoint>()
        for ((geoPoint, aPoint) in aMap) {
            bMap[geoPoint]?.let { bPoint ->
                val diffValue = bPoint.value - aPoint.value
                diffData.add(on.emission.maps.data.model.MapDataPoint(aPoint.latitude, aPoint.longitude, diffValue, "Difference"))
            }
        }

        if (diffData.isEmpty()) {
            Toast.makeText(requireContext(), "No matching points found to compare.", Toast.LENGTH_SHORT).show()
            return
        }

        map.overlays.clear()
        addScaleBar()
        addCompass()

        val values = diffData.map { it.value }
        val maxAbsValue = values.map { kotlin.math.abs(it) }.maxOrNull() ?: 1.0
        val min = -maxAbsValue
        val max = maxAbsValue

        for (point in diffData) {
            val marker = Marker(map)
            marker.position = GeoPoint(point.latitude, point.longitude)
            marker.title = "Difference: ${"%.2f".format(point.value)}"

            val color = ColormapUtil.getColor(point.value, min, max, "Diverging", false)
            val circle = ShapeDrawable(OvalShape())
            circle.intrinsicHeight = 40
            circle.intrinsicWidth = 40
            circle.paint.colorFilter = PorterDuffColorFilter(color, PorterDuff.Mode.SRC_IN)
            marker.icon = circle

            map.overlays.add(marker)
        }
        map.invalidate()
        Toast.makeText(requireContext(), "Displaying difference map.", Toast.LENGTH_SHORT).show()
    }
}
