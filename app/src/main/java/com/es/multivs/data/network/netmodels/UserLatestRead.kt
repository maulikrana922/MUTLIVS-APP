package com.es.multivs.data.network.netmodels

import com.google.gson.annotations.SerializedName

class UserLatestRead {


    var isEcgOn = false

    @SerializedName("status")
    var isStatus = false

    @SerializedName("message")
    val message = Message()

    class Message {
        @SerializedName("heart_beat")
        var heartBeat = 0

        @SerializedName("bp_systolic")
        var bpSystolic: String? = null

        @SerializedName("bp_diastolic")
        var bpDiastolic: String? = null

        @SerializedName("respiratory_rate")
        var respiratoryRate: String? = null

        @SerializedName("rrt")
        var respiratorRateT = 0

        @SerializedName("body_temperature")
        var temperature: String? = null
    }
}