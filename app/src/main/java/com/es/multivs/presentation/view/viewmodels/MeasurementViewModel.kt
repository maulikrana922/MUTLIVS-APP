package com.es.multivs.presentation.view.viewmodels

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import androidx.work.*
import com.es.multivs.data.database.entities.NonMultiVSResults
import com.es.multivs.data.database.measurements.MeasurementDbHelper
import com.es.multivs.data.database.sets.BpCuffResults
import com.es.multivs.data.database.sets.GlucoseData
import com.es.multivs.data.database.sets.OximeterResults
import com.es.multivs.data.database.sets.WeightScaleResults
import com.es.multivs.data.models.UploadStatus
import com.es.multivs.data.repository.MeasurementResultRepository
import com.es.multivs.data.utils.AppUtils
import com.es.multivs.data.utils.ScheduleUpdatesManager
import com.es.multivs.data.work.MeasurementsWorker
import com.es.multivs.presentation.view.fragments.MeasurementUploadListener
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * Created by Marko on 11/2/2021.
 * Etrog Systems LTD.
 */


@HiltViewModel
class MeasurementViewModel @Inject constructor(
    private val measurementDbHelper: MeasurementDbHelper,
    private val resultRepository: MeasurementResultRepository,
    private val updates: ScheduleUpdatesManager,
    application: Application
) : AndroidViewModel(application) {

    private val _uploadStatus = MutableLiveData<UploadStatus>()
    var uploadStatus: LiveData<UploadStatus> = _uploadStatus

    private var uploadListener: MeasurementUploadListener? = null

    fun saveBpCuffResults(sys: Int, dia: Int, heartRate: Int) {
        viewModelScope.launch {
            measurementDbHelper.updateBpCuffResults(sys, dia, heartRate)
        }
    }

    fun saveOximeterResults(spO2: Int, heartRate: Int) {
        viewModelScope.launch {
            measurementDbHelper.updateOximeterResults(spO2, heartRate)
        }
    }

    suspend fun getIsManual(): Int {
        return measurementDbHelper.getIsManualInput()
    }

    fun saveThermometerResults(thermometerResult: String) {
        viewModelScope.launch {
            measurementDbHelper.updateThermometerResults(thermometerResult)
        }
    }

    fun saveGlucometerResults(glucoseLevel: Int, eventTagIndex: Int) {
        viewModelScope.launch {
            measurementDbHelper.updateGlucoseResults(glucoseLevel, eventTagIndex)
        }
    }

    fun saveWeightScaleResults(weight: Double) {
        viewModelScope.launch {
            measurementDbHelper.updateWeightScaleResults(weight)
        }
    }

    fun getNonMultiVSResultsLiveData(): LiveData<NonMultiVSResults> {
        return measurementDbHelper.getNonMultiVSResultsLiveData()
    }

    suspend fun getNonMultiVSResults(): NonMultiVSResults {
        return measurementDbHelper.getNonMultiVSResults()
    }


    fun clearNonMultiVSResults() {
        viewModelScope.launch {
            measurementDbHelper.clearNonMultiVSResults()
        }
    }

    fun postResults(results: NonMultiVSResults) {
        val deviceBattery = AppUtils.getBatteryPercentage(getApplication())

        if (AppUtils.isInternetAvailable(getApplication())) {
            viewModelScope.launch {
                resultRepository.uploadNonMultiVSResults(results, deviceBattery) { isSuccessful ->
                    if (isSuccessful) {
                        clearNonMultiVSResults()
                        _uploadStatus.postValue(UploadStatus(true))
                    } else {
                        _uploadStatus.postValue(
                            UploadStatus(
                                false,
                                "There was a problem uploading the measurements"
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
                    "No internet connection. Measurements will be uploaded at a later time."
                )
            )
        }
    }

    fun setUploadListener(measurementUploadListener: MeasurementUploadListener) {
        uploadListener = measurementUploadListener
    }

    val weight: LiveData<WeightScaleResults> = measurementDbHelper.getWeightResults()

    var temperature: LiveData<String> = measurementDbHelper.getThermometerResults()

    var glucoseData: LiveData<GlucoseData> = measurementDbHelper.getGlucoseData()

    var oximeter: LiveData<OximeterResults> = measurementDbHelper.getOximeterResults()

    var bpCuffResults: LiveData<BpCuffResults> = measurementDbHelper.getBpCuffResults()
}