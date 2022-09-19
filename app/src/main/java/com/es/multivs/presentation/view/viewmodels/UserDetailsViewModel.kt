package com.es.multivs.presentation.view.viewmodels

import android.app.Application
import android.location.Location
import android.os.Build
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.es.multivs.BuildConfig
import com.es.multivs.R
import com.es.multivs.data.database.entities.GatewayData
import com.es.multivs.data.database.entities.UserLocation
import com.es.multivs.data.database.gateway.GatewayDbHelper
import com.es.multivs.data.database.location.LocationDbHelper
import com.es.multivs.data.database.sets.UserCredentials
import com.es.multivs.data.models.LoginDto
import com.es.multivs.data.network.Resource
import com.es.multivs.data.repository.UserDetailsRepository
import com.es.multivs.data.utils.AppUtils
import com.google.android.gms.ads.identifier.AdvertisingIdClient
import com.google.gson.Gson
import com.google.gson.JsonObject
import com.google.gson.JsonParser
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import okhttp3.*
import javax.inject.Inject
import kotlin.coroutines.resume


/*
* created by Marko
Etrog Systems LTD. 30/8/2021.
*/

@HiltViewModel
class UserDetailsViewModel @Inject constructor(
    application: Application,
    private val detailsRepo: UserDetailsRepository,
    private val gatewayDb: GatewayDbHelper,
    private val locationDb: LocationDbHelper,
) : AndroidViewModel(application) {

    private var isUserLoggedIn = false

    suspend fun getUserCountry(): String? {
        return gatewayDb.getUserCountrySuspended()
    }

    suspend fun getUserCredentials(): UserCredentials? {
        return gatewayDb.getUserCredentialsSuspended()
    }

    var manualInput: LiveData<Int> = gatewayDb.getIsManualInputLiveData()

    private val _isLoggingIn: MutableLiveData<Boolean> = MutableLiveData()
    fun isLoggingIn(): LiveData<Boolean> {
        return _isLoggingIn
    }

/*    suspend fun attemptLogin(
        url: String,
        username: String,
        password: String,
        identifier: String
    ) = suspendCancellableCoroutine<LoginStatus> { continuation ->
        _isLoggingIn.postValue(true)
        val mediaType = MediaType.parse("application/json;charset=utf-8")
        val versionName = BuildConfig.VERSION_NAME
        val json = JSONObject().apply {
            put("username", username)
            put("macAddress", identifier.lowercase())
            put("uuid", username)
            put("password", password)
            put("appVersion",versionName)
        }
        val requestBody: RequestBody = RequestBody.create(mediaType, json.toString())
        val request = Request.Builder().url(url).post(requestBody).build()
        val client = OkHttpClient.Builder()
            .readTimeout(3000, TimeUnit.SECONDS)
            .writeTimeout(3000, TimeUnit.SECONDS)
            .build()
        client.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                _isLoggingIn.postValue(false)
                if (!continuation.isCompleted) {
                    continuation.resume(LoginStatus.Failed("Login error"))
                }
            }

            override fun onResponse(call: Call, response: Response) {
                if (response.isSuccessful) {
                    val responseString = response.body()!!.string()
                    if (responseString.contains("\"status\":false")) {
                        response.close()
                        if (responseString.contains("227")) {
                            if (!continuation.isCompleted) {
                                continuation.resume(LoginStatus.Failed("Device not registered"))
                                _isLoggingIn.postValue(false)
                            }
                        } else {
                            if (!continuation.isCompleted) {
                                continuation.resume(LoginStatus.Failed("Wrong credentials"))
                                _isLoggingIn.postValue(false)
                            }
                        }
                        response.close()
                    } else {
                        if (responseString.contains("data")) {
                            val token = JsonParser.parseString(responseString)
                                .asJsonObject["data"]
                                .asJsonObject["token"]
                                .asString
                            if (!continuation.isCompleted) {
                                isUserLoggedIn = true
                                continuation.resume(LoginStatus.SuccessWithToken(token))
                            }
                        } else {
                            if (!continuation.isCompleted) {
                                isUserLoggedIn = true
                                continuation.resume(LoginStatus.Success)
                            }
                        }
                        response.close()
                    }

                } else {
                    response.close()
                    if (!continuation.isCompleted) {
                        continuation.resume(LoginStatus.Failed("Login Error"))
                    }
                    _isLoggingIn.postValue(false)
                }
            }
        })
    }*/

    suspend fun attemptLogin(
        url: String,
        username: String,
        password: String,
        identifier: String
    ) = suspendCancellableCoroutine<LoginStatus> { continuation ->
        _isLoggingIn.postValue(true)
        val versionName = BuildConfig.VERSION_NAME

        ioJob {
            detailsRepo.login(
                LoginDto(
                    username,
                    identifier.lowercase(),
                    username,
                    password,
                    versionName
                )
            ).collect {
                Log.e("login", Gson().toJson(it))

                when (it.status) {
                    Resource.Status.SUCCESS -> {
                        val responseString = it.toString()
                        if (responseString.contains("\"status\":false")) {
                            if (responseString.contains("227")) {
                                if (!continuation.isCompleted) {
                                    continuation.resume(LoginStatus.Failed("Device not registered"))
                                    _isLoggingIn.postValue(false)
                                }
                            } else {
                                if (!continuation.isCompleted) {
                                    continuation.resume(LoginStatus.Failed("Wrong credentials"))
                                    _isLoggingIn.postValue(false)
                                }
                            }
                        } else {
                            if (responseString.contains("data")) {
                                val token = JsonParser.parseString(it.data.toString())
                                    .asJsonObject["token"]
                                    .asString
                                if (!continuation.isCompleted) {
                                    isUserLoggedIn = true
                                    continuation.resume(LoginStatus.SuccessWithToken(token))
                                }
                            } else {
                                if (!continuation.isCompleted) {
                                    isUserLoggedIn = true
                                    continuation.resume(LoginStatus.Success)
                                }
                            }
                        }
                    }
                    Resource.Status.ERROR -> {
                        _isLoggingIn.postValue(false)
                        if (it.message?.contains("227") == true) {
                            if (!continuation.isCompleted) {
                                continuation.resume(LoginStatus.Failed("Device not registered"))
                            }
                        } else {
                            if (!continuation.isCompleted) {
                                continuation.resume(LoginStatus.Failed("Wrong credentials"))
                            }
                        }
                    }
                    else-> {
                        if (!continuation.isCompleted) {
                            continuation.resume(LoginStatus.Failed(it.message.toString()))
                        }
                        _isLoggingIn.postValue(false)
                        //continuation.resume(LoginStatus.Failed(it.message.toString()))
                    }
                }
            }
        }

        /*val mediaType = MediaType.parse("application/json;charset=utf-8")

        val json = JSONObject().apply {
            put("username", username)
            put("macAddress", identifier.lowercase())
            put("uuid", username)
            put("password", password)
            put("appVersion",versionName)
        }
        val requestBody: RequestBody = RequestBody.create(mediaType, json.toString())
        val request = Request.Builder().url(url).post(requestBody).build()
        val client = OkHttpClient.Builder()
            .readTimeout(3000, TimeUnit.SECONDS)
            .writeTimeout(3000, TimeUnit.SECONDS)
            .build()
        client.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                _isLoggingIn.postValue(false)
                if (!continuation.isCompleted) {
                    continuation.resume(LoginStatus.Failed("Login error"))
                }
            }

            override fun onResponse(call: Call, response: Response) {
                if (response.isSuccessful) {
                    val responseString = response.body()!!.string()
                    if (responseString.contains("\"status\":false")) {
                        response.close()
                        if (responseString.contains("227")) {
                            if (!continuation.isCompleted) {
                                continuation.resume(LoginStatus.Failed("Device not registered"))
                                _isLoggingIn.postValue(false)
                            }
                        } else {
                            if (!continuation.isCompleted) {
                                continuation.resume(LoginStatus.Failed("Wrong credentials"))
                                _isLoggingIn.postValue(false)
                            }
                        }
                        response.close()
                    } else {
                        if (responseString.contains("data")) {
                            val token = JsonParser.parseString(responseString)
                                .asJsonObject["data"]
                                .asJsonObject["token"]
                                .asString
                            if (!continuation.isCompleted) {
                                isUserLoggedIn = true
                                continuation.resume(LoginStatus.SuccessWithToken(token))
                            }
                        } else {
                            if (!continuation.isCompleted) {
                                isUserLoggedIn = true
                                continuation.resume(LoginStatus.Success)
                            }
                        }
                        response.close()
                    }

                } else {
                    response.close()
                    if (!continuation.isCompleted) {
                        continuation.resume(LoginStatus.Failed("Login Error"))
                    }
                    _isLoggingIn.postValue(false)
                }
            }
        })*/
    }

    @Suppress("BlockingMethodInNonBlockingContext")
    suspend fun resolveIdentifier(): String {
        return if (Build.VERSION.SDK_INT < Build.VERSION_CODES.R) {
            val identifier = AppUtils.getMacAddress()
            saveIdentifier(identifier)
            identifier
        } else {
            val idInfo: AdvertisingIdClient.Info? =
                AdvertisingIdClient.getAdvertisingIdInfo(getApplication())
            saveIdentifier(idInfo?.id!!)
            idInfo.id!!
        }
    }

    fun insertGatewayData() {

        viewModelScope.launch {

            val rowCount: Int = gatewayDb.getGatewayRowCount()
            if (rowCount <= 0) {

                val gatewayData = GatewayData(1)
                gatewayDb.insert(gatewayData)
            }
        }
    }

    suspend fun loadUserDetails() {

        setGatewayInfo()

        val userInfo = detailsRepo.fetchUserDetails()

        if (userInfo != null && userInfo.userID != 0) {
            userInfo.let {
                gatewayDb.updateUserIDAndIsManual(it.userID, it.isManual)
            }
            _isLoggingIn.postValue(true)
        } else {
            _isLoggingIn.postValue(false)
        }
    }

    private fun setGatewayInfo() {

        viewModelScope.launch {

            val batteryLevel = AppUtils.getBatteryPercentage(getApplication())

            val buildStr =
                getApplication<Application>().applicationContext.getString(R.string.build)
            val appBuild =
                getApplication<Application>().applicationContext.getString(R.string.appBuild)
            val build = "$buildStr $appBuild"
            val versionStr = getApplication<Application>().getString(R.string.versionString)
            val appVersion = getApplication<Application>().getString(R.string.appVersion)
            val version = "$versionStr $appVersion"

            gatewayDb.updateGatewayInfo(batteryLevel, build, version)
        }
    }

    suspend fun setUserCredentials(username: String, password: String) {
        gatewayDb.updateUserCredentials(username, password)
    }

    suspend fun insertUserLocation(_userLocation: Location?) {
        if (_userLocation != null) {
            withContext(Dispatchers.IO) {
                val userLocation = UserLocation(1, _userLocation.latitude, _userLocation.longitude)
                locationDb.insertLocation(userLocation)
            }
        }
    }

    suspend fun setBaseURLAndCountry(url: String, country: String?) {
        gatewayDb.updateBaseURLAndCountry(url, country)
    }

    suspend fun postPeriodicInfo(): Boolean {
        val result = withContext(Dispatchers.IO) {
            if (isUserLoggedIn) {

                val baseURL: String = gatewayDb.getBaseURL()
                val username: String = gatewayDb.getUsername()
                val batteryLevel: Int =
                    AppUtils.getBatteryPercentage(getApplication())
                val timeStamp = System.currentTimeMillis() / 1000
                if (AppUtils.isInternetAvailable(getApplication())) {
                    detailsRepo.postPeriodicToServer(baseURL, username, batteryLevel, timeStamp)
                    true
                } else {
                    false
                }

            } else {
                false
            }
        }
        return result
    }

    suspend fun saveIdentifier(identifier: String) {
        gatewayDb.saveIdentifier(identifier)
    }


    suspend fun getPeriodicPostFrequency(): Int {

        val frequencyFromServer = detailsRepo.fetchPeriodicPostFrequency()
        gatewayDb.setPostFrequency(frequencyFromServer)

        _isLoggingIn.postValue(false)
        return frequencyFromServer
    }

    sealed class LoginStatus {
        object Success : LoginStatus()
        data class SuccessWithToken(val token: String) : LoginStatus()
        data class Failed(val message: String) : LoginStatus()
    }
}

