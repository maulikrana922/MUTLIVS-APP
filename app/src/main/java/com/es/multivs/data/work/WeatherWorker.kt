package com.es.multivs.data.work

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import androidx.work.workDataOf
import com.es.multivs.data.repository.WeatherRepository
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject

/**
 * Created by Marko on 12/20/2021.
 * Etrog Systems LTD.
 */
@HiltWorker
class WeatherWorker @AssistedInject constructor(
    @Assisted context: Context,
    @Assisted workerParams: WorkerParameters,
    private val repository: WeatherRepository
) : CoroutineWorker(context, workerParams) {


    override suspend fun doWork(): Result {

        val weather = repository.fetchWeather()

        weather?.let {
            val weatherString = WeatherForecast.serializeWeatherToJson(it)
            return Result.success(
                workDataOf(DATA_TAG to weatherString)
            )
        }
        return Result.failure(workDataOf(DATA_TAG to WEATHER_ERROR))
    }

    companion object {
        const val TAG = "WeatherWorker"
        const val DATA_TAG = "weather_work"
        const val WEATHER_ERROR = "Could not get weather report"
    }
}