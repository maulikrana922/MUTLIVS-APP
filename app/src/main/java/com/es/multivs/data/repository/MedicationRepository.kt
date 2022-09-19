package com.es.multivs.data.repository

import android.util.Log
import com.es.multivs.data.database.entities.MedicationData
import com.es.multivs.data.database.gateway.GatewayDbHelper
import com.es.multivs.data.models.MedicationModel
import com.es.multivs.data.network.TokenKeeper
import com.es.multivs.data.network.netmodels.MedicationSchedule
import com.es.multivs.data.network.retrofit.Api
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeout
import retrofit2.Response
import java.io.Serializable
import java.lang.Exception
import java.lang.StringBuilder
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Created by Marko on 10/13/2021.
 * Etrog Systems LTD.
 */
@Singleton
class MedicationRepository @Inject constructor(
    private val gatewayDbHelper: GatewayDbHelper,
    private val api: Api,
) {

    suspend fun fetchMedications(): MedicationModel {
        val username: String = gatewayDbHelper.getUsername()
        val authHeader = "Bearer " + TokenKeeper.instance?.token
        val baseUrl = gatewayDbHelper.getBaseURL()
            val url = "${baseUrl}getUserMedications/$username"
        var response: Response<List<MedicationSchedule>>


        try {
            withTimeout(7000) {
                response = api.getUserMedications(url, authHeader)
            }
        } catch (e: TimeoutCancellationException) {
            Log.e("markomarko", "callServer: Error getting medications. No medications found")
            return MedicationModel(error = "Failed to get the medication schedule")
        } catch (e: Exception) {
            Log.e("markomarko", "${e.message}" )
            Log.e("markomarko", "callServer: Error getting medications. No medications found")
            return MedicationModel(error = "No medications found")
        }

        if (response.isSuccessful) {
            if (response.body() != null) {
                return onSuccessfulResponse(response)
            }
        }

        return MedicationModel(error = "Cannot fetch medication schedule")
    }

    private fun onSuccessfulResponse(response: Response<List<MedicationSchedule>>): MedicationModel {
        val medicationModel = MedicationModel()
        val medList = response.body()
        for (item in medList.orEmpty()) {
            val medicationItem = MedicationData()
            medicationItem.asNeeded = item.asNeeded
            medicationItem.dosageForm = item.dosageForm

            if (medicationItem.dosageQuantity.isEmpty()) {
                medicationItem.dosageQuantity = "N/A"
            } else {
                medicationItem.dosageQuantity = item.dosageQuantity.toString()
            }

            medicationItem.frequency = item.frequency
            medicationItem.medicationID = item.medicationID
            medicationItem.strength = item.strength
            medicationItem.medicationName = item.medicationName

            if (item.timeForMedication1.isNotEmpty()) {
                val clone = medicationItem.deepCopy()
                clone.timeStamp = item.timeForMedication1
                medicationModel.medicationList.add(clone)
            }
            if (item.timeForMedication2.isNotEmpty()) {
                val clone = medicationItem.deepCopy()
                clone.timeStamp = item.timeForMedication2
                medicationModel.medicationList.add(clone)
            }
            if (item.timeForMedication3.isNotEmpty()) {
                val clone = medicationItem.deepCopy()
                clone.timeStamp = item.timeForMedication3
                medicationModel.medicationList.add(clone)
            }
            if (item.timeForMedication4.isNotEmpty()) {
                val clone = medicationItem.deepCopy()
                clone.timeStamp = item.timeForMedication4
                medicationModel.medicationList.add(clone)
            }
        }

        return medicationModel
    }

    suspend fun uploadTakenMedications(medication: MedicationUploadModel): Boolean {

        val authHeader = "Bearer " + TokenKeeper.instance?.token
        val baseURL = gatewayDbHelper.getBaseURL()
        val url = "${baseURL}save-medication"

        Log.d("_MEDS", medication.toString())
        val uploaded = withContext(Dispatchers.IO) {
            val response = api.uploadMedications(url, medication, authHeader)
            if (response.isSuccessful) {
                val uploadResponse = response.body()
                Log.d("_MEDS", "uploadTakenMedications: ${uploadResponse?.message}")
                Log.d("_MEDS", "uploadTakenMedications: ${uploadResponse?.status}")
                uploadResponse?.status ?: false
            } else {
                false
            }
        }

        return uploaded
    }
}

data class MedicationUploadModel(
    val username: String,
    val medication: ArrayList<MedicationUploadData>,
    val iosBatteryLevel: String,
    val actual_time: String,
    val scheduled_time: String,
    val lat:String,
    val lng:String
): Serializable {
    override fun toString(): String {
        val builder = StringBuilder()
        medication.forEach {
            builder.append("name: ${it.medicationName}").append(", quantitiy: ${it.dosageQuantity}")
                .append(", taken: ${it.is_taken}")
        }
        return """
            username: $username,
            medications: $builder,
            battery level: $iosBatteryLevel,
            actual time: $actual_time,
            scheduled time: $scheduled_time
        """.trimIndent()
    }
}

data class MedicationUploadData(
    val medicationId: String,
    val medicationName: String,
    val strength: String,
    val dosageQuantity: String,
    val dosageForm: String,
    val is_taken: String
): Serializable

data class MedicationUploadResponse(val status: Boolean, val message: String)