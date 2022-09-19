package com.es.multivs.data.repository

import com.es.multivs.data.database.gateway.GatewayDbHelper
import com.es.multivs.data.network.TokenKeeper
import com.es.multivs.data.network.netmodels.UserDevices
import com.es.multivs.data.network.retrofit.Api
import retrofit2.Response
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Created by Marko on 10/21/2021.
 * Etrog Systems LTD.
 */
@Singleton
class UserDeviceRepository @Inject constructor(
    private val api: Api,
    private val databaseHelper: GatewayDbHelper
    ) {

    interface UserDeviceRepoCallback {
        fun onResponse(userDevices: UserDevices?)
        fun onFailure()
    }

    private var _callback: UserDeviceRepoCallback? = null

    fun setOnResponseListener(callback: UserDeviceRepoCallback) {
        _callback = callback
    }

    suspend fun fetchUserDevices(): Response<UserDevices>{

        val identifier = databaseHelper.getIdentifier()

        val authHeader = "Bearer ${TokenKeeper.instance?.token}"

        val url = databaseHelper.getBaseURL().plus("get-device-list")

        val response: Response<UserDevices> = api.getUserDevices(identifier, url, authHeader)

        return response
    }
}