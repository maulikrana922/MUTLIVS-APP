package com.es.multivs.data.models

import com.es.multivs.data.database.entities.CalibrationData
import com.es.multivs.data.network.netmodels.UserSchedule
import com.google.gson.annotations.SerializedName
import java.io.Serializable

/**
 * Created by Marko on 10/18/2021.
 * Etrog Systems LTD.
 */
class CalibrationSchedule : Comparable<CalibrationData>, Serializable {

    @SerializedName("timeOfMeasurement1")
    var timeOfMeasurement1: String? = null

    @SerializedName("timeOfMeasurement2")
    var timeOfMeasurement2: String? = null

    @SerializedName("timeOfMeasurement3")
    var timeOfMeasurement3: String? = null

    @SerializedName("timeOfMeasurement4")
    var timeOfMeasurement4: String? = null

    fun set(schedule: UserSchedule) {
        timeOfMeasurement1 = schedule.timeOfMeasurement1
        timeOfMeasurement2 = schedule.timeOfMeasurement2
        timeOfMeasurement3 = schedule.timeOfMeasurement3
        timeOfMeasurement4 = schedule.timeOfMeasurement4
    }


    fun getTimeMeasurement(i: Int): String? {
        var timeStamp: String? = ""
        when (i) {
            1 -> timeStamp = timeOfMeasurement1
            2 -> timeStamp = timeOfMeasurement2
            3 -> timeStamp = timeOfMeasurement3
            4 -> timeStamp = timeOfMeasurement4
        }
        return timeStamp
    }

    override fun compareTo(other: CalibrationData): Int {
        return 0;
    }
}