package com.es.multivs.data.database.calibrations

import androidx.room.*
import com.es.multivs.data.database.entities.CalibrationData

/**
 * Created by Marko on 10/19/2021.
 * Etrog Systems LTD.
 */
@Dao
interface CalibrationDao {

    @Query("SELECT * FROM calibrationdata")
    suspend fun getLastCalibrations(): List<CalibrationData>?

    @Transaction
    suspend fun deleteAndInsertCalibrations(newItemsList: MutableList<CalibrationData>) {
        deleteCalibrations()
        insertCalibrations(newItemsList)
    }

    @Query("DELETE FROM CalibrationData")
    suspend fun deleteCalibrations()

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCalibrations(newItemsList: MutableList<CalibrationData>)

    @Query("UPDATE calibrationdata SET is_task_done=1 WHERE time_stamp=:activeCalibration")
    suspend fun updateTaskDone(activeCalibration: String)
}