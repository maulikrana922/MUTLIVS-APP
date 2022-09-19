package com.es.multivs.presentation.view.viewmodels

import android.app.Application
import android.bluetooth.BluetoothDevice
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.es.multivs.data.bledevices.weightscale.WeightScaleListener
import com.es.multivs.data.bledevices.weightscale.WeightScaleManager
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

@HiltViewModel
class WeightScaleViewModel @Inject constructor(
    application: Application
) : AndroidViewModel(application) {

    companion object {
        const val TAG = "weightscalecheck"
    }

    private var weightScaleManager: WeightScaleManager? = null

    private val _weight = MutableLiveData<Double>()
    var weight: LiveData<Double> = _weight

    private val _isWeightScaleConnected = MutableLiveData<Boolean>()
    var isWeightScaleConnected: LiveData<Boolean> = _isWeightScaleConnected

    init {
        initDevice()
        weightScaleManager = WeightScaleManager(application, configureDeviceListener())
    }

    private fun initDevice() {
        val listener: WeightScaleListener = configureDeviceListener()
    }

    fun connectDevice(device: BluetoothDevice) {
        weightScaleManager?.connectDevice(device)
    }

    fun closeDevice() {
        weightScaleManager?.closeDevice()
    }

    private fun configureDeviceListener(): WeightScaleListener {
        return object : WeightScaleListener {
            override fun onWeightScaleDataReceived(value: Double) {
                _weight.postValue(value)
            }

            override fun onWeightScaleConnected(isReady: Boolean) {
                _isWeightScaleConnected.postValue(isReady)
            }
        }
    }
}