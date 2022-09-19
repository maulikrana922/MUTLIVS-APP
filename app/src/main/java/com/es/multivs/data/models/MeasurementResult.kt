//package com.es.careapp.data.models
//
//import com.es.careapp.data.models.MeasurementResult
//import com.es.careapp.data.models.GlucoseData
//import java.util.HashMap
//
////TODO: all of this data should be inside the room database
//class MeasurementResult {
//    fun addThermometerResults(temperature: String) {
//        addResult(THERMOMETER_TEMPERATURE, temperature)
//    }
//
//    fun addWeightScaleResults(weight: Double) {
//        addResult(WEIGHT_SCALE, weight)
//    }
//
//    fun addBloodPressureResult(sys: Int, dia: Int, heartRate: Int) {
//        addResult(BLT_DIA, dia)
//        addResult(BLT_SYS, sys)
//        addResult(HEART_RATE, heartRate)
//    }
//
//    fun addOximeterResults(spo2: Int, heartRate: Int) {
//        addResult(OXIMETER_SPO2, spo2)
//        addResult(HEART_RATE, heartRate)
//    }
//
//    fun addGlucosemeterResults(glucose: Int, eventTag: String?) {
//        val data = GlucoseData(glucose, eventTag)
//        addResult(data)
//    }
//
//    private fun addResult(data: GlucoseData) {
//        results!![GLUCOSE] = data
//    }
//
//    fun getGlucoseData(key: String): GlucoseData? {
//        return results!![key] as GlucoseData?
//    }
//
//    private fun addResult(key: String, value: Int) {
//        results!![key] = value
//    }
//
//    private fun addResult(key: String, value: String) {
//        results!![key] = value
//    }
//
//    private fun addResult(key: String, value: Double) {
//        results!![key] = value
//    }
//
//    fun getIntegerValue(key: String): Int? {
//        return results!![key] as Int?
//    }
//
//    fun getDoubleValue(key: String): Double? {
//        return results!![key] as Double?
//    }
//
//    fun getStringValue(key: String): String? {
//        return results!![key] as String?
//    }
//
//    fun clearResults() {
//        results!!.clear()
//        measurementResult = null
//    }
//
//    companion object {
//        private var measurementResult: MeasurementResult? = null
//        var BLT_SYS = "blt_sys" // Integer
//        var BLT_DIA = "blt_dia" // Integer
//        var OXIMETER_SPO2 = "oximeter_spo2" // Integer
//        var THERMOMETER_TEMPERATURE = "thermometer_temperature" // Float
//        var WEIGHT_SCALE = "weight_scale_lbs" // Double
//        var GLUCOSE = "glucose_unit" // Object
//        var HEART_RATE = "last_heart_rate"
//        private var results: HashMap<String, Any>? = null
//        val instance: MeasurementResult?
//            get() {
//                if (results == null) {
//                    results = HashMap()
//                    measurementResult = MeasurementResult()
//                }
//                return measurementResult
//            }
//    }
//}