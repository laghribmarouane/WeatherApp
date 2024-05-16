package com.marouane.laghrib.weatherapp.domain.usecase.business

import android.annotation.SuppressLint
import android.content.Context
import android.content.SharedPreferences
import android.location.Location
import android.os.Looper
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationCallback
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationResult
import com.google.android.gms.location.LocationServices
import com.marouane.laghrib.weatherapp.data.enums.asTempString
import com.marouane.laghrib.weatherapp.data.repository.WeatherRepository
import com.marouane.laghrib.weatherapp.entities.CurrentWeather
import com.marouane.laghrib.weatherapp.widget.utils.WIDGET_CITY
import com.marouane.laghrib.weatherapp.widget.utils.WIDGET_DESCR
import com.marouane.laghrib.weatherapp.widget.utils.WIDGET_ICON
import com.marouane.laghrib.weatherapp.widget.utils.WIDGET_PREF
import com.marouane.laghrib.weatherapp.widget.utils.WIDGET_TEMP
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import timber.log.Timber
import java.util.concurrent.TimeUnit

class WidgetUpdateWorker(private val context: Context, parameters: WorkerParameters) :
    CoroutineWorker(context, parameters) {

    private val repository = WeatherRepository()

    private val job = Job()
    private val coroutineScope = CoroutineScope(Dispatchers.Main + job)

    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private lateinit var locationCallback: LocationCallback

    override suspend fun doWork(): Result {
        try {
            Timber.d("Work request is run")
            startLocationUpdates()
        } catch (e: Exception) {
            Timber.d("Work request failed: ${e.message}")
            return Result.retry()
        }
        return Result.success()
    }


    @SuppressLint("MissingPermission")
    private fun startLocationUpdates() {

        fusedLocationClient = LocationServices.getFusedLocationProviderClient(context)

        locationCallback = getLocationCallback()

        fusedLocationClient.requestLocationUpdates(
            getLocationRequest(),
            locationCallback,
            Looper.getMainLooper()
        )
    }


    private fun getLocationCallback(): LocationCallback {
        return object : LocationCallback() {

            override fun onLocationResult(locationResult: LocationResult?) {
                super.onLocationResult(locationResult)
                val location = locationResult?.lastLocation ?: return
                updateWeather(location)
            }
        }
    }


    private fun getLocationRequest(): LocationRequest {
        return LocationRequest().apply {
            interval = TimeUnit.SECONDS.toMillis(60)
            fastestInterval = TimeUnit.SECONDS.toMillis(30)
            maxWaitTime = TimeUnit.MINUTES.toMillis(2)
            priority = LocationRequest.PRIORITY_HIGH_ACCURACY
        }
    }


    private fun updateWeather(location: Location) {
        coroutineScope.launch {
            withContext(Dispatchers.IO) {
                repository.getWeather(location.latitude, location.longitude)
                val currentWeather = repository.widgetWeather
                currentWeather?.let { weather ->
                    savedWeatherToSharedPrefsAndNotifyWidget(weather)
                }
            }
        }
    }


    private fun savedWeatherToSharedPrefsAndNotifyWidget(weather: CurrentWeather) {
        val sharedPref = context
            .getSharedPreferences(WIDGET_PREF, Context.MODE_PRIVATE)
        val editor: SharedPreferences.Editor = sharedPref.edit()
        editor.putString(WIDGET_ICON, weather.weathers[0].iconId)
        editor.putString(WIDGET_CITY, weather.city)
        editor.putString(WIDGET_DESCR, weather.weathers[0].description)
        editor.putString(WIDGET_TEMP, weather.temp.asTempString())
        editor.apply()
        WeatherAppWidget.notifyAppWidgetViewDataChanged(context)
    }

    companion object {
        const val WORK_NAME = "com.breiter.weathercheckerapp.WidgetUpdateWorker"
    }
}