package com.es.multivs.data.database.entities

import androidx.room.PrimaryKey
import androidx.room.ColumnInfo
import androidx.room.Entity

/*
* created by marko
Etrog Systems LTD. 20/7/2021.
*/
@Entity
class NonMultiVSResults {
    @PrimaryKey(autoGenerate = true)
    var id = 0

    @ColumnInfo(name = "thermometer_temperature")
    var thermometerTemperature: String? = null

    @ColumnInfo(name = "weight_scale")
    var weight: String? = ""

    @ColumnInfo(name = "bp_cuff_dia")
    var bpCuffDia = 0

    @ColumnInfo(name = "bp_cuff_sys")
    var bpCuffSys = 0

    @ColumnInfo(name = "heart_rate")
    var heartRate = 0

    @ColumnInfo(name = "oximeter_spo2")
    var oximeterSpo2 = 0

    @ColumnInfo(name = "glucose_level")
    var glucoseLevel = 0

    @ColumnInfo(name = "glucose_event_tag_index")
    var glucoseEventIndex = 0

    @ColumnInfo(name = "active_measurement")
    var activeMeasurement = ""

}