package com.es.multivs.data.database.sets

import androidx.room.ColumnInfo

/*
Etrog Systems LTD. 26/7/2021.
*/   class TestType {
    @ColumnInfo(name = "is_ecg_checked")
    var isECGChecked = false

    @ColumnInfo(name = "is_ppg_checked")
    var isPPGChecked = false
}