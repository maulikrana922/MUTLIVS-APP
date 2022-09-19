package com.es.multivs.presentation.view.viewmodels

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import androidx.work.*
import com.es.multivs.data.database.entities.NonMultiVSResults
import com.es.multivs.data.models.UploadStatus
import com.es.multivs.data.repository.IntervalsRepository
import com.es.multivs.data.utils.AppUtils
import com.es.multivs.data.work.MeasurementsWorker
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * Created by Dinesh on 23/03/2022.
 * Etrog Systems LTD.
 */

@HiltViewModel
class IntervalsViewModel @Inject constructor(
    private val resultRepository: IntervalsRepository,
    application: Application
) : AndroidViewModel(application) {

    private val _uploadStatus = MutableLiveData<UploadStatus>()
    var uploadStatus: LiveData<UploadStatus> = _uploadStatus


    fun postResults(results: NonMultiVSResults) {
        val deviceBattery = AppUtils.getBatteryPercentage(getApplication())

        if (AppUtils.isInternetAvailable(getApplication())) {
            viewModelScope.launch {
                resultRepository.uploadNonMultiVSResults(results, deviceBattery) { isSuccessful ->
                    if (isSuccessful) {
                        _uploadStatus.postValue(UploadStatus(true))
                    } else {
                        _uploadStatus.postValue(
                            UploadStatus(
                                false,
                                "There was a problem uploading the Intervals"
                            )
                        )
                    }
                }
            }
        } else {
            val constraints =
                Constraints.Builder().setRequiredNetworkType(NetworkType.CONNECTED).build()

            val serializedResults = MeasurementsWorker.serializeResultToJson(results)

            val data = workDataOf(
                MeasurementsWorker.RESULT to serializedResults,
                MeasurementsWorker.BATTERY to deviceBattery
            )

            val request = OneTimeWorkRequestBuilder<MeasurementsWorker>()
                .setInputData(data)
                .setConstraints(constraints).build()

            WorkManager.getInstance(getApplication()).enqueue(request)

            _uploadStatus.postValue(
                UploadStatus(
                    false,
                    "No internet connection. Intervals will be uploaded at a later time."
                )
            )
        }
    }
}