import android.content.DialogInterface
import android.os.Bundle
import android.view.*
import android.widget.*
import androidx.activity.OnBackPressedCallback
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import on.emission.maps.R
import on.emission.maps.parser.data.DataFetcher

class FilterBottomSheetFragment(
    private val cities: List<String>,
    private val applyFilterCallback: (String, String, String, String) -> Unit
) : BottomSheetDialogFragment() {

    private lateinit var citySpinner: Spinner
    private lateinit var yearSpinner: Spinner
    private lateinit var gasTypeSpinner: Spinner
    private lateinit var progressBar: ProgressBar
    private lateinit var applyButton: Button
    private var cities_now = cities
    lateinit var cityFileUrl: String
    private var selectedGasType: String = "CO2"
    private var isLoading = true



    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        val view = inflater.inflate(R.layout.bottom_sheet_filter, container, false)

        progressBar = view.findViewById(R.id.progress_bar)
        citySpinner = view.findViewById(R.id.spinner_city)
        yearSpinner = view.findViewById(R.id.spinner_year)
        gasTypeSpinner = view.findViewById(R.id.spinner_gas_type)
        applyButton = view.findViewById(R.id.apply_filter)

        applyButton.visibility = View.GONE
        citySpinner.adapter = ArrayAdapter(requireContext(), android.R.layout.simple_spinner_dropdown_item, cities_now)

        val gasTypes = listOf("CO2", "CH4", "Ozone", "Aerosols", "isoprene")
        gasTypeSpinner.adapter = ArrayAdapter(requireContext(), android.R.layout.simple_spinner_dropdown_item, gasTypes)

        gasTypeSpinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>, view: View?, position: Int, id: Long) {
                selectedGasType = gasTypes[position]
                val baseUrl = getBase()

                progressBar.visibility = View.VISIBLE
                applyButton.visibility = View.GONE
                isLoading = true
                setCancelable(false)

                DataFetcher().fetchCities(baseUrl) { cityList ->
                    progressBar.visibility = View.GONE
                    isLoading = false
                    setCancelable(true)

                    if (cityList != null) {
                        cities_now = cityList
                        citySpinner.adapter = ArrayAdapter(
                            requireContext(),
                            android.R.layout.simple_spinner_dropdown_item,
                            cities_now
                        )
                        Toast.makeText(requireContext(), "Cities updated for $selectedGasType", Toast.LENGTH_SHORT).show()
                    } else {
                        Toast.makeText(requireContext(), "Failed to load cities for $selectedGasType", Toast.LENGTH_SHORT).show()
                    }
                }
            }

            override fun onNothingSelected(parent: AdapterView<*>) {}
        }


        citySpinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>, view: View?, position: Int, id: Long) {
                progressBar.visibility = View.VISIBLE
                applyButton.visibility = View.GONE
                isLoading = true
                setCancelable(false)

                val selectedCity = cities_now[position]
                fetchDataFilesForCity(selectedCity) { cityFiles ->
                    if (cityFiles != null && cityFiles.isNotEmpty()) {
                        fetchYearsForCity(cityFiles.first()) { years ->
                            if (years != null && years.isNotEmpty()) {
                                yearSpinner.adapter = ArrayAdapter(
                                    requireContext(),
                                    android.R.layout.simple_spinner_dropdown_item,
                                    years
                                )
                                applyButton.visibility = View.VISIBLE
                                applyButton.isEnabled = true
                            } else {
                                Toast.makeText(requireContext(), "No years found for the selected city", Toast.LENGTH_SHORT).show()
                                Toast.makeText(requireContext(), cityFiles.first(), Toast.LENGTH_SHORT).show()
                            }
                            progressBar.visibility = View.GONE
                            isLoading = false
                            setCancelable(true)
                        }
                    } else {
                        Toast.makeText(requireContext(), "No files found for the selected city", Toast.LENGTH_SHORT).show()
                        progressBar.visibility = View.GONE
                        isLoading = false
                        setCancelable(true)
                    }
                }
            }

            override fun onNothingSelected(parent: AdapterView<*>) {}
        }

        applyButton.setOnClickListener {
            if (!isLoading) {
                val selectedCity = citySpinner.selectedItem.toString()
                val selectedYear = yearSpinner.selectedItem.toString()
                applyFilterCallback(selectedCity, selectedYear, cityFileUrl, selectedGasType)
                dismiss()
            }
        }

        return view
    }

    //val gasTypes = listOf("CO2", "CH4", "Ozone", "Aerosols", "isoprene")

    private fun fetchDataFilesForCity(cityName: String, callback: (List<String>?) -> Unit) {
        DataFetcher().fetchDataFilesForCity(getBase(), cityName) { cityFiles ->
            callback(cityFiles)
        }
    }

    private fun fetchYearsForCity(cityDataUrl: String, callback: (List<Int>?) -> Unit) {
        cityFileUrl = "https://gml.noaa.gov/data/$cityDataUrl"
        DataFetcher().fetchCityData(cityFileUrl) { years ->
            callback(years)
        }
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

    fun getBase(): String{
        return if (selectedGasType == "CO2") {
            "https://gml.noaa.gov/dv/data/index.php?parameter_name=Carbon%252BDioxide&frequency=Discrete"
        } else if (selectedGasType == "CH4") {
            "https://gml.noaa.gov/dv/data/index.php?category=Greenhouse%252BGases&parameter_name=Methane&frequency=Discrete"
        } else if (selectedGasType == "Ozone") {
            "https://gml.noaa.gov/data/data.php?parameter_name=Ozone&frequency=Discrete"
        } else if (selectedGasType == "Aerosol") {
            "https://gml.noaa.gov/data/data.php?parameter_name=Aerosols&frequency=Discrete"
        } else {
            "https://gml.noaa.gov/data/data.php?parameter_name=isoprene&frequency=Discrete"
        }
    }


}
