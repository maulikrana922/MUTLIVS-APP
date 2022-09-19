package com.es.multivs.data.database.sets

import androidx.room.ColumnInfo

/*
* created by marko
Etrog Systems LTD. 27/7/2021.
*/ /**
 * a class to hold the glucose data fetched from the app's database
 */
class GlucoseData {
    @ColumnInfo(name = "glucose_level")
    var glucose = 0

    @ColumnInfo(name = "glucose_event_tag_index")
    var eventTagIndex = 0
}