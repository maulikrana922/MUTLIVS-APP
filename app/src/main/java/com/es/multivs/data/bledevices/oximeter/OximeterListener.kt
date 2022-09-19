package com.es.multivs.data.bledevices.oximeter

/**
 * Created by Marko on 11/2/2021.
 * Etrog Systems LTD.
 */
interface OximeterListener {
    fun onDataReceived(spo2: Int, pulseRate: Int)

    fun onOximeterConnected(isConnected: Boolean)

    fun onScanningForOximeter(isScanning: Boolean)

    fun onScanFailed(message: String?)
}