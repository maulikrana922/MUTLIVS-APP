package com.es.multivs.data.database.entities

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.google.gson.annotations.Expose
import com.google.gson.annotations.SerializedName
import java.io.Serializable

/**
 * Created by Marko on 10/21/2021.
 * Etrog Systems LTD.
 */
@Entity
class CalibrationResults : Serializable {

    @PrimaryKey(autoGenerate = true)
    private val id = 0

    @SerializedName("username")
    @Expose
    var username: String? = null

    @SerializedName("uuid")
    @Expose
    var uuid: String? = null

    @SerializedName("lat")
    @Expose
    var lat: String? = null

    @SerializedName("lng")
    @Expose
    var lng: String? = null

    @SerializedName("sensor_type")
    @Expose
    var sensorType = "1"

    @SerializedName("temp_value")
    @Expose
    var tempValue: String? = null

    @SerializedName("step_value")
    @Expose
    var stepValue: String? = null

    @SerializedName("batterylevel")
    @Expose
    var batterylevel: String? = null

    @SerializedName("iosBatteryLevel")
    @Expose
    var iosBatteryLevel: String? = null

    @SerializedName("cuff_bp_sys")
    @Expose
    var sys: String? = null

    @SerializedName("cuff_bp_dia")
    @Expose
    var dia: String? = null

    @SerializedName("device_position")
    @Expose
    var devicePosition: String? = null

    @SerializedName("body_position")
    @Expose
    var bodyPosition: String? = null

    @SerializedName("filename")
    @Expose
    var filename: String? = null

    @SerializedName("timestamp")
    @Expose
    var timeStamp: String? = null

    @SerializedName("data")
    @Expose
    var data: String? = null

    @SerializedName("bp_cuff_placement")
    @Expose
    var bpCuffPlacement: String? = null
}