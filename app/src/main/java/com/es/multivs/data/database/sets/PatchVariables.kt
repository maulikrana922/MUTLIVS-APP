package com.es.multivs.data.database.sets

import androidx.room.ColumnInfo

/*
Etrog Systems LTD. 26/7/2021.
*/
class PatchVariables {

    @ColumnInfo(name = "is_ecg_checked")
    var isECGChecked = false

    @ColumnInfo(name = "is_ppg_checked")
    var isPPGChecked = false

    @ColumnInfo(name = "is_heart_rate_checked")
    var isHeartRateChecked = false

    @ColumnInfo(name = "is_resp_checked")
    var isRespChecked = false

    @ColumnInfo(name = "is_temperature_checked")
    var isTemperatureChecked = false

    @ColumnInfo(name = "is_steps_checked")
    var isStepsChecked = false
}