package on.emission.maps.data

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.launch

class MainViewModel : ViewModel() {

    private val repository = Repository()

    private val _cities = MutableLiveData<Result<List<String>>>()
    val cities: LiveData<Result<List<String>>> = _cities

    private val _years = MutableLiveData<Result<List<Int>>>()
    val years: LiveData<Result<List<Int>>> = _years

    private val _data = MutableLiveData<Result<List<Triple<Double, Double, Double>>>>()
    val data: LiveData<Result<List<Triple<Double, Double, Double>>>> = _data

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

    fun fetchData(url: String, selectedYear: String) {
        viewModelScope.launch {
            _data.value = repository.analyzeData(url, selectedYear)
        }
    }
}
