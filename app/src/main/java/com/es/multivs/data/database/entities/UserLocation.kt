package com.es.multivs.data.database.entities

import androidx.room.PrimaryKey
import androidx.room.ColumnInfo
import androidx.room.Entity

@Entity
data class UserLocation(
    @PrimaryKey(autoGenerate = true)
    var id: Int,

    @ColumnInfo(name = "latitude")
    var latitude: Double,

    @ColumnInfo(name = "longitude")
    var longitude: Double
)