package com.es.multivs.data.database.sets

import androidx.room.ColumnInfo

/*
* created by marko
Etrog Systems LTD. 25/7/2021.
*/ /**
 * a class to hold the location data fetched from the app's database
 */
class LastLocation {
    @ColumnInfo(name = "latitude")
    var latitude: Double? = null

    @ColumnInfo(name = "longitude")
    var longitude: Double? = null
}