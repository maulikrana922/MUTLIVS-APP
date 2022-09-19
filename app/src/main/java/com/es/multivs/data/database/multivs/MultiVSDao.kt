package com.es.multivs.data.database.multivs

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.es.multivs.data.database.entities.MultiVSData
import com.es.multivs.data.database.sets.PatchVariables
import com.es.multivs.data.database.sets.TestType

/**
 * Created by Marko on 10/19/2021.
 * Etrog Systems LTD.
 */
@Dao
interface MultiVSDao {

//    """UPDATE multivsdata SET
//is_ecg_checked = :ecgDataChecked,
//is_ppg_checked =:ppgDataChecked,
//is_heart_rate_checked=:heartRateChecked,
//is_resp_checked=:respChecked,
//is_temperature_checked =:temperatureChecked,
//is_steps_checked=:stepsChecked,
//device_position=:devicePosition,
//test_type=:testType,
//calibration_count=:calibrationCount,
//bad_data_detection=:badDataDetection """

    @Query(
        """UPDATE multivsdata SET 
is_ecg_checked = :ecgDataChecked, 
is_ppg_checked =:ppgDataChecked, 
is_heart_rate_checked=:heartRateChecked,
is_resp_checked=:respChecked, 
is_temperature_checked =:temperatureChecked,
is_steps_checked=:stepsChecked,
measurement_device_position=:devicePosition,
calibration_device_position=:calibrationDevicePosition,
test_type=:testType,
bad_data_detection=:badDataDetection """
    )
    suspend fun updateMultiVSVariables(
        ecgDataChecked: Boolean,
        ppgDataChecked: Boolean,
        heartRateChecked: Boolean,
        respChecked: Boolean,
        temperatureChecked: Boolean,
        stepsChecked: Boolean,
        devicePosition: String,
        calibrationDevicePosition: String,
        badDataDetection: String,
        testType: String
    )

    @Query("SELECT COUNT(id) FROM multivsdata")
    suspend fun getRowCount(): Int

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(multiVSData: MultiVSData)

    @Query("SELECT is_ecg_checked, is_ppg_checked FROM MultiVSData")
    suspend fun getECGAndPPG(): TestType

    @Query("SELECT is_ecg_checked, is_ppg_checked, is_heart_rate_checked, is_resp_checked, is_temperature_checked, is_steps_checked FROM multivsdata")
    suspend fun getVariables(): PatchVariables

    @Query("SELECT measurement_device_position FROM MultiVSData")
    suspend fun getMeasurementBodyPosition(): String

    @Query("SELECT calibration_device_position FROM MultiVSData")
    suspend fun getCalibrationBodyPosition(): String
}