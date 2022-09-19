package com.es.multivs.data.network

import com.google.gson.annotations.SerializedName

data class GenericResponse<T>(@SerializedName("data") var data: T?, var status: Boolean, var message: String?)