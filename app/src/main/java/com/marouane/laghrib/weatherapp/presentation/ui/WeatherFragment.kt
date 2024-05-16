package com.marouane.laghrib.weatherapp.presentation.ui

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import android.os.Looper
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.app.ActivityCompat
import androidx.core.view.updatePadding
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import com.google.android.gms.location.LocationCallback
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationResult
import com.google.android.gms.location.LocationServices
import com.marouane.laghrib.weatherapp.adapters.ForecastAdapter
import com.marouane.laghrib.weatherapp.databinding.WeatherFragmentBinding
import com.marouane.laghrib.weatherapp.data.enums.Constants
import com.marouane.laghrib.weatherapp.viewmodel.WeatherViewModel
import java.util.concurrent.TimeUnit

class WeatherFragment : Fragment() {
    private val binding: WeatherFragmentBinding by lazy {
        WeatherFragmentBinding.inflate(layoutInflater)

    }

    private val weatherViewModel: WeatherViewModel by lazy {
        ViewModelProvider(this).get(WeatherViewModel::class.java)
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {

        binding.apply {
            lifecycleOwner = viewLifecycleOwner
            viewModel = weatherViewModel
            forecastList.adapter = ForecastAdapter()
        }
        var adapter = ArrayAdapter(requireContext(),android.R.layout.test_list_item, Constants.CITY)
        binding.queryEditText.setAdapter(adapter)
        requestLastLocationOrStartLocationUpdates()
        setWindowInsets()

        return binding.root
    }

    private fun setWindowInsets() {
        val weatherLayout: ConstraintLayout = binding.weatherLayout
        weatherLayout.setOnApplyWindowInsetsListener { view, insets ->
            view.updatePadding(
                top = insets.systemWindowInsetTop,
                bottom = insets.systemWindowInsetBottom
            )
            insets
        }
    }


    private fun requestLastLocationOrStartLocationUpdates() {
        if (ActivityCompat.checkSelfPermission(
                requireContext(),
                Manifest.permission.ACCESS_FINE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(
                requireContext(),
                Manifest.permission.ACCESS_BACKGROUND_LOCATION
            ) != PackageManager.PERMISSION_GRANTED
        ) {

        } else {
            val fusedLocationClient = LocationServices
                .getFusedLocationProviderClient(requireContext())

            fusedLocationClient.lastLocation.addOnSuccessListener { location ->
                if (location != null)
                    weatherViewModel.onLocationUpdated(location)
                else
                    fusedLocationClient.requestLocationUpdates(
                        getLocationRequest(),
                        getLocationCallback(),
                        Looper.getMainLooper()
                    )
            }
        }
    }


    private fun getLocationCallback(): LocationCallback {
        return object : LocationCallback() {
            override fun onLocationResult(locationResult: LocationResult?) {
                super.onLocationResult(locationResult)

                val location = locationResult?.lastLocation ?: return
                weatherViewModel.onLocationUpdated(location)
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

}