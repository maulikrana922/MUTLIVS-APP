package com.es.multivs.data.repository

import android.util.Log
import com.es.multivs.data.database.gateway.GatewayDbHelper
import com.es.multivs.data.models.VideoModel
import com.es.multivs.data.network.TokenKeeper
import com.es.multivs.data.network.retrofit.Api
import retrofit2.Response
import javax.inject.Inject

class VideoRepository @Inject constructor(
    private val api: Api,
    private val databaseHelper: GatewayDbHelper
) {

    suspend fun getUserURL(): Response<VideoModel> {
        val authHeader = "Bearer ${TokenKeeper.instance?.token}"
        val url = databaseHelper.getBaseURL().plus("getUserURLs/").plus(databaseHelper.getUsername())
        Log.e("Url",url)
        return api.getUserURL(url, authHeader)
    }
}