package com.es.multivs.data.network.netmodels

import com.google.gson.annotations.SerializedName

/**
 * Created by Marko on 12/2/2021.
 * Etrog Systems LTD.
 */
data class MeasurementsPostAnswer(
    @SerializedName("status")
    var status:Boolean,

    @SerializedName("message")
    var message:String,
)