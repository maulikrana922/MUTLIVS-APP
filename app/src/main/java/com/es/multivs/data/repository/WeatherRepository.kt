package com.es.multivs.data.repository

import com.es.multivs.data.database.location.LocationDbHelper
import com.es.multivs.data.network.retrofit.Api
import com.google.gson.annotations.SerializedName
import java.io.Serializable
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Created by Marko on 12/20/2021.
 * Etrog Systems LTD.
 */
@Singleton
class WeatherRepository @Inject constructor(
    private val locationDbHelper: LocationDbHelper,
    private val api: Api
) {

    companion object {
        const val API_KEY = "07586f3ce8d260dd03228e2183663570"
    }

    suspend fun fetchWeather(): Weather? {
        val location = locationDbHelper.getLocation()
        val lat = location.latitude
        val lon = location.longitude
        val url =
            "https://api.openweathermap.org/data/2.5/weather?lat=${lat}&lon=${lon}&appid=${API_KEY}&units=imperial"
        val response = api.getWeather(url)

        if (response.isSuccessful) {
            val weatherResponse = response.body()
            return if (weatherResponse != null) {
                Weather(
                    weatherResponse.name,
                    weatherResponse.sys.country,
                    weatherResponse.weather[0].main,
                    weatherResponse.weather[0].description,
                    weatherResponse.main.temp,
                    weatherResponse.weather[0].icon
                )
            } else {
                null
            }
        } else {
            return null
        }
    }
}

class Weather(
    val city: String,
    val country: String,
    val condition: String,
    val description: String,
    val temperature: Float,
    val icon: String
) : Serializable

class WeatherResponse() {

    lateinit var name: String
    lateinit var weather: ArrayList<InnerWeather>
    lateinit var main: Main
    lateinit var sys: Sys

    data class Coordination(val lon: Float, val lat: Float)

    data class Sys(
        val type: Int,
        val id: Int,
        val country: String,
        val sunrise: Long,
        val sunset: Long
    )

    data class Main(
        val temp: Float,
        @SerializedName(value = "feels_like") val feelsLike: Float,
        @SerializedName(value = "temp_min") val tempMin: Float,
        @SerializedName(value = "temp_max") val tempMax: Float,
        val pressure: Float,
        val humidity: Float
    )

    data class InnerWeather(
        val id: Int,
        val main: String,
        val description: String,
        val icon: String
    )
}