package com.es.multivs.presentation.view.viewmodels


import dagger.hilt.android.lifecycle.HiltViewModel
import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import androidx.work.*
import com.es.multivs.data.database.gateway.GatewayDbHelper
import com.es.multivs.data.database.location.LocationDbHelper
import com.es.multivs.data.database.medications.MedicationDbHelper
import com.es.multivs.data.models.MedicationScheduleItem
import com.es.multivs.data.models.UploadStatus
import com.es.multivs.data.repository.MedicationRepository
import com.es.multivs.data.repository.MedicationUploadData
import com.es.multivs.data.repository.MedicationUploadModel
import com.es.multivs.data.utils.AppUtils
import com.es.multivs.data.utils.ScheduleUpdatesManager
import com.es.multivs.data.utils.ScheduleUtils
import com.es.multivs.data.work.MedicationWorker
import com.es.multivs.data.work.MedicationWorker.Companion.serializeMedsToJson
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * Created by Marko on 10/13/2021.
 * Etrog Systems LTD.
 */
@HiltViewModel
class MedicationsViewModel @Inject constructor(
    application: Application,
    private val medsDatabaseHelper: MedicationDbHelper,
    private val gatewayDatabaseHelper: GatewayDbHelper,
    private val repository: MedicationRepository,
    private val locationDbHelper: LocationDbHelper,
    private val updates: ScheduleUpdatesManager
) : AndroidViewModel(application) {

    private val _medicationUpload = MutableLiveData<UploadStatus>()
    val medicationUpload: LiveData<UploadStatus> = _medicationUpload

    suspend fun getMedicationsFromDatabase(): MedicationScheduleItem? {
        val list = medsDatabaseHelper.getLastMedicationItems()
        var timestamp: String
        list?.let { medication ->
            val items = ScheduleUtils.aggregateMedsByTimeStamps(medication)
            timestamp = ScheduleUtils.findActiveMedication(items)
            items.forEach {
                if (it.timeStamp == timestamp) {
                    return it
                }
            }
        }
        return null
    }

    fun uploadTakenMedications(
        medications: ArrayList<MedicationUploadData>,
        medicationItem: MedicationScheduleItem?
    ) {
        viewModelScope.launch {

            val location = locationDbHelper.getLocation()

            val medicationUploadModel = MedicationUploadModel(
                gatewayDatabaseHelper.getUsername(),
                medications,
                AppUtils.getBatteryPercentage(getApplication()).toString(),
                (System.currentTimeMillis() / 1000).toString(),
                ScheduleUtils.parseScheduleTimeToEpochMillis(medicationItem),
                location.latitude.toString(),
                location.longitude.toString()
            )

            if (AppUtils.isInternetAvailable(getApplication())) {


                val isUploaded = repository.uploadTakenMedications(medicationUploadModel)
                if (isUploaded) {
                    _medicationUpload.postValue(UploadStatus(true))

                    updates.medicationsComplete(medicationItem?.timeStamp ?: "")

                } else {
                    _medicationUpload.postValue(
                        UploadStatus(
                            false,
                            "There was an error uploading the medications"
                        )
                    )
                }
            } else {
                _medicationUpload.postValue(
                    UploadStatus(
                        false,
                        "No internet connection. Medications will be uploaded at a later time"
                    )
                )

                val constraints =
                    Constraints.Builder().setRequiredNetworkType(NetworkType.CONNECTED).build()

                val data = workDataOf(
                    MedicationWorker.USERNAME to medicationUploadModel.username,
                    MedicationWorker.MEDS to serializeMedsToJson(medicationUploadModel.medication),
                    MedicationWorker.BATTERY to AppUtils.getBatteryPercentage(getApplication())
                        .toString(),
                    MedicationWorker.TIME to medicationUploadModel.actual_time,
                    MedicationWorker.SCHEDULE_TIME to medicationUploadModel.scheduled_time,
                    MedicationWorker.LAT to medicationUploadModel.lat,
                    MedicationWorker.LNG to medicationUploadModel.lng,
                    MedicationWorker.SCHEDULE_TIME_SERVER to medicationItem?.timeStamp
                )

                val request = OneTimeWorkRequestBuilder<MedicationWorker>()
                    .setInputData(data)
                    .setConstraints(constraints).build()

                WorkManager.getInstance(getApplication()).enqueue(request)
            }
        }
    }
}