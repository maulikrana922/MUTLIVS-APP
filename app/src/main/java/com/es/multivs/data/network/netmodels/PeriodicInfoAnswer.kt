package com.es.multivs.data.network.netmodels

import com.google.gson.annotations.SerializedName

/**
 * Created by Marko on 11/24/2021.
 * Etrog Systems LTD.
 */
data class PeriodicInfoAnswer(
    @SerializedName("status")
    var status:Boolean,

    @SerializedName("message")
    var message:String,
)