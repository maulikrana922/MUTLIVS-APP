package com.es.multivs.data.bledevices.thermometer

interface ThermometerListener {

    fun onThermometerDataReceived(value: Float)

    fun onThermometerConnected(isReady: Boolean)

    fun onThermometerConnecting()
}