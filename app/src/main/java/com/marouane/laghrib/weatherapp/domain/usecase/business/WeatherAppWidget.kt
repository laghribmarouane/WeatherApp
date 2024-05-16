package com.marouane.laghrib.weatherapp.domain.usecase.business

import android.Manifest
import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Context.MODE_PRIVATE
import android.content.Intent
import android.content.SharedPreferences
import android.widget.RemoteViews
import android.widget.Toast
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import com.marouane.laghrib.weatherapp.R
import com.marouane.laghrib.weatherapp.data.enums.asResourceId
import com.marouane.laghrib.weatherapp.data.enums.hasPermission
import com.marouane.laghrib.weatherapp.presentation.ui.MainActivity
import com.marouane.laghrib.weatherapp.widget.utils.WIDGET_CITY
import com.marouane.laghrib.weatherapp.widget.utils.WIDGET_DESCR
import com.marouane.laghrib.weatherapp.widget.utils.WIDGET_ICON
import com.marouane.laghrib.weatherapp.widget.utils.WIDGET_PREF
import com.marouane.laghrib.weatherapp.widget.utils.WIDGET_TEMP
import timber.log.Timber
import java.util.concurrent.TimeUnit


class WeatherAppWidget : AppWidgetProvider() {
    override fun onUpdate(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetIds: IntArray
    ) {


        for (appWidgetId in appWidgetIds)
            updateAppWidget(
                context,
                appWidgetManager,
                appWidgetId
            )
    }

    companion object {


        private fun updateAppWidget(
            context: Context,
            appWidgetManager: AppWidgetManager,
            appWidgetId: Int
        ) {
            val sharedPref: SharedPreferences = context.getSharedPreferences(
                WIDGET_PREF, MODE_PRIVATE
            )
            val icon: String =

                sharedPref.getString(WIDGET_ICON, context.getString(R.string.widget_default))
                    .toString()
            val temp: String =
                sharedPref.getString(WIDGET_TEMP, context.getString(R.string.default_null))
                    .toString()
            val descr: String =
                sharedPref.getString(WIDGET_DESCR, context.getString(R.string.default_null))
                    .toString()
            val city: String =
                sharedPref.getString(WIDGET_CITY, context.getString(R.string.default_null))
                    .toString()


            val views = RemoteViews(
                context.packageName,
                R.layout.app_widget
            )

            views.apply {
                setImageViewResource(R.id.weather_icon, icon.asResourceId())
                setTextViewText(R.id.appwidget_temp, temp)
                setTextViewText(R.id.appwidget_city, city)
                setTextViewText(R.id.appwidget_description, descr.capitalize())
            }

            val intent = Intent(context, MainActivity::class.java)

            val pendingIntent = PendingIntent.getActivity(
                context,
                appWidgetId,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT
            )
            views.setOnClickPendingIntent(R.id.widget_root, pendingIntent)

            appWidgetManager.updateAppWidget(appWidgetId, views)
        }


        fun notifyAppWidgetViewDataChanged(context: Context) {
            val appWidgetManager: AppWidgetManager = AppWidgetManager.getInstance(context)

            val appWidgetIds = appWidgetManager.getAppWidgetIds(
                ComponentName(
                    context,
                    WeatherAppWidget::class.java
                )
            )
            for (appWidgetId in appWidgetIds)
                updateAppWidget(
                    context,
                    appWidgetManager,
                    appWidgetId
                )
        }
    }


    override fun onEnabled(context: Context) {
        super.onEnabled(context)

        if (context.hasPermission(Manifest.permission.ACCESS_FINE_LOCATION) &&
            context.hasPermission(Manifest.permission.ACCESS_BACKGROUND_LOCATION)
        )
            setupRecurringWork(context)
         else
            Toast.makeText(
                context,
                R.string.location_permission_rejected,
                Toast.LENGTH_LONG
            ).show()
    }


    private fun setupRecurringWork(context: Context) {
        val repeatingRequest =
            PeriodicWorkRequestBuilder<WidgetUpdateWorker>(15, TimeUnit.MINUTES)
                .build()

        WorkManager.getInstance(context).enqueueUniquePeriodicWork(
            WidgetUpdateWorker.WORK_NAME,
            ExistingPeriodicWorkPolicy.KEEP,
            repeatingRequest
        )
    }


    override fun onDisabled(context: Context) {
        super.onDisabled(context)
        WorkManager.getInstance(context)
            .cancelUniqueWork(WidgetUpdateWorker.WORK_NAME)

        Timber.i("Work request cancelled")
    }
}