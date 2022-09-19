package com.es.multivs.data.database.sets

import androidx.room.ColumnInfo

/*
* created by marko
Etrog Systems LTD. 26/7/2021.
*/ /**
 * a class to hold the oximeter data fetched from the app's database
 */
class OximeterResults {
    @ColumnInfo(name = "oximeter_spo2")
    var spo2 = 0

    @ColumnInfo(name = "heart_rate")
    var heartRate = 0
}