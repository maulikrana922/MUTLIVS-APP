package com.es.multivs.data.database.entities

import androidx.room.PrimaryKey
import androidx.room.ColumnInfo
import androidx.room.Entity

/**
 * Device and user information
 */
@Entity
data class GatewayData(
    @PrimaryKey(autoGenerate = true)
    val id: Int,

    @ColumnInfo(name = "gateway_battery")
    var gatewayBattery: Int = 0,

    @ColumnInfo(name = "username")
    var username: String? = "",

    @ColumnInfo(name = "password")
    var password: String? = "",

    @ColumnInfo(name = "mac_address")
    var androidId: String? = "",

    @ColumnInfo(name = "is_manual")
    var manual: Int? = 0,

    @ColumnInfo(name = "build")
    var build: String? = "",

    @ColumnInfo(name = "version")
    var version: String? = "",

    @ColumnInfo(name = "country")
    var country: String? = "",

    @ColumnInfo(name = "base_url")
    var baseURL: String? = "",

    @ColumnInfo(name = "user_id")
    val userID: Int = -1,

    @ColumnInfo(name = "last_checked_date")
    var lastCheckedDate: String = "",

    @ColumnInfo(name = "identifier")
    var identifier: String = "",

    @ColumnInfo(name = "post_frequency")
    var postFrequency: Int = 0


)
