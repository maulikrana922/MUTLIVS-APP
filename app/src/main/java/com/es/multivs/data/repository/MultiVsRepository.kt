package com.es.multivs.data.repository

import android.util.Log
import com.es.multivs.data.database.entities.CalibrationResults
import com.es.multivs.data.database.gateway.GatewayDbHelper
import com.es.multivs.data.network.BaseApiResponse
import com.es.multivs.data.network.Resource
import com.es.multivs.data.network.TokenKeeper
import com.es.multivs.data.network.netmodels.CalibrationPostAnswer
import com.es.multivs.data.network.netmodels.CalibrationResponse
import com.es.multivs.data.network.netmodels.SerializedPatchResults
import com.es.multivs.data.network.netmodels.UserLatestRead
import com.es.multivs.data.network.retrofit.Api
import com.es.multivs.data.network.retrofit.CoreApiImp
import com.es.multivs.data.utils.Status
import dagger.hilt.android.scopes.ActivityRetainedScoped
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import java.lang.Exception
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.coroutines.CoroutineContext
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine

/**
 * Created by Marko on 10/21/2021.
 * Etrog Systems LTD.
 */

@ActivityRetainedScoped
class MultiVsRepository @Inject constructor(
    private val api: Api,
    private val coreApiImpl: CoreApiImp,
    private val gatewayDbHelper: GatewayDbHelper
) : CoroutineScope, BaseApiResponse() {

    private var _baseURL: String = ""

    companion object {
        private const val SAVE_FILE_RECORD_PATH = "saveFileRecord"
        private const val USER_LATEST_READ_PATH = "userLatestRead/"
    }

    suspend fun postCalibrationResult(results: CalibrationResults): Flow<Resource<CalibrationResponse>> {
        return flow { emit(safeApiCall { coreApiImpl.uploadCalibrationData(results) }) }.flowOn(
            Dispatchers.IO
        )
    }

    suspend fun postCalibration(results: CalibrationResults): CalibrationPostAnswer? {
        val authHeader = "Bearer " + TokenKeeper.instance?.token
        _baseURL = gatewayDbHelper.getBaseURL()
        val url = "${_baseURL}saveCalibrationRecord"
        val response = api.uploadCalibration(url, results, authHeader)
        if (response.isSuccessful) {
            return response.body()
        } else {
            val response2 = api.uploadCalibration(url, results, authHeader) // second try
            if (response2.isSuccessful) {
                val answer = response2.body()
                answer?.let {
                    return if (it.status) {
                        answer
                    } else {
                        null
                    }
                }
            }
            return null
        }
    }

    suspend fun postPatchResults(results: SerializedPatchResults, baseUrl: String): Unit {
        _baseURL = baseUrl
        return suspendCoroutine { continuation ->
            launch {
                val authHeader = "Bearer " + TokenKeeper.instance?.token
                val url: String = _baseURL + SAVE_FILE_RECORD_PATH
                try {
                    withTimeout(60000) {
                        val response = api.saveFileRecord(url, results, authHeader)
                        if (response.isSuccessful && response.body() != null) {
                            continuation.resume(Unit)
                        } else {
                            throw(ResultFailedException("server response was not successful"))
                        }
                    }
                } catch (e: Exception) {
                    Log.e("exception",e.message.toString())
                    //throw(ResultPostException("Could not post MULTIVS results", e))
                }
            }
        }
    }

    suspend fun getUserLatestRead(userID: Int): ReadResult {
        return withContext(Dispatchers.IO) {
            val authHeader = "Bearer " + TokenKeeper.instance?.token
            val url: String = _baseURL + USER_LATEST_READ_PATH + userID.toString()
            val response = api.getUserLatestRead(url, authHeader)
            if (response.isSuccessful) {
                ReadResult.success(response.body())
            } else {
                ReadResult.error("Could not fetch results", null)
            }
        }
    }

    private val job = Job()
    override val coroutineContext: CoroutineContext
        get() = job + Dispatchers.IO
}

class ReadResult(val status: Status, val data: UserLatestRead?, val message: String?) {
    companion object {
        fun success(data: UserLatestRead?): ReadResult {
            return ReadResult(Status.SUCCESS, data, null)
        }

        fun error(msg: String, data: UserLatestRead?): ReadResult {
            return ReadResult(Status.SUCCESS, data, null)
        }
    }
}
