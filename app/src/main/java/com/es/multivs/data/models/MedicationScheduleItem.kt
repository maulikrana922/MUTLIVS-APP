package com.es.multivs.data.models

import com.es.multivs.data.database.entities.MedicationData
import java.io.Serializable
import java.lang.StringBuilder

class MedicationScheduleItem(
    var medicationList: ArrayList<MedicationData>,
    var isTaskDone: Boolean
) : ScheduleItem,
    Serializable {

    var isActive: Boolean = false
    var timeStamp: String = medicationList[0].timeStamp
//    var isTaskDone: Boolean = false

    override fun showContent(): String {
        val builder = StringBuilder()
        for (med in medicationList) {
            builder.append("Name: ${med.medicationName}")
                .append("\nDosage Form: ${med.dosageForm}")
                .append("\nQuantity: ${med.dosageQuantity}")
                .append("\n-----------------------------------\n")
        }

        return builder.toString()
    }

    override fun getItemType(): String {
        return "medication_item"
    }

    override fun getTitle(): String {
        return "Medications"
    }

    override fun getTime(): String {
        return timeStamp
    }

    override fun isItemActive(): Boolean {
        return isActive
    }

    override fun taskDone(): Boolean {
        return isTaskDone
    }


}