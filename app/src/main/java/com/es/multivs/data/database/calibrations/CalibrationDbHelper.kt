package com.es.multivs.data.database.calibrations

import com.es.multivs.data.database.entities.CalibrationData
import javax.inject.Inject

class CalibrationDbHelper @Inject constructor(
    private val dataDao: CalibrationDao
) {
    suspend fun getLastCalibrations(): List<CalibrationData>? {
        return dataDao.getLastCalibrations()
    }

    suspend fun deleteAndInsertCalibrations(newItemsList: MutableList<CalibrationData>) {
        dataDao.deleteAndInsertCalibrations(newItemsList)
    }

    suspend fun updateTaskDone(activeCalibration: String) {
        dataDao.updateTaskDone(activeCalibration)
    }


}