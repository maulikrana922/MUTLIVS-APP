package com.es.multivs.data.database.multivs

import com.es.multivs.data.database.entities.MultiVSData
import com.es.multivs.data.database.sets.PatchVariables
import com.es.multivs.data.database.sets.TestType
import javax.inject.Inject

class MultiVsDbHelper @Inject constructor(
    private val dataDao: MultiVSDao
) {

    suspend fun updateES008Variables(
        ecgDataChecked: Boolean,
        ppgDataChecked: Boolean,
        heartRateChecked: Boolean,
        respChecked: Boolean,
        temperatureChecked: Boolean,
        stepsChecked: Boolean,
        devicePosition: String,
        calibrationDevicePosition:String,
        badDataDetection: String,
        testType: String
    ) {
        dataDao.updateMultiVSVariables(
            ecgDataChecked,
            ppgDataChecked,
            heartRateChecked,
            respChecked,
            temperatureChecked,
            stepsChecked,
            devicePosition,
            calibrationDevicePosition,
            badDataDetection,
            testType
        )
    }

    suspend fun insertIfEmpty() {
        val count: Int = dataDao.getRowCount()
        if (count == 0) {
            dataDao.insert(MultiVSData(1))
        }
    }

    suspend fun getTestType(): TestType {
        return dataDao.getECGAndPPG()
    }

    suspend fun getVariables(): PatchVariables {
        return dataDao.getVariables()
    }

    suspend fun getMeasurementBodyPosition(): String {
        return dataDao.getMeasurementBodyPosition()
    }

    suspend fun getCalibrationBodyPosition(): String {
        return dataDao.getCalibrationBodyPosition()
    }


}