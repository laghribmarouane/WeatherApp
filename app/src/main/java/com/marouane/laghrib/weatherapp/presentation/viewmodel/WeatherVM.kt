
package com.marouane.laghrib.weatherapp.viewmodel

import android.location.Location
import android.text.Editable
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.marouane.laghrib.weatherapp.entities.CurrentWeather
import com.marouane.laghrib.weatherapp.entities.ForecastItem
import com.marouane.laghrib.weatherapp.data.repository.WeatherRepository
import kotlinx.coroutines.launch

enum class WeatherApiStatus { START, LOADING, ERROR, DONE }

class WeatherViewModel : ViewModel() {
    private val _currentLocation = MutableLiveData<Location>()

    private val repository: WeatherRepository = WeatherRepository()

    private val query = MutableLiveData<String>()

    private val _currentWeather = repository.currentWeather
    val currentWeather: LiveData<CurrentWeather>
        get() = _currentWeather

    private val _forecasts = repository.forecasts
    val forecasts: LiveData<List<ForecastItem>>
        get() = _forecasts

    private val _status = MutableLiveData<WeatherApiStatus>()
    val status: LiveData<WeatherApiStatus>
        get() = _status

    private val _resultForCurrentLocation = MutableLiveData<Boolean>()
    val resultForCurrentLocation: LiveData<Boolean>
        get() = _resultForCurrentLocation

    private val _errorMessage = MutableLiveData<String?>()
    val errorMessage: LiveData<String?>
        get() = _errorMessage

    private val _toastWarningEvent = MutableLiveData<Boolean?>()
    val toastWarningEvent: LiveData<Boolean?>
        get() = _toastWarningEvent

    private val _typingCompleteEvent = MutableLiveData<Boolean?>()
    val typingCompleteEvent: LiveData<Boolean?>
        get() = _typingCompleteEvent

    init {
        query.value = ""
        _status.value = WeatherApiStatus.START
    }

    fun onLocationUpdated(location: Location) {
        _currentLocation.value = location
        getWeatherDataForCurrentLocation()
    }

    private fun getWeatherDataForCurrentLocation() = launchDataLoad {
        _currentLocation.value?.let { it ->
            repository.getWeatherAndForecasts(it.latitude, it.longitude)
            _resultForCurrentLocation.value = true
        }
    }


    fun afterCityTextChange(e: Editable) {
        query.value = e.trim().toString()
    }

    fun onStartSearching() {
        _typingCompleteEvent.value = true

        query.value?.let {
            if (it.isBlank())
                _toastWarningEvent.value = true
            else
                getWeatherDataForCity(it)
        }
    }


    private fun getWeatherDataForCity(cityName: String) = launchDataLoad {
        _resultForCurrentLocation.value = false
        repository.getWeatherAndForecasts(cityName)
    }


    private fun launchDataLoad(executor: suspend () -> Unit) {
        viewModelScope.launch {
            try {
                _status.value = WeatherApiStatus.LOADING
                executor()
                _status.value = WeatherApiStatus.DONE
            } catch (error: Throwable) {
                _errorMessage.value = error.message
                _status.value = WeatherApiStatus.ERROR
            }
        }
    }
}