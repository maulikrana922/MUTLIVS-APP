package com.es.multivs.data.database.measurements

import androidx.lifecycle.LiveData
import androidx.room.*
import com.es.multivs.data.database.entities.MeasurementData
import com.es.multivs.data.database.entities.NonMultiVSResults
import com.es.multivs.data.database.sets.BpCuffResults
import com.es.multivs.data.database.sets.GlucoseData
import com.es.multivs.data.database.sets.OximeterResults
import com.es.multivs.data.database.sets.WeightScaleResults

/**
 * Created by Marko on 10/19/2021.
 * Etrog Systems LTD.
 */
@Dao
interface MeasurementDao {

    @Query("SELECT last_checked_date FROM gatewaydata")
    suspend fun getLastCheckedDate(): String

    @Query("UPDATE gatewaydata SET last_checked_date = :currentDate")
    suspend fun updateLastCheckedDate(currentDate: String)

    @Query("SELECT * FROM MeasurementData")
    suspend fun getLastMeasurementItems(): List<MeasurementData>?

    @Transaction
    suspend fun deleteAndInsertMeasurements(newItemsList: List<MeasurementData>) {
        deleteMeasurements()
        insertMeasurements(newItemsList)
    }

    @Query("DELETE FROM MeasurementData")
    suspend fun deleteMeasurements()

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMeasurements(newItemsList: List<MeasurementData>)

    @Query("SELECT bp_cuff_sys, bp_cuff_dia, heart_rate FROM nonmultivsresults")
    fun getBpCuffResults(): LiveData<BpCuffResults>

    @Query("UPDATE nonmultivsresults SET bp_cuff_sys=:sys, bp_cuff_dia=:dia, heart_rate=:heartRate WHERE id=1")
    suspend fun updateBpCuffResults(sys: Int, dia: Int, heartRate: Int)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertNonMultiVSResults(nonMultiVSResults: NonMultiVSResults)

    @Transaction
    suspend fun deleteAndInsertNonMultiVSResults(nonMultiVSResults: NonMultiVSResults) {
        deleteNonMultiVSResults()
        insertNonMultiVSResults(nonMultiVSResults)
    }

    @Query("DELETE FROM nonmultivsresults")
    suspend fun deleteNonMultiVSResults()

    @Query("SELECT oximeter_spo2, heart_rate FROM nonmultivsresults")
    fun getOximeterResults(): LiveData<OximeterResults>

    @Query("UPDATE nonmultivsresults SET oximeter_spo2=:spO2, heart_rate=:heartRate")
    suspend fun updateOximeterResults(spO2: Int, heartRate: Int)

    @Query("SELECT is_manual FROM gatewaydata")
    suspend fun getIsManualInput(): Int

    @Query("UPDATE nonmultivsresults SET thermometer_temperature =:thermometerResult")
    suspend fun updateThermometerResults(thermometerResult: String)

    @Query("SELECT glucose_level, glucose_event_tag_index FROM nonmultivsresults")
    fun getGlucoseData(): LiveData<GlucoseData>

    @Query("UPDATE nonmultivsresults SET glucose_level=:glucoseLevel, glucose_event_tag_index=:eventTagIndex")
    suspend fun updateGlucoseResults(glucoseLevel: Int, eventTagIndex: Int)

    @Query("SELECT thermometer_temperature FROM nonmultivsresults")
    fun getThermometerResults(): LiveData<String>

    @Query("UPDATE nonmultivsresults SET weight_scale =:weight")
    suspend fun updateWeightScaleResults(weight: Double)

    @Query("SELECT weight_scale FROM nonmultivsresults")
    fun getWeightResults(): LiveData<WeightScaleResults>

    @Query("SELECT * FROM nonmultivsresults ORDER BY id DESC LIMIT 1")
    fun getNonMultiVsResultsLiveData(): LiveData<NonMultiVSResults>

    @Query("SELECT * FROM nonmultivsresults ORDER BY id DESC LIMIT 1")
    suspend fun getNonMultiVsResults(): NonMultiVSResults

    @Query("UPDATE nonmultivsresults SET active_measurement=:activeMeasurement")
    suspend fun insertActiveMeasurement(activeMeasurement: String)

    @Query("SELECT active_measurement FROM nonmultivsresults")
    suspend fun getActiveMeasurement(): String

    @Query("UPDATE MeasurementData SET is_task_done=1 WHERE time_stamp=:timestamp")
    suspend fun updateTaskDone(timestamp: String): Int

}