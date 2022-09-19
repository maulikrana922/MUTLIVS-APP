package com.es.multivs.data.work

import android.content.Context
import android.util.Log
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import androidx.work.workDataOf
import com.es.multivs.data.database.entities.NonMultiVSResults
import com.es.multivs.data.database.measurements.MeasurementDbHelper
import com.es.multivs.data.repository.MeasurementResultRepository
import com.es.multivs.data.utils.ScheduleUpdatesManager
import com.google.gson.Gson
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject

/**
 * Created by Marko on 1/13/2022.
 * Etrog Systems LTD.
 */
@HiltWorker
class MeasurementsWorker @AssistedInject constructor(
    @Assisted context: Context,
    @Assisted workerParams: WorkerParameters,
    private val db: MeasurementDbHelper,
    private val updates: ScheduleUpdatesManager,
    private val repo: MeasurementResultRepository,
) : CoroutineWorker(context, workerParams) {

    companion object {

        fun serializeResultToJson(result: NonMultiVSResults): String {
            val gson = Gson()
            return gson.toJson(result)
        }

        fun deserializeResultFromJson(jsonString: String): NonMultiVSResults {
            val gson = Gson()
            return gson.fromJson(jsonString, NonMultiVSResults::class.java)
        }

        const val RESULT = "MeasurementsWorker_result"
        const val BATTERY = "MeasurementsWorker_battery"
        const val TAG = "MeasurementsWorker_battery"
    }

    override suspend fun doWork(): Result {

        Log.d(TAG, "doWork: starting")

        val battery = inputData.getInt(BATTERY, 0)
        inputData.getString(RESULT)?.let { json ->

            val result: NonMultiVSResults? = try {
                deserializeResultFromJson(json)
            } catch (t: Throwable) {
                Log.e(TAG, """
                    Failed to deserialize results from json
                    JSON: $json
                """.trimIndent() )
                null
            }

            result?.let {

                /**
                 * the battery value will obviously never be 0, but if for some reason
                 * The inputData didn't go through or failed to parse the content,
                 * the upload operation should be stopped.
                 */
                val isSuccess: Boolean = if (battery != 0) {
                    repo.uploadNonMultiVSResults(it, battery) { }
                } else {
                    false
                   // throw IllegalStateException("MeasurementsWorker did not have 'key' in the worker's inputData object ")
                }

                if (isSuccess) {
                    val activeMeasurement = db.getActiveMeasurement()
                    db.clearNonMultiVSResults()
                    updates.measurementsComplete(activeMeasurement)
                    Log.d(TAG, "doWork: $activeMeasurement"
                    )
                    Log.d(TAG, "doWork: success")
                    return Result.success(workDataOf(RESULT to true))
                }
            }
        }
        Log.d(TAG, "doWork: failure")
        return Result.failure(workDataOf(RESULT to false))
    }

}