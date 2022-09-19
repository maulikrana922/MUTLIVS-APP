package com.es.multivs.data.bledevices

import com.es.multivs.data.network.netmodels.UserDevices
import java.util.ArrayList
import java.util.HashMap


/*
* created by Marko
Etrog Systems LTD. 31/8/2021.
*/
class BleDeviceTypes {

    companion object {

        private  var deviceMap: HashMap<String, String> = HashMap()
        private  var deviceOrder: ArrayList<String> = ArrayList()

        const val ES_008 = "ES-008" // Chest Patch
        const val ES_009 = "ES-009" // Chest Strap
        const val ES_020 = "ES-020" // Smart Oximeter
        const val ES_021 = "ES-021" // Smart Temp
        const val ES_022 = "ES-022" // Smart BP cuff
        const val ES_023 = "ES-023" // Smart Scale
        const val ES_024 = "ES-024" // Glucose Meter (True Air Metrix)

        const val ES_008_NAME = "MULTIVS" // Chest Patch
        const val ES_020_NAME = "Oximeter" // Smart Oximeter
        const val ES_021_NAME = "Thermometer" // Smart Temp
        const val ES_022_NAME = "Blood Pressure" // Smart BP cuff
        const val ES_023_NAME = "Weight Scale" // Smart Scale
        const val ES_024_NAME = "Glucosemeter" // Glucose Meter (True Air Metrix)

//        val instance = DeviceTypes()

        fun getDeviceName(deviceType: String): String {
            var deviceName = ""
            when (deviceType) {
                ES_008 -> deviceName = "MULTIVS"
                ES_020 -> deviceName = "Oximeter"
                ES_021 -> deviceName = "Thermometer"
                ES_022 -> deviceName = "Blood Pressure"
                ES_023 -> deviceName = "Weight Scale"
                ES_024 -> deviceName = "Glucosemeter"
            }
            return deviceName
        }

        fun setUserDevices(userDevices: UserDevices) {
            for (deviceInfo in userDevices.deviceInfoList) {
                val deviceID: String = deviceInfo.deviceID
                val mac: String = deviceInfo.deviceMAC
                processDeviceIDAndMAC(deviceID, mac)
            }
        }

        private fun processDeviceIDAndMAC(deviceID: String, mac: String) {
            when (deviceID.substring(0, 6)) {
                ES_008 -> deviceMap[ES_008] = mac
                ES_009 -> deviceMap[ES_009] = mac
                ES_020 -> deviceMap[ES_020] = mac
                ES_021 -> deviceMap[ES_021] = mac
                ES_022 -> deviceMap[ES_022] = mac
                ES_023 -> deviceMap[ES_023] = mac
                ES_024 -> deviceMap[ES_024] = mac
            }
        }

        fun getDeviceMAC(deviceID: String?): String? {
            return if (deviceMap.containsKey(deviceID)) {
                deviceMap[deviceID]
            } else ""
        }
    }

}