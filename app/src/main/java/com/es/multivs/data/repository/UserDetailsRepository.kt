package com.es.multivs.data.repository


import android.util.Log
import android.widget.Toast
import com.es.multivs.data.database.entities.CalibrationResults
import com.es.multivs.data.database.entities.UserLocation
import com.es.multivs.data.database.gateway.GatewayDbHelper
import com.es.multivs.data.database.location.LocationDbHelper
import com.es.multivs.data.models.LoginDto
import com.es.multivs.data.network.BaseApiResponse
import com.es.multivs.data.network.Resource
import com.es.multivs.data.network.TokenKeeper
import com.es.multivs.data.network.netmodels.*
import com.es.multivs.data.network.retrofit.Api
import com.es.multivs.data.network.retrofit.Apis
import com.es.multivs.data.network.retrofit.CoreApiImp
import com.es.multivs.data.utils.AppUtils
import com.es.multivs.data.utils.Constants
import dagger.hilt.android.scopes.ActivityRetainedScoped
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import retrofit2.Response
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.coroutines.CoroutineContext

/**
 * Created by Marko on 10/21/2021.
 * Etrog Systems LTD.
 */
@ActivityRetainedScoped
class UserDetailsRepository @Inject constructor(
    private val api: Api,
    private val coreApiImp: CoreApiImp,
    private val gatewayHelper: GatewayDbHelper,
    private val locationHelper: LocationDbHelper
):CoroutineScope, BaseApiResponse() {

    suspend fun login(loginDto: LoginDto): Flow<Resource<Any>> {
        return flow { emit(safeApiCall { coreApiImp.login(loginDto) }) }.flowOn(
            Dispatchers.IO
        )
    }

    private val job = Job()
    override val coroutineContext: CoroutineContext
        get() = job + Dispatchers.IO

    suspend fun fetchUserDetails(): UserDetails.UserInfo? {

        val baseURL = gatewayHelper.getBaseURL()
        val username = gatewayHelper.getUsername()
        val url = "${baseURL}getUserInfo/$username"
        val authHeader = "Bearer " + TokenKeeper.instance?.token

        val response: Response<UserDetails> = api.getUserDetails(url, authHeader)

        return if (response.isSuccessful) {

            response.body()?.userInfo
        } else {
            Toast.makeText(AppUtils.application, response.message(), Toast.LENGTH_SHORT).show()
            UserDetails.UserInfo()
        }
    }

    suspend fun postPeriodicToServer(
        baseURL: String,
        username: String?,
        batteryLevel: Int,
        timeStamp: Long
    ) {
        val userLocation: UserLocation = locationHelper.getLocation()
        val serializedUserLocation = SerializedUserLocation()

        serializedUserLocation.setUserName(username)
        serializedUserLocation.setUuid(username)
        serializedUserLocation.setBatteryLevel(batteryLevel.toString())
        serializedUserLocation.setTimeStamp(timeStamp.toString())
        serializedUserLocation.setLatitude(userLocation.latitude.toString())
        serializedUserLocation.setLongitude(userLocation.longitude.toString())

        val url = baseURL + "set-sensor-value"
        val authHeader = "Bearer " + TokenKeeper.instance?.token

        val response: Response<PeriodicInfoAnswer> =
            api.uploadUserLocation(url, serializedUserLocation, authHeader)
        if (response.isSuccessful) {
            if (response.body()?.status == true) {
                Log.d("PERIODIC_INFO", "onResponse: " + response.body());
            }
        } else {
            Toast.makeText(AppUtils.application, response.message(), Toast.LENGTH_SHORT).show()
            Log.d("PERIODIC_INFO", "onResponse: " + response.message())
        }
    }


    suspend fun fetchPeriodicPostFrequency(): Int {
        val username: String = gatewayHelper.getUsername()
        val baseURL: String = gatewayHelper.getBaseURL()
        val url = baseURL + Constants.GET_USER_SCHEDULE + username
        val response: Response<List<UserSchedule>> =
            api.getUserSchedule(url, "Bearer ${TokenKeeper.instance?.token}")
        if (response.isSuccessful) {
            response.body()?.let { list ->
                if (list.isNotEmpty()) {
                    list.forEach {
                        if (it.frequency != 0) {
                            return it.frequency
                        }
                    }
                }
            }
        }else{
            Toast.makeText(AppUtils.application, response.message(), Toast.LENGTH_SHORT).show()
        }
        return 7200
    }
}