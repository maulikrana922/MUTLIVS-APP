package com.es.multivs.presentation.view.viewmodels

import android.app.Application
import android.bluetooth.BluetoothDevice
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.es.multivs.data.bledevices.oximeter.OximeterListener
import com.es.multivs.data.bledevices.oximeter.OximeterManager
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

/**
 * Created by Marko on 11/2/2021.
 * Etrog Systems LTD.
 */
@HiltViewModel
class OximeterViewModel @Inject constructor(
    application: Application
) : AndroidViewModel(application) {

    private var oximeterManager: OximeterManager? = null

    private val _spO2 = MutableLiveData<Int>()
    val spO2: LiveData<Int> = _spO2

    private val _pulseRate = MutableLiveData<Int>()
    val pulseRate: LiveData<Int> = _pulseRate

    private val _isOximeterConnected = MutableLiveData<Boolean>()
    val isOximeterConnected: LiveData<Boolean> = _isOximeterConnected

    fun connectDevice(bluetoothDevice: BluetoothDevice) {
        oximeterManager?.connectDevice(bluetoothDevice)
    }

    fun initializeDevice() {
        val listener: OximeterListener = configureDeviceListener()
        oximeterManager = OximeterManager(getApplication(), listener)
    }

    private fun configureDeviceListener(): OximeterListener {
        return object : OximeterListener {
            override fun onDataReceived(spo2: Int, pulseRate: Int) {
                if (spo2 <= 100 && pulseRate < 200) {
                    _spO2.postValue(spo2)
                    _pulseRate.postValue(pulseRate)
                }
            }

            override fun onOximeterConnected(isConnected: Boolean) {
                _isOximeterConnected.postValue(isConnected)
            }

            override fun onScanningForOximeter(isScanning: Boolean) {
            }

            override fun onScanFailed(message: String?) {
                //TODO handle this error
            }
        }
    }

    fun closeOximeterDataParser() {
        if (oximeterManager != null) {
            oximeterManager?.closeDataParser()
        }
    }

    fun closeDevice() {
        if (oximeterManager != null) {
            oximeterManager?.closeDataParser()
            oximeterManager?.closeBluetoothGatt()
            oximeterManager = null
            _isOximeterConnected.value = false
        }
    }
}