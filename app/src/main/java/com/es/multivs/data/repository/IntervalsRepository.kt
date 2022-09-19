package com.es.multivs.data.repository

import com.es.multivs.data.database.entities.NonMultiVSResults
import com.es.multivs.data.database.gateway.GatewayDbHelper
import com.es.multivs.data.database.location.LocationDbHelper
import com.es.multivs.data.network.TokenKeeper
import com.es.multivs.data.network.netmodels.SerializedResults
import com.es.multivs.data.network.retrofit.Api
import com.es.multivs.data.utils.Constants
import com.es.multivs.presentation.view.fragments.MeasurementUploadListener
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Created by Dinesh on 23/03/2022.
 * Etrog Systems LTD.
 */

@Singleton
class IntervalsRepository @Inject constructor(
    private val api: Api,
    private val gatewayDbHelper: GatewayDbHelper,
    private val locationDbHelper: LocationDbHelper
) {

    suspend fun uploadNonMultiVSResults(
        results: NonMultiVSResults,
        deviceBattery: Int,
        callback: MeasurementUploadListener
    ): Boolean {

        var success: Boolean

        val baseURL = gatewayDbHelper.getBaseURL()
        val username = gatewayDbHelper.getUsername()
        val identifier = gatewayDbHelper.getIdentifier()
        val url = "${baseURL}set-sensor-value"
        val authHeader = "Bearer ${TokenKeeper.instance?.token}"

        val serializedResults = SerializedResults()
        serializedResults.setUserName(username)
        serializedResults.setMacAddress(identifier)
        serializedResults.setUuid(username)
        serializedResults.setBatteryLevel(deviceBattery.toString())
        serializedResults.setIsManualEntry(Constants.isManualEntry)
        Constants.isManualEntry=1
        prepareResultsData(serializedResults, results)
        try {
            success = withContext(Dispatchers.IO) {
                val response = api.setSensorValue(url, serializedResults, authHeader)

                if (response.isSuccessful) {
                    callback.onResultsUploaded(true)

                    true
                } else {
                    callback.onResultsUploaded(false)
                    false
                }
            }
        } catch (e: Exception) {
            success = false
            callback.onResultsUploaded(false)
        }
        return success
    }

    private suspend fun prepareResultsData(
        resultsToSend: SerializedResults,
        resultsToRead: NonMultiVSResults
    ) {
        resultsToSend.setTimeStamp((System.currentTimeMillis() / 1000).toString())

        /**
         * spO2 data
         */
        if (resultsToRead.oximeterSpo2 != 0) {
            resultsToSend.setSpo2(resultsToRead.oximeterSpo2.toString())
        }

        /**
         * heart rate data
         */
        if (resultsToRead.heartRate != 0) {
            resultsToSend.setHeartValue(resultsToRead.heartRate.toString())
        }

        /**
         * location data
         */
        val location = locationDbHelper.getLocation()
        resultsToSend.setLatitude(location.latitude.toString())
        resultsToSend.setLongitude(location.longitude.toString())
    }
}