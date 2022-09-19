package com.es.multivs.data.database.sets

import androidx.room.ColumnInfo

/*
Etrog Systems LTD. 26/7/2021.
*/
//class UserCredentials {
//    @ColumnInfo(name = "username")
//    var username: String? = null
//
//    @ColumnInfo(name = "password")
//    var password: String? = null
//}

data class UserCredentials(
    @ColumnInfo(name = "username")
    var username: String,

    @ColumnInfo(name = "password")
    var password: String
)