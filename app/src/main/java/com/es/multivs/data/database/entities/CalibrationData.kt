package com.es.multivs.data.database.entities

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Ignore
import androidx.room.PrimaryKey
import com.es.multivs.data.models.ScheduleItem
import java.io.Serializable
import java.util.ArrayList

/**
 * Created by Marko on 10/18/2021.
 * Etrog Systems LTD.
 */
@Entity
class CalibrationData : Comparable<CalibrationData>, ScheduleItem, Serializable {

    @PrimaryKey(autoGenerate = true)
    var id = 0

    @ColumnInfo(name = "time_stamp")
    var timeStamp: String = ""

    @ColumnInfo(name = "time_stamp_milli")
    var timeStampMilli: Long = 0

    @ColumnInfo(name = "is_active")
    var isActive = false

    @ColumnInfo(name = "is_task_done")
    var isTaskDone = false

    @Ignore
    var mDeviceTypes: ArrayList<String>? = null

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
    override fun showContent(): String {
        return "MULTIVS Calibration"
    }

    @Ignore
    override fun getItemType(): String {
        return "calibration_item"
    }

    @Ignore
    override fun getTitle(): String {
        return "Calibration"
    }

    override fun compareTo(other: CalibrationData): Int {
        if (getTime() == other.getTime()) {
            return 0
        }
        val hourMinutes1 = timeStamp.split(":").toTypedArray()
        val hourMinutes2: Array<String> = other.getTime().split(":").toTypedArray()

        val thisHour = hourMinutes1[0].toInt()
        val thisMinutes = hourMinutes1[1].toInt()

        val givenHour = hourMinutes2[0].toInt()
        val givenMinutes = hourMinutes2[1].toInt()

        return if (thisHour > givenHour) {
            1
        } else if (thisHour < givenHour) {
            -1
        } else thisMinutes.compareTo(givenMinutes)
    }

    override fun equals(other: Any?): Boolean {
        var isSameTimeStamp = false

        if (other is CalibrationData) {
            isSameTimeStamp = timeStamp == other.getTime()
        }

        return isSameTimeStamp
    }
}