package com.es.multivs.data.network.netmodels

import com.google.gson.annotations.Expose
import com.google.gson.annotations.SerializedName

/**
 * This model will be sent to the server.
 */
class SerializedResults {
    @SerializedName("username") // check
    @Expose
    private var userName: String? = null

    @SerializedName("uuid") // check
    @Expose
    private var uuid: String? = null

    @SerializedName("macaddress") //check
    @Expose
    private var macAddress: String? = null

    @SerializedName("timestamp") //check
    @Expose
    private var timeStamp: String? = null

    @SerializedName("lng")
    @Expose
    private var longitude: String? = null

    @SerializedName("lat")
    @Expose
    private var latitude: String? = null

    @SerializedName("heart_value") // check
    @Expose
    private var heartValue: String? = null

    // Thermometer measurement
    @SerializedName("temp_value") // check
    @Expose
    private var tempValue: String? = null

    // BLT measurement
    @SerializedName("bp_sys") //check
    @Expose
    private var sys: String? = null

    // BLT measurement
    @SerializedName("bp_dia") //check
    @Expose
    private var dia: String? = null

    // Weight scale measurement
    @SerializedName("weight") //check
    @Expose
    private var weight: String? = null

    // Oximeter spo2
    @SerializedName("spo2") // check
    @Expose
    private var spo2: String? = null

    // Oximeter heart rate measurement
    @SerializedName("baseHR")
    @Expose
    private var baseHR: String? = null

    // patch measurement related
    @SerializedName("data")
    @Expose
    private var data: String? = null

    // patch measurement related
    // testType + username + "_" + new SimpleDateFormat("yyyy-MM-dd_HH-mm-ss", Locale.US).format(Calendar.getInstance().getTime()) + ".txt";
    @SerializedName("filename")
    @Expose
    private var filename: String? = null

    // Glucose measurement
    @SerializedName("blood_glucose") // check
    @Expose
    private var glucose: String? = null

    // Glucose measurement event tag
    @SerializedName("event_tag")
    @Expose
    private var eventTag: String? = null

    @SerializedName("iosBatteryLevel")
    @Expose
    private var batteryLevel: String? = null

    @SerializedName("is_manual_entry")
    @Expose
    private var manualEntry: Int? = null

    override fun toString(): String {
        return """
            user name: $userName
            MAC: $macAddress
            Time Stamp: $timeStamp
            location: $longitude, $latitude
            last heart rate: $heartValue
            temperature: $tempValue
            systolic: $sys, diastolic: $dia
            weight: $weight
            spo2: $spo2
            Glucose Data: $glucose, $eventTag
            """.trimIndent()
    }

    fun setUserName(userName: String?) {
        this.userName = userName
    }

    fun setMacAddress(macAddress: String?) {
        this.macAddress = macAddress
    }

    fun setTimeStamp(timeStamp: String?) {
        this.timeStamp = timeStamp
    }

    fun setLongitude(longitude: String?) {
        this.longitude = longitude
    }

    fun setLatitude(latitude: String?) {
        this.latitude = latitude
    }

    fun setHeartValue(heartValue: String?) {
        this.heartValue = heartValue
    }

    fun setTempValue(tempValue: String?) {
        this.tempValue = tempValue
    }

    fun setSys(sys: String?) {
        this.sys = sys
    }

    fun setDia(dia: String?) {
        this.dia = dia
    }

    fun setWeight(weight: String?) {
        this.weight = weight
    }

    fun setSpo2(spo2: String?) {
        this.spo2 = spo2
    }

    fun setBaseHR(baseHR: String?) {
        this.baseHR = baseHR
    }

    fun setData(data: String?) {
        this.data = data
    }

    fun setFilename(filename: String?) {
        this.filename = filename
    }

    fun setGlucose(glucose: String?) {
        this.glucose = glucose
    }

    fun setEventTag(eventTag: String?) {
        this.eventTag = eventTag
    }

    fun setUuid(uuid: String?) {
        this.uuid = uuid
    }

    fun setBatteryLevel(batteryLevel: String?) {
        this.batteryLevel = batteryLevel
    }

    fun setIsManualEntry(is_manual_entry:Int){
        this.manualEntry = is_manual_entry
    }
}