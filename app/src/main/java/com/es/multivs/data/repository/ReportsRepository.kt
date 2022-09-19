package com.es.multivs.data.repository

import android.util.Log
import com.es.multivs.data.database.gateway.GatewayDbHelper
import com.es.multivs.data.models.ReportsModel
import com.es.multivs.data.network.TokenKeeper
import com.es.multivs.data.network.netmodels.ReportsMeasurements
import com.es.multivs.data.network.netmodels.ReportsMedication
import com.es.multivs.data.network.retrofit.Api
import kotlinx.coroutines.TimeoutCancellationException
import retrofit2.Response
import javax.inject.Inject
import javax.inject.Singleton


@Singleton
class ReportsRepository @Inject constructor(
    private val gatewayDbHelper: GatewayDbHelper,
    private val api: Api
) {

    suspend fun fetchMeasurements(hours: String): ReportsModel {
        val userId: Int = gatewayDbHelper.getUserID()
        val authHeader = "Bearer " + TokenKeeper.instance?.token
        val baseUrl = gatewayDbHelper.getBaseURL()
        val url = "${baseUrl}getUserChart/$userId/$hours"
        var response: Response<ReportsMeasurements>

        try {
            response = api.getReportsMeasurements(url, authHeader)
        } catch (e: TimeoutCancellationException) {
            Log.e("dinesh", "callServer: Error getting measurements. No measurements found")
            return ReportsModel(error = "Your internet connection is very slow")
        } catch (e: Exception) {
            Log.e("dinesh", "${e.message}")
            return ReportsModel(error = "No measurements found")
        }

        if (response.isSuccessful) {
            if (response.body() != null) {
                return onSuccessfulResponse(response)
            }
        }
        return ReportsModel(error = "Cannot fetch measurements")
    }

    private fun onSuccessfulResponse(response: Response<ReportsMeasurements>): ReportsModel {
        val reportsModel = ReportsModel()
        val medList = response.body()
        medList?.let { reportsModel.measurementsList.addAll(it.message) }
        Log.e("response", medList.toString())
        return reportsModel
    }


    suspend fun fetchMedications(hours: String): ReportsModel {
        val userId: Int = gatewayDbHelper.getUserID()
        val authHeader = "Bearer " + TokenKeeper.instance?.token
        val baseUrl = gatewayDbHelper.getBaseURL()
        val url = "${baseUrl}getMedicationChart/$userId/$hours"
        var response: Response<ReportsMedication>

        try {
            response = api.getReportsMedication(url, authHeader)
        } catch (e: TimeoutCancellationException) {
            return ReportsModel(error = "Your internet connection is very slow")
        } catch (e: Exception) {
            Log.e("dinesh", "${e.message}")
            return ReportsModel(error = "No medications found")
        }

        if (response.isSuccessful) {
            if (response.body() != null) {
                val reportsModel = ReportsModel()
                response.body()
                    ?.let { reportsModel.medicationsList.addAll(it.message.medication) }
                return reportsModel
            }
        }
        return ReportsModel(error = "Cannot fetch medications")
    }

}