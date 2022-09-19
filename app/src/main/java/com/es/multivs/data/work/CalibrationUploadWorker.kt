package com.es.multivs.data.work

import android.content.Context
import android.os.Handler
import android.os.Looper
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.es.multivs.data.database.entities.CalibrationResults
import com.es.multivs.data.repository.MultiVsRepository
import com.google.gson.Gson
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import javax.inject.Inject

//TODO: Upload calibrations
class CalibrationUploadWorker @AssistedInject constructor(
    @Assisted appContext: Context,
    @Assisted workerParams: WorkerParameters,
) : CoroutineWorker(appContext, workerParams) {

    private val handler = Handler(Looper.getMainLooper())
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    @Inject
    lateinit var repo: MultiVsRepository

    override suspend fun doWork(): Result {
        val jsonData = inputData.getString("data")
        val data = Gson().fromJson(jsonData, CalibrationResults::class.java)


//        val answer = repo.postCalibration(data)

        return Result.success()
    }

    companion object {
        const val NOTIFICATION_ID = 1001
        const val TAG = "multivs_worker"
    }
}