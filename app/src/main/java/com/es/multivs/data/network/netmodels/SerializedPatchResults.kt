package com.es.multivs.data.network.netmodels

import androidx.room.PrimaryKey
import androidx.room.ColumnInfo
import androidx.room.Entity
import com.google.gson.annotations.Expose
import com.google.gson.annotations.SerializedName
import java.io.Serializable

@Entity
class SerializedPatchResults : Serializable {

    @PrimaryKey(autoGenerate = true)
    var id = 0

    @ColumnInfo(name = "data")
    @SerializedName("data")
    @Expose
    var data: String? = null

    //    @SerializedName("heart_value")
    //    @Expose
    //    private String heartValue;

    @SerializedName("filename")
    @Expose
    var filename: String? = null

    @SerializedName("step_value")
    @Expose
    var stepValue: String? = null

    @SerializedName("username")
    @Expose
    var username: String? = null

    @SerializedName("uuid")
    @Expose
    var uuid: String? = null

    @SerializedName("body_position")
    @Expose
    var bodyPosition: String? = null

    @SerializedName("device_position")
    @Expose
    var devicePosition: String? = null

    @SerializedName("temp_value")
    @Expose
    var tempValue: String? = null

    @SerializedName("sensor_type")
    @Expose
    var sensorType // always "1"
            : String? = null

    @SerializedName("mac_address")
    @Expose
    var macAddress: String? = null

    @SerializedName("iosBatteryLevel")
    @Expose
    var iosBatteryLevel: String? = null

    @SerializedName("batterylevel")
    @Expose
    var batteryLevel: String? = null

    @SerializedName("timestamp")
    @Expose
    var timeStamp: String? = null

    @SerializedName("lat")
    @Expose
    var latitude: String? = null

    @SerializedName("lng")
    @Expose
    var longitude: String? = null
}