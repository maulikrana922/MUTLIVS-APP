package com.es.multivs.data.database.medications

import com.es.multivs.data.database.entities.MedicationData
import javax.inject.Inject

class MedicationDbHelper @Inject constructor(
    private val dataDao: MedicationDao
) {
    suspend fun getLastMedicationItems(): List<MedicationData>? {
        return dataDao.getLastMedicationItems()
    }

    suspend fun deleteAndInsertMedications(newItemsList: MutableList<MedicationData>) {
        dataDao.deleteAndInsertMedications(newItemsList)
    }

    suspend fun updateTaskDone(timestamp: String) {
        dataDao.updateTaskDone(timestamp)
    }


}