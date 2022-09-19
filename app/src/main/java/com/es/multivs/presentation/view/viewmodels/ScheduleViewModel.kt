package com.es.multivs.presentation.view.viewmodels

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.es.multivs.data.TimeStampComparator
import com.es.multivs.data.database.calibrations.CalibrationDbHelper
import com.es.multivs.data.database.entities.*
import com.es.multivs.data.database.measurements.MeasurementDbHelper
import com.es.multivs.data.database.multivs.MultiVsDbHelper
import com.es.multivs.data.database.medications.MedicationDbHelper
import com.es.multivs.data.database.gateway.GatewayDbHelper
import com.es.multivs.data.database.survey.SurveyDbHelper
import com.es.multivs.data.models.*
import com.es.multivs.data.repository.MedicationRepository
import com.es.multivs.data.repository.SurveyRepository
import com.es.multivs.data.repository.UserScheduleRepository
import com.es.multivs.data.utils.ScheduleUtils
import com.es.multivs.data.utils.AppUtils
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.*
import javax.inject.Inject
import kotlin.collections.ArrayList


/**
 * Created by Marko on 10/18/2021.
 * Etrog Systems LTD.
 */
@HiltViewModel
class ScheduleViewModel @Inject constructor(
    private val multiVSDbHelper: MultiVsDbHelper,
    private val measurementDbHelper: MeasurementDbHelper,
    private val medicationDbHelper: MedicationDbHelper,
    private val calibrationDbHelper: CalibrationDbHelper,
    private val gatewayDbHelper: GatewayDbHelper,
    private val surveyDbHelper: SurveyDbHelper,
    private val measurementRepo: UserScheduleRepository,
    private val medicationRepo: MedicationRepository,
    private val surveyRepo: SurveyRepository,
    application: Application
) : AndroidViewModel(application) {

    var recentMeasurements = listOf<MeasurementData>()
    private lateinit var lastCheckedDate: String
    private lateinit var currentDate: String

    private val _doingSomething: MutableLiveData<Boolean> = MutableLiveData()
    var doingSomething: LiveData<Boolean> = _doingSomething

    var showTimerLiveData = measurementRepo.showTimerLiveData

    /**
     * Server location LiveData
     */
    val serverLocation: LiveData<String>
        get() {
            return gatewayDbHelper.getUserCountryLiveData()
        }

    /**
     * Frequency LiveData
     */
    private val mFrequency = MutableLiveData<Int>()

     var isCalibrationRequired = MutableLiveData<Boolean>()

    /**
     * Calibrations LiveData
     */
    private val _calibrationSchedules = MutableLiveData<List<CalibrationData>>()
    var calibrations: LiveData<List<CalibrationData>> = _calibrationSchedules

    /**
     * Measurements LiveData
     */
    private val _measurementSchedules = MutableLiveData<List<MeasurementData>>()
    var measurements: LiveData<List<MeasurementData>> = _measurementSchedules

    /**
     * Medications LiveData
     */
    private val _medicationSchedules = MutableLiveData<List<MedicationScheduleItem>>()
    var medications: LiveData<List<MedicationScheduleItem>> = _medicationSchedules

    private suspend fun fetchSurveysAsync(ignoreDatabase: Boolean): List<SurveyScheduleItem> {

        currentDate = AppUtils.getCurrentDate()
        lastCheckedDate = measurementDbHelper.getLastCheckedDate()
        val isSameDay: Boolean = (currentDate == lastCheckedDate)

        val isInternetAvailable = AppUtils.isInternetAvailable(getApplication())
        if (isInternetAvailable) {
            val response = surveyRepo.fetchSurveys()
            if (response != null) {
                response.let {
                    return handleSurveyResults(it, isSameDay, ignoreDatabase)
                }
            }else{
                surveyDbHelper.deleteAndSaveSurveys(emptyList())
            }
        }
        return emptyList()
    }

    private suspend fun handleSurveyResults(
        list: List<Survey>,
        isSameDay: Boolean,
        ignoreDatabase: Boolean
    ): MutableList<SurveyScheduleItem> {

        val surveyScheduleItems: List<SurveyScheduleItem> =
            ScheduleUtils.aggregateSurveyListByTimeStamps(list)

        if (isSameDay && !ignoreDatabase) {
            val dbItems: List<SurveyScheduleItem>? = surveyDbHelper.getLastSurveyItems()
            val newItemsList = mutableListOf<SurveyScheduleItem>()

            for (item in surveyScheduleItems) {
                val dbItem = ScheduleUtils.doesSurveyExist(dbItems, item)
                if (dbItem != null && dbItem.isTaskDone) {
                    newItemsList.add(dbItem)
                } else {
                    newItemsList.add(item)
                }
            }

            Collections.sort(newItemsList, TimeStampComparator())
            surveyDbHelper.deleteAndSaveSurveys(newItemsList)
            return newItemsList
        } else {
            Collections.sort(surveyScheduleItems, TimeStampComparator())
            surveyDbHelper.deleteAndSaveSurveys(surveyScheduleItems)
            return surveyScheduleItems.toMutableList()
        }
    }


    private suspend fun fetchMedicationsAsync(ignoreDatabase: Boolean = false): List<MedicationScheduleItem> {

        currentDate = AppUtils.getCurrentDate()
        lastCheckedDate = measurementDbHelper.getLastCheckedDate()

        /**
         * get the last checked date to see if need to update measurement schedule
         */
        val isSameDay: Boolean = (currentDate == lastCheckedDate)

        val isInternetAvailable = AppUtils.isInternetAvailable(getApplication())
        return if (isInternetAvailable) {
            val medicationModel = medicationRepo.fetchMedications()
            if (medicationModel.error.isNullOrEmpty()) {
                handleMedicationResults(medicationModel, isSameDay, ignoreDatabase)
            } else {
                medicationDbHelper.deleteAndInsertMedications(mutableListOf())
                mutableListOf()
            }
        } else {
            medicationDbHelper.deleteAndInsertMedications(mutableListOf())
            mutableListOf()
        }
    }

    private suspend fun fetchMeasurementsAsync(ignoreDatabase: Boolean = false): List<MeasurementData> {

        currentDate = AppUtils.getCurrentDate()
        lastCheckedDate = measurementDbHelper.getLastCheckedDate()
        val isInternetAvailable = AppUtils.isInternetAvailable(getApplication())
        if (isInternetAvailable) {
            val measurementModel: MeasurementModel = measurementRepo.fetchUserSchedule()
            return if (measurementModel.error.isNullOrEmpty()) {
                isCalibrationRequired.postValue(measurementModel.userSchedule.is_calibration_required)
                mFrequency.postValue(measurementModel.frequency)
                multiVSDbHelper.insertIfEmpty()
                updateES008Variables(measurementModel)
                recentMeasurements = handleNonePatchSchedule(
                    measurementModel,
                    ignoreDatabase
                )
                recentMeasurements // returning this
            } else {
                listOf()
            }
        } else {
            return listOf()
        }

    }

    private suspend fun fetchCalibrationsAsync(ignoreDatabase: Boolean = false): List<CalibrationData> {

        currentDate = AppUtils.getCurrentDate()
        lastCheckedDate = measurementDbHelper.getLastCheckedDate()

        val isInternetAvailable = AppUtils.isInternetAvailable(getApplication())
        return if (isInternetAvailable) {
            val measurementModel: MeasurementModel = measurementRepo.fetchUserSchedule()
            if (measurementModel.error.isNullOrEmpty()) {
                handleCalibrationSchedule(measurementModel, ignoreDatabase)
            } else {
                listOf()
            }
        } else {
            listOf()
        }
    }

    private suspend fun handleNonePatchSchedule(
        measurementModel: MeasurementModel,
        ignoreDatabase: Boolean
    ): MutableList<MeasurementData> {
        val listToReturn: MutableList<MeasurementData>
        val items: ArrayList<MeasurementData> = ArrayList()

        for ((key, value) in measurementModel.getMap().entries) {
            val measurementScheduleItem = MeasurementData(key, value)
            items.add(measurementScheduleItem)
        }

        /**
         * get the last checked date to see if need to update measurement schedule
         */
        val isSameDay: Boolean = (currentDate == lastCheckedDate)

        /**
         * take last modified schedule item
         */
        if (lastCheckedDate.isNotEmpty() && isSameDay && !ignoreDatabase) {
            val dbItems: List<MeasurementData>? = measurementDbHelper.getLastScheduleItems()
            val newItemsList = mutableListOf<MeasurementData>()

            for (item in items) {
                val dbItem = ScheduleUtils.doesMeasurementExist(dbItems, item)
                if (dbItem != null && dbItem.isTaskDone) {
                    newItemsList.add(dbItem)
                } else {
                    newItemsList.add(item)
                }
            }
            Collections.sort(newItemsList, TimeStampComparator())
            measurementDbHelper.deleteAndInsertMeasurements(newItemsList)
            listToReturn = newItemsList
        } else {
            Collections.sort(items, TimeStampComparator())
            measurementDbHelper.deleteAndInsertMeasurements(items)
            listToReturn = items
        }

        measurementDbHelper.updateLastCheckedDate(currentDate)
        return listToReturn
    }

    private suspend fun handleCalibrationSchedule(
        measurementModel: MeasurementModel,
        ignoreDatabase: Boolean
    ): MutableList<CalibrationData> {
        val items = mutableListOf<CalibrationData>()
        measurementModel.let {
            for (i in 0..3) {
                val item = CalibrationData()
                val timeStamp: String? = it.calibrationSchedule.getTimeMeasurement(i)
                timeStamp?.let { time ->
                    if (time.isNotEmpty()) {
                        item.timeStamp = time
                        items.add(item)
                    }
                }
            }
        }

        /**
         * get the last checked date to see if need to update measurement schedule
         */
        val isSameDay: Boolean = (currentDate == lastCheckedDate)
        if (lastCheckedDate.isNotEmpty() && isSameDay && !ignoreDatabase) {
            val dbItems: List<CalibrationData>? = calibrationDbHelper.getLastCalibrations()
            val newItemsList = mutableListOf<CalibrationData>()
            for (item in items) {
                val dbItem = ScheduleUtils.doesCalibrationExist(dbItems, item)
                if (dbItem != null && dbItem.isTaskDone) {
                    newItemsList.add(dbItem)
                } else {
                    newItemsList.add(item)
                }
            }
            Collections.sort(newItemsList, TimeStampComparator())
            calibrationDbHelper.deleteAndInsertCalibrations(newItemsList)
            return newItemsList
        } else {
            Collections.sort(items, TimeStampComparator())
            calibrationDbHelper.deleteAndInsertCalibrations(items)
            return items
        }
    }

    private suspend fun handleMedicationResults(
        medicationModel: MedicationModel,
        isSameDay: Boolean,
        ignoreDatabase: Boolean
    ): List<MedicationScheduleItem> {
        if (isSameDay && !ignoreDatabase) {
            val dbItems: List<MedicationData>? =
                medicationDbHelper.getLastMedicationItems()
            val newItemsList = mutableListOf<MedicationData>()

            for (item in medicationModel.medicationList) {
                val dbItem = ScheduleUtils.doesMedicationExist(dbItems, item)
                if (dbItem != null && dbItem.isTaskDone) {
                    newItemsList.add(dbItem)
                } else {
                    newItemsList.add(item)
                }
            }
            Collections.sort(medicationModel.medicationList, TimeStampComparator())
            medicationDbHelper.deleteAndInsertMedications(newItemsList)
            return ScheduleUtils.aggregateMedsByTimeStamps(newItemsList)
        } else {
            Collections.sort(medicationModel.medicationList, TimeStampComparator())
            medicationDbHelper.deleteAndInsertMedications(medicationModel.medicationList)
            return ScheduleUtils.aggregateMedsByTimeStamps(medicationModel.medicationList)
        }
    }

    private suspend fun updateES008Variables(measurementModel: MeasurementModel) {

        val isHeartRateChecked: Boolean =
            measurementModel.userSchedule.hearRateData.isChecked

        val isRespChecked: Boolean =
            measurementModel.userSchedule.respirationData.isChecked

        val isTemperatureChecked: Boolean =
            measurementModel.userSchedule.temperatureData.isChecked

        val isECGDataChecked: Boolean = measurementModel.userSchedule.ecgData.isChecked
        val isPPGDataChecked: Boolean = measurementModel.userSchedule.ppgData.isChecked
        val isStepsChecked: Boolean = measurementModel.userSchedule.stepsData.isChecked
        val measurementDevicePosition: String =
            measurementModel.userSchedule.measurementDevicePosition
        val calibrationDevicePosition: String =
            measurementModel.userSchedule.calibrationDevicePosition
        val badDataDetection: String = measurementModel.userSchedule.badDataDetection
        val testType: String = measurementModel.userSchedule.testType

        multiVSDbHelper.updateES008Variables(
            isECGDataChecked,
            isPPGDataChecked,
            isHeartRateChecked,
            isRespChecked,
            isTemperatureChecked,
            isStepsChecked,
            measurementDevicePosition,
            calibrationDevicePosition,
            badDataDetection,
            testType
        )
    }

    suspend fun insertActiveMeasurement(activeMeasurement: String) {
        measurementDbHelper.insertActiveMeasurement(activeMeasurement)
    }

    suspend fun getActiveMeasurement(): String {
        return measurementDbHelper.getActiveMeasurement()
    }

    /**
     * Finds the active calibration and marks it as complete
     */
    suspend fun updateCalibrationTasksCompletion() {
        withContext(Dispatchers.IO) {
            val list = calibrationDbHelper.getLastCalibrations()
            list?.let {
                val activeCalibration = ScheduleUtils.findActiveCalibration(it)
                calibrationDbHelper.updateTaskDone(activeCalibration)
            }
        }
    }

    /**
     * Finds the active measurement and marks it as complete
     */
    suspend fun updateMeasurementTasksCompletion(timestamp: String) {
        withContext(Dispatchers.IO) {
            measurementDbHelper.updateTaskDone(timestamp)
        }
    }

    /**
     * Finds the active medication and marks it as complete
     */
    suspend fun updateMedicationTasksCompletion(timestamp: String) {
        withContext(Dispatchers.IO) {
            medicationDbHelper.updateTaskDone(timestamp)
        }
    }

    suspend fun updateSurveyTaskCompletion(timestamp: String) {
        withContext(Dispatchers.IO) {
            surveyDbHelper.updateTaskDone(timestamp)
        }
    }

    suspend fun getDBMeasurements(): List<MeasurementData> {
        val list = measurementDbHelper.getLastScheduleItems()
        return if (list.isNullOrEmpty()) {
            emptyList()
        } else {
            list
        }
    }

    suspend fun getDBMedications(): List<MedicationScheduleItem> {
        val list = medicationDbHelper.getLastMedicationItems()
        return if (list.isNullOrEmpty()) {
            emptyList()
        } else {
            ScheduleUtils.aggregateMedsByTimeStamps(list)
        }
    }

    suspend fun getDBCalibrations(): List<CalibrationData> {
        val list = calibrationDbHelper.getLastCalibrations()
        return if (list.isNullOrEmpty()) {
            emptyList()
        } else {
            list
        }
    }

    suspend fun getDBSurveysByDay(): List<SurveyScheduleItem> {
        val list = surveyDbHelper.getLastSurveyItems()
        val today = AppUtils.getCurrentDayOfWeek()
        val listToReturn = mutableListOf<SurveyScheduleItem>()

        list.forEach {
            if (today in it.daysOfWeek){
                listToReturn.add(it)
            }
        }

        return listToReturn
    }

    suspend fun fetchAllSchedules(ignoreDatabase: Boolean): MutableList<ScheduleItem> {
        return withContext(Dispatchers.IO) {
            val list = mutableListOf<ScheduleItem>()

            val t1 = System.currentTimeMillis()
            val measurementList1 = async { fetchMeasurementsAsync(ignoreDatabase) }

            val medsList1 = async { fetchMedicationsAsync(ignoreDatabase) }

            val calibrationsList1 = async { fetchCalibrationsAsync(ignoreDatabase) }

            val surveyList1 = async { fetchSurveysAsync(ignoreDatabase) }

            list.addAll(measurementList1.await())
            list.addAll(medsList1.await())
            list.addAll(calibrationsList1.await())
            list.addAll(surveyList1.await())

            list
        }
    }


    init {
        viewModelScope.launch {
            currentDate = AppUtils.getCurrentDate()
            lastCheckedDate = measurementDbHelper.getLastCheckedDate()

            measurementDbHelper.clearNonMultiVSResults()
        }
    }
}