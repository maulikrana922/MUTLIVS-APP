package com.es.multivs.data.bledevices.weightscale

interface WeightScaleListener {
    fun onWeightScaleDataReceived(value: Double)
    fun onWeightScaleConnected(isReady: Boolean)
}