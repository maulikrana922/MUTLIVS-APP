package com.es.multivs.data.database.medications

import androidx.room.*
import com.es.multivs.data.database.entities.MedicationData

/**
 * Created by Marko on 10/19/2021.
 * Etrog Systems LTD.
 */
@Dao
interface MedicationDao {

    @Query("SELECT * FROM MedicationData")
    suspend fun getLastMedicationItems(): List<MedicationData>?

    @Transaction
    suspend fun deleteAndInsertMedications(newItemsList: MutableList<MedicationData>) {
        deleteMedications()
        insertMedications(newItemsList)
    }

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMedications(newItemsList: MutableList<MedicationData>)

    @Query("DELETE FROM MedicationData")
    suspend fun deleteMedications()

    @Query("UPDATE MedicationData SET isTaskDone=1 WHERE timeStamp=:timestamp")
    suspend fun updateTaskDone(timestamp: String)
}