package com.es.multivs.data.bledevices.spyhmomanometer

import android.bluetooth.BluetoothDevice

/**
 * Created by Marko on 10/31/2021.
 * Etrog Systems LTD.
 */
class BleDevice {

     var bluetoothDevice: BluetoothDevice? = null
     var deviceUUID: String? = null
     var rssi = 0
     var deviceName: String? = null
     var address: String? = null
//    private val scanRecordBytes: ByteArray = scanRecord.getBytes()
     var deviceType: String? = null
}