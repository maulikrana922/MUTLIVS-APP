package com.es.multivs.data.database.entities

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Ignore
import androidx.room.PrimaryKey
import com.es.multivs.data.models.ScheduleItem
import com.es.multivs.data.utils.ScheduleUtils
import java.io.Serializable
import java.util.ArrayList

/**
 * Created by Marko on 10/19/2021.
 * Etrog Systems LTD.
 */
@Entity
class MeasurementData(
    @ColumnInfo(name = "time_stamp") var timeStamp: String,
    @ColumnInfo(name = "device_list") var deviceList: ArrayList<String>
    ) : ScheduleItem, Serializable {


    @PrimaryKey(autoGenerate = true)
    var id = 0

//    @ColumnInfo(name = "time_stamp")
//    var mTimeStamp: String = ""

    @ColumnInfo(name = "time_stamp_milli")
    var timeStampMilli: Long = 0

    @ColumnInfo(name = "device_string")
    var deviceStrings: String = ""

    @ColumnInfo(name = "is_active")
    var isActive = false

    @ColumnInfo(name = "is_task_done")
    var isTaskDone = false

    @Ignore
    override fun showContent(): String {
        return ScheduleUtils.parseDeviceList(deviceList)
    }

    @Ignore
    override fun getItemType(): String {
        return "measurement_item"
    }

    @Ignore
    override fun getTitle(): String {
        return "Measurements"
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
    override fun equals(other: Any?): Boolean {
        var isSameTimeStamp = false
        var isSameDevices = false

        if (other is MeasurementData){
            isSameTimeStamp = (this.timeStamp == other.timeStamp)
            isSameDevices = (this.deviceList == other.deviceList)
        }

        return isSameDevices && isSameTimeStamp
    }


    //    @ColumnInfo(name = "device_list")
//    private val mDeviceTypes: ArrayList<String> = ArrayList()
}