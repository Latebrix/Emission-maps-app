package on.emission.maps.ui.home

import FilterBottomSheetFragment
import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.fragment.app.Fragment
import on.emission.maps.databinding.FragmentHomeBinding
import on.emission.maps.parser.data.DataAnalyzer
import on.emission.maps.parser.data.DataFetcher
import org.osmdroid.config.Configuration
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.Marker
import org.osmdroid.api.IMapController
import on.emission.maps.R
import on.emission.maps.databinding.SubBarBinding
import org.osmdroid.views.overlay.ScaleBarOverlay
import org.osmdroid.views.overlay.compass.CompassOverlay
import org.osmdroid.views.overlay.mylocation.GpsMyLocationProvider
import org.osmdroid.views.overlay.mylocation.MyLocationNewOverlay

class MainFragment : Fragment() {

    private lateinit var binding: FragmentHomeBinding
    private lateinit var map: MapView
    private lateinit var controller: IMapController
    private val dataFetcher = DataFetcher()
    private lateinit var bindingBar: SubBarBinding
    private lateinit var cities: List<String>


    private lateinit var locationOverlay: MyLocationNewOverlay

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        binding = FragmentHomeBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)

        inflateMyViews()
        setupMap()
        fetchCities()

        binding.toolFab.setOnClickListener {
            getCurrentLocation()
        }

        binding.fab.setOnClickListener {
            try {
                showFilterBottomSheet()
            }catch (e: Exception) { }
        }
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

    private fun fetchCities() {
        val baseUrl =
            "https://gml.noaa.gov/dv/data/index.php?parameter_name=Carbon%252BDioxide&frequency=Discrete"
        dataFetcher.fetchCities(baseUrl) { cityList ->
            if (cityList != null) {
                cities = cityList
            } else {
                Toast.makeText(requireContext(), "Failed to load cities", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun getCurrentLocation() {
        try {

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
            fun LocationGetter(callback: (GeoPoint?) -> Unit){
                locationOverlay = MyLocationNewOverlay(GpsMyLocationProvider(requireContext()), map)
                locationOverlay.enableMyLocation()
                map.overlays.add(locationOverlay)

                callback(locationOverlay.myLocation)

            }

            LocationGetter() { location ->
                if (location != null) {
                    controller.setCenter(location)
                }
            }

        } catch (e: Exception) {
            Toast.makeText(requireContext(), "Failed to get location", Toast.LENGTH_SHORT).show()
        }

    }

    private fun fetchAndDisplayData(year: String, url: String, gasType: String) {
        analyzeAndDisplayData(url, gasType, year)
    }

    fun analyzeAndDisplayData(url: String, gasType: String, selectedYear: String) {
        val iconRes = if (gasType == "CO2") R.drawable.red_circle else R.drawable.yellow_circle
        try {
            val dataAnalyzer = DataAnalyzer(requireContext())
            dataAnalyzer.analyzeData(url, gasType, selectedYear) { result ->
                if (result != null) {
                    for (point in result) {
                        val marker = Marker(map)
                        marker.position = GeoPoint(point.first, point.second)
                        marker.title = "$selectedYear, $gasType: ${point.third}"
                        marker.icon = resources.getDrawable(iconRes, null)
                        map.overlays.add(marker)
                    }
                    map.invalidate()
                } else {
                    Toast.makeText(requireContext(), "No data available", Toast.LENGTH_SHORT).show()
                }
            }
        } catch (e: Exception) {
            Toast.makeText(requireContext(), "error: ${e.toString()}", Toast.LENGTH_SHORT).show()
        }
    }

    private fun showFilterBottomSheet() {
        val bottomSheetFragment =
            FilterBottomSheetFragment(cities) { selectedCity, selectedYear, cityFileUrl, selectedGasType ->
                try {
                    fetchAndDisplayData(selectedYear, cityFileUrl, selectedGasType)
                } catch (e: Exception) {
                    e.printStackTrace()
                    Toast.makeText(requireContext(), "Failed to fetch data", Toast.LENGTH_SHORT)
                        .show()
                }
            }
        bottomSheetFragment.show(childFragmentManager, bottomSheetFragment.tag)
    }

    fun inflateMyViews() {
        bindingBar = SubBarBinding.inflate(LayoutInflater.from(requireContext()), binding.bar, true)
        bindingBar.root.setOnClickListener {
            binding.fab.performClick()
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
                // Permission granted, fetch the location
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
        private const val LOCATION_PERMISSION_REQUEST_CODE =
            1000
    }
}
