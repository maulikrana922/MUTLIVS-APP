package com.es.multivs.data.work

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.es.multivs.data.repository.MedicationRepository
import com.es.multivs.data.repository.MedicationUploadData
import com.es.multivs.data.repository.MedicationUploadModel
import com.es.multivs.data.utils.ScheduleUpdatesManager
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import java.lang.reflect.Type

/**
 * Created by Marko on 1/31/2022.
 * Etrog Systems LTD.
 */
@HiltWorker
class MedicationWorker @AssistedInject constructor(
    @Assisted context: Context,
    @Assisted workerParams: WorkerParameters,
    private val updates: ScheduleUpdatesManager,
    private val repository: MedicationRepository,
) : CoroutineWorker(context, workerParams) {

    companion object {
        const val MEDS = "med_worker_meds"
        const val USERNAME = "med_worker_username"
        const val BATTERY = "med_worker_battery"
        const val TIME = "med_worker_actual_time"
        const val SCHEDULE_TIME = "med_worker_schedule_time"
        const val LAT = "med_worker_lat"
        const val LNG = "med_worker_lng"
        const val SCHEDULE_TIME_SERVER = "med_worker_schedule_time_server"

        fun serializeMedsToJson(list: ArrayList<MedicationUploadData>): String {
            val listType = object : TypeToken<ArrayList<MedicationUploadData>>() {}.type
            return Gson().toJson(list, listType)
        }

        fun deserializeMedsFromJson(jsonString: String): List<MedicationUploadData> {
            val type: Type = object : TypeToken<ArrayList<MedicationUploadData>>() {}.type
            return Gson().fromJson(jsonString, type)
        }
    }

    override suspend fun doWork(): Result {
        val medsToUpload = someFunc()
        val isUploaded = repository.uploadTakenMedications(medsToUpload)

        return if (isUploaded) {
            val timestamp = inputData.getString(SCHEDULE_TIME_SERVER)
            updates.medicationsComplete(timestamp ?: "")
            Result.success()
        } else {
            Result.failure()
        }
    }

    private fun someFunc(): MedicationUploadModel {

        val meds = deserializeMedsFromJson(inputData.getString(MEDS)!!)
        val username = inputData.getString(USERNAME)
        val battery = inputData.getString(BATTERY)
        val time = inputData.getString(TIME)
        val scheduleTime = inputData.getString(SCHEDULE_TIME)
        val lat = inputData.getString(LAT)
        val lng = inputData.getString(LNG)

        return MedicationUploadModel(
            username!!,
            meds as ArrayList<MedicationUploadData>,
            battery!!,
            time!!,
            scheduleTime!!,
            lat!!,
            lng!!
        )
    }
}