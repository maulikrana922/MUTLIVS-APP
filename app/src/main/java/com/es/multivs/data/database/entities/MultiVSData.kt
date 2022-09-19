package com.es.multivs.data.database.entities

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Created by Marko on 10/19/2021.
 * Etrog Systems LTD.
 */
@Entity
data class MultiVSData(

    @PrimaryKey(autoGenerate = true)
    var id: Int,

    @ColumnInfo(name = "is_ecg_checked")
    var isECGChecked: Boolean = false,

    @ColumnInfo(name = "is_ppg_checked")
    var isPPGChecked: Boolean = false,

    @ColumnInfo(name = "is_heart_rate_checked")
    var isHeartRateChecked: Boolean = false,

    @ColumnInfo(name = "is_resp_checked")
    var isRespChecked: Boolean = false,

    @ColumnInfo(name = "is_temperature_checked")
    var isTemperatureChecked: Boolean = false,

    @ColumnInfo(name = "is_steps_checked")
    var isStepsChecked: Boolean = false,

    @ColumnInfo(name = "test_type")
    var testType: String? = null,

    @ColumnInfo(name = "bad_data_detection")
    var badDataDetection: String? = null,

    @ColumnInfo(name = "calibration_count")
    var calibrationCount: Int = 0,

    @ColumnInfo(name = "measurement_device_position")
    var measurementDevicePosition: String? = null,

    @ColumnInfo(name = "calibration_device_position")
    var calibrationDevicePosition: String? = null,
)