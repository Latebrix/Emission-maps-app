import android.content.DialogInterface
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.ProgressBar
import android.widget.Spinner
import android.widget.Toast
import androidx.activity.OnBackPressedCallback
import androidx.fragment.app.activityViewModels
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import on.emission.maps.R
import on.emission.maps.data.MainViewModel
import on.emission.maps.data.Repository

class FilterBottomSheetFragment(
    private val cities: List<String>,
    private val applyFilterCallback: (
        selectedCity: String,
        selectedYear: String,
        cityFileUrl: String,
        gasType: String,
        selectedParser: String,
        colormapName: String,
        colormapMin: Double?,
        colormapMax: Double?,
        useLogScale: Boolean
    ) -> Unit
) : BottomSheetDialogFragment() {

    private lateinit var citySpinner: Spinner
    private lateinit var yearSpinner: Spinner
    private lateinit var gasTypeSpinner: Spinner
    private lateinit var parserSpinner: Spinner
    private lateinit var colormapSpinner: Spinner
    private lateinit var minRangeEditText: android.widget.EditText
    private lateinit var maxRangeEditText: android.widget.EditText
    private lateinit var logScaleSwitch: androidx.appcompat.widget.SwitchCompat
    private lateinit var progressBar: ProgressBar
    private lateinit var applyButton: Button
    private var citiesNow = cities
    private lateinit var cityFileUrl: String
    private var selectedGasType: String = "CO2"
    private var isLoading = true

    private val viewModel: MainViewModel by activityViewModels()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.bottom_sheet_filter, container, false)

        progressBar = view.findViewById(R.id.progress_bar)
        citySpinner = view.findViewById(R.id.spinner_city)
        yearSpinner = view.findViewById(R.id.spinner_year)
        gasTypeSpinner = view.findViewById(R.id.spinner_gas_type)
        parserSpinner = view.findViewById(R.id.spinner_parser)
        colormapSpinner = view.findViewById(R.id.spinner_colormap)
        minRangeEditText = view.findViewById(R.id.edit_text_min_range)
        maxRangeEditText = view.findViewById(R.id.edit_text_max_range)
        logScaleSwitch = view.findViewById(R.id.switch_log_scale)
        applyButton = view.findViewById(R.id.apply_filter)

        applyButton.visibility = View.GONE
        citySpinner.adapter =
            ArrayAdapter(requireContext(), android.R.layout.simple_spinner_dropdown_item, citiesNow)

        val gasTypes = listOf("CO2", "CH4", "Ozone", "Aerosols", "isoprene")
        gasTypeSpinner.adapter =
            ArrayAdapter(requireContext(), android.R.layout.simple_spinner_dropdown_item, gasTypes)

        val parsers = listOf("Automatic", "Standard", "Alternative")
        parserSpinner.adapter =
            ArrayAdapter(requireContext(), android.R.layout.simple_spinner_dropdown_item, parsers)

        val colormaps = listOf("Simple") // For now
        colormapSpinner.adapter =
            ArrayAdapter(requireContext(), android.R.layout.simple_spinner_dropdown_item, colormaps)

        gasTypeSpinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(
                parent: AdapterView<*>,
                view: View?,
                position: Int,
                id: Long
            ) {
                selectedGasType = gasTypes[position]
                val baseUrl = getBase()

                progressBar.visibility = View.VISIBLE
                applyButton.visibility = View.GONE
                isLoading = true
                isCancelable = false

                viewModel.fetchCities(baseUrl)
            }

            override fun onNothingSelected(parent: AdapterView<*>) {}
        }

        citySpinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(
                parent: AdapterView<*>,
                view: View?,
                position: Int,
                id: Long
            ) {
                progressBar.visibility = View.VISIBLE
                applyButton.visibility = View.GONE
                isLoading = true
                isCancelable = false

                val selectedCity = citiesNow[position]
                fetchDataFilesForCity(selectedCity) { result ->
                    result.onSuccess { cityFiles ->
                        if (cityFiles.isNotEmpty()) {
                            fetchYearsForCity(cityFiles.first())
                        } else {
                            Toast.makeText(
                                requireContext(),
                                "No files found for the selected city",
                                Toast.LENGTH_SHORT
                            ).show()
                            progressBar.visibility = View.GONE
                            isLoading = false
                            isCancelable = true
                        }
                    }.onFailure {
                        Toast.makeText(requireContext(), it.message, Toast.LENGTH_SHORT).show()
                        progressBar.visibility = View.GONE
                        isLoading = false
                        isCancelable = true
                    }
                }
            }

            override fun onNothingSelected(parent: AdapterView<*>) {}
        }

        applyButton.setOnClickListener {
            if (!isLoading) {
                applyFilterCallback(
                    citySpinner.selectedItem.toString(),
                    yearSpinner.selectedItem.toString(),
                    cityFileUrl,
                    selectedGasType,
                    parserSpinner.selectedItem.toString(),
                    colormapSpinner.selectedItem.toString(),
                    minRangeEditText.text.toString().toDoubleOrNull(),
                    maxRangeEditText.text.toString().toDoubleOrNull(),
                    logScaleSwitch.isChecked
                )
                dismiss()
            }
        }

        observeViewModel()

        return view
    }

    private fun observeViewModel() {
        viewModel.cities.observe(viewLifecycleOwner) { result ->
            progressBar.visibility = View.GONE
            isLoading = false
            isCancelable = true

            result.onSuccess { cityList ->
                citiesNow = cityList
                citySpinner.adapter = ArrayAdapter(
                    requireContext(),
                    android.R.layout.simple_spinner_dropdown_item,
                    citiesNow
                )
                Toast.makeText(
                    requireContext(),
                    "Cities updated for $selectedGasType",
                    Toast.LENGTH_SHORT
                ).show()
            }.onFailure {
                Toast.makeText(
                    requireContext(),
                    "Failed to load cities for $selectedGasType",
                    Toast.LENGTH_SHORT
                ).show()
            }
        }

        viewModel.years.observe(viewLifecycleOwner) { result ->
            result.onSuccess { years ->
                if (years.isNotEmpty()) {
                    yearSpinner.adapter = ArrayAdapter(
                        requireContext(),
                        android.R.layout.simple_spinner_dropdown_item,
                        years.map { it.toString() }
                    )
                    applyButton.visibility = View.VISIBLE
                    applyButton.isEnabled = true
                } else {
                    Toast.makeText(
                        requireContext(),
                        "No years found for the selected city",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            }.onFailure {
                Toast.makeText(requireContext(), it.message, Toast.LENGTH_SHORT).show()
            }
            progressBar.visibility = View.GONE
            isLoading = false
            isCancelable = true
        }
    }

    private fun fetchDataFilesForCity(cityName: String, callback: (Result<List<String>>) -> Unit) {
        val repository = Repository()
        CoroutineScope(Dispatchers.IO).launch {
            val result = repository.fetchDataFilesForCity(getBase(), cityName)
            CoroutineScope(Dispatchers.Main).launch {
                callback(result)
            }
        }
    }

    private fun fetchYearsForCity(cityDataUrl: String) {
        cityFileUrl = "https://gml.noaa.gov/data/$cityDataUrl"
        viewModel.fetchYears(cityFileUrl)
    }

    override fun onCancel(dialog: DialogInterface) {
        if (!isLoading) {
            super.onCancel(dialog)
        }
    }

    override fun onDismiss(dialog: DialogInterface) {
        if (!isLoading) {
            super.onDismiss(dialog)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val callback = object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                if (!isLoading) {
                    dismiss()
                }
            }
        }

        requireActivity().onBackPressedDispatcher.addCallback(this, callback)
    }

    private fun getBase(): String {
        return when (selectedGasType) {
            "CO2" -> on.emission.maps.util.Config.CO2_URL
            "CH4" -> on.emission.maps.util.Config.CH4_URL
            "Ozone" -> on.emission.maps.util.Config.OZONE_URL
            "Aerosol" -> on.emission.maps.util.Config.AEROSOL_URL
            else -> on.emission.maps.util.Config.ISOPRENE_URL
        }
    }
}
