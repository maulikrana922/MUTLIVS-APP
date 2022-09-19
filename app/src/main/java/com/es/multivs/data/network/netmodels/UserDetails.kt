package com.es.multivs.data.network.netmodels

import com.google.gson.annotations.SerializedName

class UserDetails(status: String?, userInfo: UserInfo?) {

    @SerializedName("status")
    lateinit var status: String

    @SerializedName("userInfo")
    var userInfo: UserInfo? = null

    class UserInfo {

        @SerializedName("is_manual")
        var isManual = 0

        @SerializedName("name")
        private var userName: String? = null

        @SerializedName("gender")
        private var gender: String? = null

        @SerializedName("age")
        private var age: String? = null

        @SerializedName("user_id")
        val userID = 0
    }

    init {
        if (status != null) {
            this.status = status
        }
        if (userInfo != null) {
            this.userInfo = userInfo
        }
    }
}