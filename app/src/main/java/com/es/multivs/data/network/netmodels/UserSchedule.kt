package com.es.multivs.data.network.netmodels

import com.google.gson.annotations.SerializedName

/**
 * A class that is responsible to store the measurement schedule data retrieved from the server.
 */
class UserSchedule {
    @SerializedName("deviceType")
    var deviceType: String = ""

    @SerializedName("interval")
    var interval: String = ""

    @SerializedName("timeOfMeasurement1")
    var timeOfMeasurement1: String = ""

    @SerializedName("timeOfMeasurement2")
    var timeOfMeasurement2: String = ""

    @SerializedName("timeOfMeasurement3")
    var timeOfMeasurement3: String = ""

    @SerializedName("timeOfMeasurement4")
    var timeOfMeasurement4: String = ""

    @SerializedName("is_calibration")
    var isCalibration: String = ""

    @SerializedName("is_calibration_required")
    var is_calibration_required: Boolean = false

    @SerializedName("duration")
    var duration: String = ""

    @SerializedName("ecg_data")
    var ecgData = EcgData()

    @SerializedName("ppg_data")
    var ppgData = PpgData()

    @SerializedName("heart_rate_data")
    var hearRateData = HearRateData()

    @SerializedName("respiration_data")
    var respirationData = RespirationData()

    @SerializedName("temperature_data")
    var temperatureData = TemperatureData()

    @SerializedName("steps_data")
    var stepsData = StepsData()

    @SerializedName("frequency")
    var frequency: Int = 0

    @SerializedName("device_position")
    var measurementDevicePosition: String = ""

    var calibrationDevicePosition: String = ""

//    @SerializedName("calibration_count")
//    var calibrationCount: Int = 0

    @SerializedName("bad_data_detection")
    var badDataDetection: String = ""

    @SerializedName("test_type")
    var testType: String = ""

    class EcgData {
        @SerializedName("is_checked")
        var isChecked = false

        @SerializedName("timeOfMeasurement1")
        var timeOfMeasurement1: String = ""

        @SerializedName("timeOfMeasurement2")
        var timeOfMeasurement2: String = ""

        @SerializedName("timeOfMeasurement3")
        var timeOfMeasurement3: String = ""

        @SerializedName("timeOfMeasurement4")
        var timeOfMeasurement4: String = "null"

        @SerializedName("duration")
        var duration: String? = ""
    }

    class PpgData {
        @SerializedName("is_checked")
        var isChecked = false

        @SerializedName("timeOfMeasurement1")
        var timeOfMeasurement1: String = ""

        @SerializedName("timeOfMeasurement2")
        var timeOfMeasurement2: String = ""

        @SerializedName("timeOfMeasurement3")
        var timeOfMeasurement3: String = ""

        @SerializedName("timeOfMeasurement4")
        var timeOfMeasurement4: String = ""

        @SerializedName("duration")
        var duration: String? = null
    }

    class HearRateData {
        @SerializedName("is_checked")
        var isChecked = false

        @SerializedName("timeOfMeasurement1")
        var timeOfMeasurement1: String = ""

        @SerializedName("timeOfMeasurement2")
        var timeOfMeasurement2: String = ""

        @SerializedName("timeOfMeasurement3")
        var timeOfMeasurement3: String = ""

        @SerializedName("timeOfMeasurement4")
        var timeOfMeasurement4: String = ""

        @SerializedName("duration")
        var duration: String = ""
    }

    class RespirationData {
        @SerializedName("is_checked")
        var isChecked = false

        @SerializedName("timeOfMeasurement1")
        var timeOfMeasurement1: String = ""

        @SerializedName("timeOfMeasurement2")
        var timeOfMeasurement2: String = ""

        @SerializedName("timeOfMeasurement3")
        var timeOfMeasurement3: String = ""

        @SerializedName("timeOfMeasurement4")
        var timeOfMeasurement4: String = ""

        @SerializedName("duration")
        var duration: String = ""
    }

    class TemperatureData {
        @SerializedName("is_checked")
        var isChecked = false

        @SerializedName("timeOfMeasurement1")
        var timeOfMeasurement1: String = ""

        @SerializedName("timeOfMeasurement2")
        var timeOfMeasurement2: String = ""

        @SerializedName("timeOfMeasurement3")
        var timeOfMeasurement3: String = ""

        @SerializedName("timeOfMeasurement4")
        var timeOfMeasurement4: String = ""

        @SerializedName("duration")
        var duration: String = ""
    }

    class StepsData {
        @SerializedName("is_checked")
        var isChecked = false

        @SerializedName("timeOfMeasurement1")
        var timeOfMeasurement1: String = ""

        @SerializedName("timeOfMeasurement2")
        var timeOfMeasurement2: String = ""

        @SerializedName("timeOfMeasurement3")
        var timeOfMeasurement3: String = ""

        @SerializedName("timeOfMeasurement4")
        var timeOfMeasurement4: String = ""

        @SerializedName("duration")
        var duration: String = ""
    }
}