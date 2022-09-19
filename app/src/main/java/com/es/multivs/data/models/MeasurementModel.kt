package com.es.multivs.data.models

import com.es.multivs.data.bledevices.BleDeviceTypes
import com.es.multivs.data.network.netmodels.UserSchedule
import java.util.*

/**
 * This class will be used to store the measurement schedules received from the server
 */
class MeasurementModel(val error: String? = null) {


    private val map: HashMap<String, ArrayList<String>> = HashMap()
    fun getMap() = map

    /**
     * A [UserSchedule] object that stores the fetched measurement schedules from the server.
     * Data stored in this object will be populated in the [.mMap] Hashmap object.
     */
    var userSchedule = UserSchedule()

    var calibrationSchedule = CalibrationSchedule()

    var frequency = 0

    /**
     * Function that is responsible of populating the [.mMap] Hashmap object.
     * Data will be stored in the [.mMap] Hashmap if the time stamp key is valid.
     *
     * @param measurementTitle The device's name in the measurement.
     * @param args             An array of time stamps.
     */
    private fun insertScheduleEntry(measurementTitle: String, vararg args: String) {
        for (timeStamp in args) {
            if (timeStamp.isNotEmpty()) {
                if (map[timeStamp] == null) {
                    map[timeStamp] = ArrayList()
                }

                map[timeStamp]!!.add(measurementTitle)
            }
        }
    }

    /**
     * Function that is responsible for calling the [.insertScheduleEntry] function
     * with the relevant data type.
     *
     * @param userSchedule [UserSchedule] object
     * @param dataType     String representation of the data type.
     */
    fun getMultiVSTimeStamps(userSchedule: UserSchedule, dataType: String?) {
        val t1: String
        val t2: String
        val t3: String
        val t4: String
        when (dataType) {
            BLOOD_PRESSURE_TYPE_DATA -> {
                t1 = userSchedule.ecgData!!.timeOfMeasurement1
                t2 = userSchedule.ecgData!!.timeOfMeasurement2
                t3 = userSchedule.ecgData!!.timeOfMeasurement3
                t4 = userSchedule.ecgData!!.timeOfMeasurement4

                insertScheduleEntry("MULTIVS - Blood Pressure", t1, t2, t3, t4)
            }
            ECG_TYPE_DATA -> {

                t1 = userSchedule.ecgData!!.timeOfMeasurement1
                t2 = userSchedule.ecgData!!.timeOfMeasurement2
                t3 = userSchedule.ecgData!!.timeOfMeasurement3
                t4 = userSchedule.ecgData!!.timeOfMeasurement4

                insertScheduleEntry("MULTIVS - ECG", t1, t2, t3, t4)
            }
            PPG_TYPE_DATA -> {
                t1 = userSchedule.ppgData!!.timeOfMeasurement1
                t2 = userSchedule.ppgData!!.timeOfMeasurement2
                t3 = userSchedule.ppgData!!.timeOfMeasurement3
                t4 = userSchedule.ppgData!!.timeOfMeasurement4

                insertScheduleEntry("MULTIVS - PPG", t1, t2, t3, t4)
            }
            HEART_RATE_TYPE_DATA -> {
                t1 = userSchedule.hearRateData!!.timeOfMeasurement1
                t2 = userSchedule.hearRateData.timeOfMeasurement2
                t3 = userSchedule.hearRateData.timeOfMeasurement3
                t4 = userSchedule.hearRateData.timeOfMeasurement4

                insertScheduleEntry("MULTIVS - Heart Rate", t1, t2, t3, t4)
            }
            RESPIRATION_TYPE_DATA -> {
                t1 = userSchedule.respirationData!!.timeOfMeasurement1
                t2 = userSchedule.respirationData.timeOfMeasurement2
                t3 = userSchedule.respirationData.timeOfMeasurement3
                t4 = userSchedule.respirationData.timeOfMeasurement4

                insertScheduleEntry("MULTIVS - Respiratory", t1, t2, t3, t4)
            }
            TEMPERATURE_TYPE_DATA -> {
                t1 = userSchedule.temperatureData!!.timeOfMeasurement1
                t2 = userSchedule.temperatureData.timeOfMeasurement2
                t3 = userSchedule.temperatureData.timeOfMeasurement3
                t4 = userSchedule.temperatureData.timeOfMeasurement4

                insertScheduleEntry("MULTIVS - Temperature", t1, t2, t3, t4)
            }
            STEPS_TYPE_DATA -> {
                t1 = userSchedule.stepsData!!.timeOfMeasurement1
                t2 = userSchedule.stepsData.timeOfMeasurement2
                t3 = userSchedule.stepsData.timeOfMeasurement3
                t4 = userSchedule.stepsData.timeOfMeasurement4

                insertScheduleEntry("MULTIVS - Steps", t1, t2, t3, t4)
            }
        }
    }

    /**
     * Function to add MultiVS schedules to the [.mMap] Hashmap object
     *
     * @param userSchedule [UserSchedule] object
     */
    fun addPatchSchedule(userSchedule: UserSchedule) {
//        this.userSchedule = userSchedule
        userSchedule.let {
            if (it.ecgData.isChecked && it.ppgData.isChecked) {
                getMultiVSTimeStamps(userSchedule, BLOOD_PRESSURE_TYPE_DATA)
            } else if (it.ecgData.isChecked) {

                getMultiVSTimeStamps(userSchedule, ECG_TYPE_DATA)
            } else if (it.ppgData.isChecked) {

                getMultiVSTimeStamps(userSchedule, PPG_TYPE_DATA)
            }
            if (it.hearRateData.isChecked) {

                getMultiVSTimeStamps(userSchedule, HEART_RATE_TYPE_DATA)
            }
            if (it.respirationData.isChecked) {

                getMultiVSTimeStamps(userSchedule, RESPIRATION_TYPE_DATA)
            }
            if (it.temperatureData.isChecked) {

                getMultiVSTimeStamps(userSchedule, TEMPERATURE_TYPE_DATA)
            }
            if (it.stepsData.isChecked) {

                getMultiVSTimeStamps(userSchedule, STEPS_TYPE_DATA)
            }
        }
    }

    /**
     * Function to add none MultiVS schedules to the [.mMap] Hashmap object
     *
     * @param userSchedule [UserSchedule] object
     */
    fun addNonPatchSchedule(userSchedule: UserSchedule) {
        val t1: String = userSchedule.timeOfMeasurement1
        val t2: String = userSchedule.timeOfMeasurement2
        val t3: String = userSchedule.timeOfMeasurement3
        val t4: String = userSchedule.timeOfMeasurement4
        val deviceName: String = BleDeviceTypes.getDeviceName(userSchedule.deviceType)
        if (deviceName != "") {
            insertScheduleEntry(userSchedule.deviceType, t1, t2, t3, t4)
        }

    }

    companion object {
        private const val BLOOD_PRESSURE_TYPE_DATA = "blood_pressure_type_data"
        private const val ECG_TYPE_DATA = "ecg_type_data"
        private const val PPG_TYPE_DATA = "ppg_type_data"
        private const val HEART_RATE_TYPE_DATA = "heart_rate_type_data"
        private const val RESPIRATION_TYPE_DATA = "respiration_type_data"
        private const val TEMPERATURE_TYPE_DATA = "temperature_type_data"
        private const val STEPS_TYPE_DATA = "steps_type_data"
    }

}