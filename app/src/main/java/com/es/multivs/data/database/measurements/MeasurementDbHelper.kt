package com.es.multivs.data.database.measurements


import androidx.lifecycle.LiveData
import com.es.multivs.data.database.entities.MeasurementData
import com.es.multivs.data.database.entities.NonMultiVSResults
import com.es.multivs.data.database.sets.BpCuffResults
import com.es.multivs.data.database.sets.GlucoseData
import com.es.multivs.data.database.sets.OximeterResults
import com.es.multivs.data.database.sets.WeightScaleResults
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import javax.inject.Inject

class MeasurementDbHelper @Inject constructor(
    private val dataDao: MeasurementDao
) {
    suspend fun getLastCheckedDate(): String {
        return dataDao.getLastCheckedDate()
    }

    suspend fun getLastScheduleItems(): List<MeasurementData>? {
        return dataDao.getLastMeasurementItems()
    }

    suspend fun deleteAndInsertMeasurements(newItemsList: List<MeasurementData>) {
        dataDao.deleteAndInsertMeasurements(newItemsList)
    }

    suspend fun updateLastCheckedDate(currentDate: String) {
        dataDao.updateLastCheckedDate(currentDate)
    }

    fun getBpCuffResults(): LiveData<BpCuffResults> {
        return dataDao.getBpCuffResults()
    }

    suspend fun updateBpCuffResults(sys: Int, dia: Int, heartRate: Int) {
        dataDao.updateBpCuffResults(sys, dia, heartRate)
    }

    suspend fun insertNonMultiVSResults(nonMultiVSResults: NonMultiVSResults) {
        dataDao.insertNonMultiVSResults(nonMultiVSResults)
    }

    fun getOximeterResults(): LiveData<OximeterResults> {
        return dataDao.getOximeterResults()
    }

    suspend fun updateOximeterResults(spO2: Int, heartRate: Int) {
        dataDao.updateOximeterResults(spO2, heartRate)
    }

    suspend fun getIsManualInput() : Int {
        return dataDao.getIsManualInput()
    }

    suspend fun updateThermometerResults(thermometerResult: String) {
        dataDao.updateThermometerResults(thermometerResult)
    }

    fun getGlucoseData(): LiveData<GlucoseData> {
        return dataDao.getGlucoseData()
    }

    suspend fun updateGlucoseResults(glucoseLevel: Int, eventTagIndex: Int) {
        dataDao.updateGlucoseResults(glucoseLevel, eventTagIndex)
    }

    fun getThermometerResults(): LiveData<String> {
        return dataDao.getThermometerResults()
    }

    suspend fun updateWeightScaleResults(weight: Double) {
        dataDao.updateWeightScaleResults(weight)
    }

    fun getWeightResults(): LiveData<WeightScaleResults> {
        return dataDao.getWeightResults()
    }

    fun getNonMultiVSResultsLiveData(): LiveData<NonMultiVSResults> {
        return dataDao.getNonMultiVsResultsLiveData()
    }

    suspend fun getNonMultiVSResults(): NonMultiVSResults {
        return dataDao.getNonMultiVsResults()
    }

    suspend fun clearNonMultiVSResults() {

        val nmr = NonMultiVSResults()
        nmr.id = 1
        dataDao.deleteAndInsertNonMultiVSResults(nmr)
    }


    suspend fun insertActiveMeasurement(activeMeasurement: String) {
        dataDao.insertActiveMeasurement(activeMeasurement)
    }

    suspend fun getActiveMeasurement(): String {
        return withContext(Dispatchers.IO){
            dataDao.getActiveMeasurement()
        }
    }

    suspend fun updateTaskDone(timestamp: String) {

        val rowsNum = dataDao.updateTaskDone(timestamp)
    }


}