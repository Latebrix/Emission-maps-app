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
import org.osmdroid.views.overlay.ScaleBarOverlay
import org.osmdroid.views.overlay.compass.CompassOverlay
import org.osmdroid.views.overlay.mylocation.GpsMyLocationProvider
import org.osmdroid.views.overlay.mylocation.MyLocationNewOverlay

class MainFragment : Fragment() {

    private lateinit var binding: FragmentHomeBinding
    private lateinit var map: MapView
    private lateinit var controller: IMapController
    private val viewModel: MainViewModel by viewModels()
    private lateinit var bindingBar: SubBarBinding
    private var cities: List<String> = emptyList()

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
            showFilterBottomSheet()
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

    private fun observeViewModel() {
        viewModel.cities.observe(viewLifecycleOwner) { result ->
            result.onSuccess {
                cities = it
            }.onFailure {
                Toast.makeText(requireContext(), it.message, Toast.LENGTH_SHORT).show()
            }
        }

        viewModel.data.observe(viewLifecycleOwner) { result ->
            result.onSuccess { data ->
                map.overlays.clear()
                addScaleBar()
                addCompass()
                for (point in data) {
                    val marker = Marker(map)
                    marker.position = GeoPoint(point.first, point.second)
                    marker.title = "${point.third}"
                    map.overlays.add(marker)
                }
                map.invalidate()
            }.onFailure {
                Toast.makeText(requireContext(), it.message, Toast.LENGTH_SHORT).show()
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
            FilterBottomSheetFragment(cities) { _, selectedYear, cityFileUrl, _ ->
                viewModel.fetchData(cityFileUrl, selectedYear)
            }
        bottomSheetFragment.show(childFragmentManager, bottomSheetFragment.tag)
    }

    private fun inflateMyViews() {
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
}
