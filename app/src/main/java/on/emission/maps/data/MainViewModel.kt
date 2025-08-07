package on.emission.maps.data

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.launch
import on.emission.maps.data.model.MapDataPoint
import on.emission.maps.data.model.TimeSeriesData

class MainViewModel : ViewModel() {

    private val repository = Repository()

    private val _cities = MutableLiveData<Result<List<String>>>()
    val cities: LiveData<Result<List<String>>> = _cities

    private val _years = MutableLiveData<Result<List<Int>>>()
    val years: LiveData<Result<List<Int>>> = _years

    private val _data = MutableLiveData<Result<List<MapDataPoint>>>()
    val data: LiveData<Result<List<MapDataPoint>>> = _data

    private val _customData = MutableLiveData<List<MapDataPoint>>()
    val customData: LiveData<List<MapDataPoint>> = _customData

    fun displayCustomData(data: List<MapDataPoint>) {
        _customData.value = data
    }

    fun fetchCities(baseUrl: String) {
        viewModelScope.launch {
            _cities.value = repository.fetchCities(baseUrl)
        }
    }

    fun fetchYears(cityDataUrl: String) {
        viewModelScope.launch {
            _years.value = repository.fetchCityData(cityDataUrl)
        }
    }

    fun fetchData(url: String, selectedYear: String, parser: String) {
        viewModelScope.launch {
            _data.value = repository.analyzeData(url, selectedYear, parser)
        }
    }

    private val _animationTimeSeriesData = MutableLiveData<Result<TimeSeriesData>>()
    val animationTimeSeriesData: LiveData<Result<TimeSeriesData>> = _animationTimeSeriesData

    fun fetchAnimationTimeSeries(urlString: String) {
        viewModelScope.launch {
            _animationTimeSeriesData.value = repository.fetchTimeSeriesData(urlString)
        }
    }

    private val _probeTimeSeriesData = MutableLiveData<Result<TimeSeriesData>>()
    val probeTimeSeriesData: LiveData<Result<TimeSeriesData>> = _probeTimeSeriesData

    fun fetchProbeTimeSeries(urlString: String) {
        viewModelScope.launch {
            _probeTimeSeriesData.value = repository.fetchTimeSeriesData(urlString)
        }
    }

    private val _parsedUrls = MutableLiveData<List<String>>()
    val parsedUrls: LiveData<List<String>> = _parsedUrls

    fun fetchParsedUrls() {
        _parsedUrls.value = repository.getParsedUrls()
    }
}
