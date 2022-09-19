package com.es.multivs.data.utils

import com.es.multivs.BuildConfig

object Constants {

    const val BASE_URL = BuildConfig.SERVER

    const val API_SUCCESS = 200
    const val IntervalsFragment = "IntervalsFragments"
    const val MeasurementsFragment = "MeasurementsFragments"
    const val GET_USER_SCHEDULE = "getUserSchedule/"

    var batteryLevel = "0"
    var steps = "0"
    var isManualEntry: Int = 1
    var isMULTIVS = false
    var isIntervals = false
    var isIntervalsOn = false
    var deviceType: String = ""
    var interval: String = ""
    var isCheckedEcg: Boolean = false
    var isCheckedPpg: Boolean = false
    var isDisConnectDevice = false
    var bpSide: String = ""
    var isMultivs = false
    var MeasurementDuration = ""
    var CalibrationDuration = ""


    const val HEADER_CACHE_CONTROL = "Cache-Control"
    const val HEADER_PRAGMA = "Pragma"

    //SharedPreferences
    const val CalibrationSet = "set"
    const val Measurement = "measurement"
    var isTextChange = true
}