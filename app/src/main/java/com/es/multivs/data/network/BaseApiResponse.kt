package com.es.multivs.data.network

import android.content.Context
import com.es.multivs.data.utils.Constants
import retrofit2.Response

abstract class BaseApiResponse() {

    suspend fun <T> safeApiCall(apiCall: suspend () -> Response<GenericResponse<T>>): Resource<T> {
        // if (Utils.hasInternetConnection(context)) {

        try {
            val response = apiCall()
            if (response.isSuccessful) {
                val body = response.body()
                body?.let { genericResponse ->
                    return if (genericResponse.status)
                    //LiveDataEventBus.publish(LiveDataEventBus.NETWORK_ERROR, false)
                        Resource.success(
                            genericResponse.data,
                            genericResponse.message.toString()
                        ) as Resource<T>
                    else {
                        Resource.error(genericResponse.message ?: "Something went wrong")
                    }
                } ?: kotlin.run {
                    return Resource.error(response.message().toString())
                }
            }
            if (response.code() != 200) {
            }
            if (response.code() == 403) {
                //WebengageHelper.logEvent(WebengageEvents.LEAP_TOKEN_EXPIRED)
                // Logger.d("Authenticator", "EXPIRED TOKEN")
            }
            if (response.code() == 502) {
                // Logger.d("Authenticator", "Network Exception")
            }
            return Resource.error(response.message().toString())
        } catch (e: Exception) {
            return kotlin.error(e.message ?: e.toString())
        }
        // }
        return error("No internet connection !")
    }

    private fun <T> error(errorMessage: String): Resource<T> =
        Resource.error("Network call failed : $errorMessage")


}