package com.es.multivs.data.database.entities

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Ignore
import androidx.room.PrimaryKey
import com.es.multivs.data.models.ScheduleItem
import com.google.gson.Gson
import java.io.Serializable

/**
 * Created by Marko on 10/19/2021.
 * Etrog Systems LTD.
 */
@Entity
class MedicationData: Serializable, ScheduleItem {

    @PrimaryKey(autoGenerate = true)
    var id = 0

    @ColumnInfo(name = "asNeeded")
    var asNeeded: String = ""

    @ColumnInfo(name = "dosageForm")
    var dosageForm: String = ""

    @ColumnInfo(name = "dosageQuantity")
    var dosageQuantity: String = "N/A"

    @ColumnInfo(name = "frequency")
    var frequency: String = ""

    @ColumnInfo(name = "medicationID")
    var medicationID: Int = 0

    @ColumnInfo(name = "medicationName")
    var medicationName: String = ""

    @ColumnInfo(name = "stength")
    var strength: String = ""

    @ColumnInfo(name = "timeStamp")
    var timeStamp: String = ""

    @ColumnInfo(name = "isActive")
    var isActive: Boolean = false

    @ColumnInfo(name = "isTaskDone")
    var isTaskDone: Boolean = false


    @Ignore
    override fun showContent(): String {
        //TODO: show content in all schedule items
        return medicationName
    }

    @Ignore
    override fun getItemType(): String {
        return "medication_item"
    }

    @Ignore
    override fun getTitle(): String {
        return "Medications"
    }

    @Ignore
    override fun getTime(): String {
        return timeStamp
    }

    @Ignore
    override fun isItemActive(): Boolean {
        return isActive
    }

    @Ignore
    override fun taskDone(): Boolean {
        return isTaskDone
    }

    @Ignore
    fun deepCopy(): MedicationData{
        val json  = Gson().toJson(this)
        return Gson().fromJson(json,MedicationData::class.java)
    }

    @Ignore
    override fun equals(other: Any?): Boolean {
        var isSameTimeStamp = false
        var isSameMedID = false
        var isSameDBID = false

        if (other is MedicationData){
            isSameTimeStamp = (this.timeStamp == other.timeStamp)
            isSameMedID = (this.medicationID == other.medicationID)
            isSameDBID = (this.id == other.id)
        }

        return isSameMedID && isSameTimeStamp /*&& isSameDBID*/
    }


}