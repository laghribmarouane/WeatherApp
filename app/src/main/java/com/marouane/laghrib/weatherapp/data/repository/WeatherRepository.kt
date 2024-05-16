package com.marouane.laghrib.weatherapp.data.repository

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.Transformations
import com.marouane.laghrib.weatherapp.entities.CurrentWeather
import com.marouane.laghrib.weatherapp.entities.ForecastItem
import com.marouane.laghrib.weatherapp.network.CurrentWeatherDTO
import com.marouane.laghrib.weatherapp.network.WeatherApi
import com.marouane.laghrib.weatherapp.network.asDomainModel
import timber.log.Timber

class WeatherRepository {

    private val _currentWeather = MutableLiveData<CurrentWeatherDTO>()
    val currentWeather: LiveData<CurrentWeather> =
        Transformations.map(_currentWeather) {
            it.asDomainModel()
        }

    private val _forecasts = MutableLiveData<List<ForecastItem>>()
    val forecasts: LiveData<List<ForecastItem>>
        get() = _forecasts

    private var _widgetWeather: CurrentWeather? = null
    val widgetWeather: CurrentWeather?
        get() = _widgetWeather

    suspend fun getWeatherAndForecasts(lat: Double, lon: Double) {
        try {
            _currentWeather.value = WeatherApi.retrofitService.getCurrentWeather(lat, lon)
            val forecastResponse = WeatherApi.retrofitService.getForecast(lat, lon)
            _forecasts.value = forecastResponse.asDomainModel()
        } catch (t: Throwable) {
            throw Throwable(t)
        }
    }

    suspend fun getWeatherAndForecasts(cityName: String) {
        try {
            _currentWeather.value = WeatherApi.retrofitService.getCurrentWeather(cityName)
            val forecastResponse = WeatherApi.retrofitService.getForecast(cityName)
            _forecasts.value = forecastResponse.asDomainModel()
        } catch (t: Throwable) {
            throw Throwable(t)
        }
    }

    suspend fun getWeather(lat: Double, lon: Double) {
        try {
            _widgetWeather = WeatherApi.retrofitService.getCurrentWeather(lat, lon).asDomainModel()
        } catch (t: Throwable) {
            Timber.d(t)
        }
    }
}