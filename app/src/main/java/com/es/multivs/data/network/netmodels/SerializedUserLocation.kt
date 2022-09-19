package com.es.multivs.data.network.netmodels

import com.google.gson.annotations.Expose
import com.google.gson.annotations.SerializedName

/**
 * Created by marko on 7/26/2021.
 */
class SerializedUserLocation {
    @SerializedName("username")
    @Expose
    private var userName: String? = null

    @SerializedName("uuid")
    @Expose
    private var uuid: String? = null

    @SerializedName("iosBatteryLevel")
    @Expose
    private var batteryLevel: String? = null

    @SerializedName("timestamp")
    @Expose
    private var timeStamp: String? = null

    @SerializedName("lat")
    @Expose
    private var latitude: String? = null

    @SerializedName("lng")
    @Expose
    private var longitude: String? = null
    fun setUserName(userName: String?) {
        this.userName = userName
    }

    fun setUuid(uuid: String?) {
        this.uuid = uuid
    }

    fun setBatteryLevel(batteryLevel: String?) {
        this.batteryLevel = batteryLevel
    }

    fun setTimeStamp(timeStamp: String?) {
        this.timeStamp = timeStamp
    }

    fun setLatitude(latitude: String?) {
        this.latitude = latitude
    }

    fun setLongitude(longitude: String?) {
        this.longitude = longitude
    }
}