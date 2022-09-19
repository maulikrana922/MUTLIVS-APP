package com.es.multivs.data.database.sets

import androidx.room.ColumnInfo

/*
* created by marko
Etrog Systems LTD. 26/7/2021.
*/ /**
 * a class to hold the blood pressure data fetched from the app's database.
 * the heart rate data is shared between the blood pressure result and the oximeter result.
 */
class BpCuffResults {
    @ColumnInfo(name = "bp_cuff_sys")
    var sys = 0

    @ColumnInfo(name = "bp_cuff_dia")
    var dia = 0

    @ColumnInfo(name = "heart_rate")
    var heartRate = 0
}