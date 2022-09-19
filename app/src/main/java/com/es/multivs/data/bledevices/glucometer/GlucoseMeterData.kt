package com.es.multivs.data.bledevices.glucometer

import java.sql.Timestamp

/**
 * Created by Marko on 11/8/2021.
 * Etrog Systems LTD.
 */
class GlucoseMeterData(var glucoseInMgDl: String?, var timestamp: Timestamp?) {

    constructor() : this(null, null)

//    var glucoseInMgDl: String? = null
//    var timestamp: Timestamp
    var sequenceNumber = 0
    var recordList: List<GlucoseMeterData>? = null
    var recordContextList: List<GlucoseMeterData>? = null


//    constructor(glucoseInMgDl: String, timestamp: Timestamp) : this() {
//        this.timestamp = timestamp
//    }
//
//    var glucoseInMgDl: String = glucoseInMgDl
//    lateinit var timestamp: Timestamp
//    var sequenceNumber = 0
//    var recordList: List<GlucoseMeterData>? = null
//    var recordContextList: List<GlucoseMeterData>? = null
}