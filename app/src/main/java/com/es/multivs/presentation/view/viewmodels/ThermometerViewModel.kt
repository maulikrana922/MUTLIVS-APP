package com.es.multivs.presentation.view.viewmodels

import android.app.Application
import android.bluetooth.BluetoothDevice
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.es.multivs.data.bledevices.thermometer.ThermometerListener
import com.es.multivs.data.bledevices.thermometer.ThermometerManager
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

@HiltViewModel
class ThermometerViewModel @Inject constructor(
    application: Application
): AndroidViewModel(application) {

    private var thermometerManager: ThermometerManager? = ThermometerManager(application)

    private val _thermometerTemperature = MutableLiveData<Float>()
    var thermometerTemperature: LiveData<Float> = _thermometerTemperature

    private val _isThermometerConnected = MutableLiveData<Boolean>()
    var isThermometerConnected: LiveData<Boolean> = _isThermometerConnected

    private val _thermometerConnecting = MutableLiveData<Boolean>()
    var isThermometerConnecting: LiveData<Boolean> = _thermometerConnecting

    private var listener: ThermometerListener? = null

    fun setUpListener(){
        thermometerManager?.setListener(configureDeviceListener())
    }

    private fun configureDeviceListener(): ThermometerListener {
        return object : ThermometerListener {
            override fun onThermometerDataReceived(value: Float) {
                _thermometerTemperature.postValue(value)
            }

            override fun onThermometerConnected(isReady: Boolean) {
                _isThermometerConnected.postValue(isReady)
            }

            override fun onThermometerConnecting() {
                _thermometerConnecting.postValue(true)
            }
        }
    }

    fun connectDevice(bluetoothDevice: BluetoothDevice) {
        thermometerManager?.connectDevice(bluetoothDevice)
    }

    fun closeDevice() {
        _thermometerConnecting.value = false
        _isThermometerConnected.value = false
        thermometerManager?.stopScan()
        thermometerManager?.closeBluetoothGatt()

        listener?.let {
            listener = null
        }
    }

    override fun onCleared() {
        super.onCleared()
        thermometerManager?.let {
            thermometerManager = null
        }
    }
}