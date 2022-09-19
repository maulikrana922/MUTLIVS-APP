package com.es.multivs.presentation.view.viewmodels

import android.app.Application
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Handler
import android.os.Looper
import android.util.Log
import androidx.lifecycle.*
import com.es.multivs.data.utils.AppUtils
import com.es.multivs.data.utils.BluetoothTools
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import java.lang.IllegalArgumentException
import javax.inject.Inject

/**
 * Created by Marko on 10/21/2021.
 * Etrog Systems LTD.
 */
@HiltViewModel
class BluetoothViewModel @Inject constructor(
    application: Application,
    private val bluetoothTools: BluetoothTools
) : AndroidViewModel(application) {

    companion object {
        const val SCAN_DURATION_MILLI = 30000L
    }

    private val _bluetoothDevice = MutableLiveData<BluetoothDevice>()

    val bluetoothDevice: LiveData<BluetoothDevice> = _bluetoothDevice

    private var _isScanning = MutableLiveData<Boolean>()
    var isScanning: LiveData<Boolean> = _isScanning
     var isMultivsConnect = MutableLiveData<Boolean>()
    private var _rescan = MutableLiveData<Boolean>()
    var rescan: LiveData<Boolean> = _rescan
    fun resetScan() {
        _rescan.postValue(false)
    }

    fun startScan(mac: String) {
        viewModelScope.launch {

            bluetoothTools.setCallback(object : BluetoothTools.BluetoothToolsListener {
                override fun onDeviceDetected(device: BluetoothDevice) {
                    _bluetoothDevice.postValue(device)
                }

                override fun onScanStarted() {
                    _isScanning.postValue(true)
                }

                override fun onScanStopped() {
                    _isScanning.postValue(false)
                }

                override fun onScanTimeout() {
                    _isScanning.postValue(false)
                    _rescan.postValue(true)
                }
            })

            /**
             * The TPS tablet fails to retrieve a bluetooth device from [BluetoothAdapter.getRemoteDevice] method.
             * It is reliable in the Samsung tablet, but for now,
             * all tablets will start a bluetooth search and then fetch the device.
             */
            if (false/*Build.VERSION.SDK_INT > Build.VERSION_CODES.P*/) {


                /**
                 * This works with the samsung tablet
                 */
                val device = bluetoothTools.getDevice(mac)
                device?.let {
                    _bluetoothDevice.postValue(it)
                }
            } else {
                /**
                 * This works with the TPS tablet
                 */
                bluetoothTools.setMacAddressToSearch(mac)
                bluetoothTools.startScan(SCAN_DURATION_MILLI)
            }
        }
    }

    private val broadcastReceiver: BroadcastReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            val action = intent!!.action

            if (action == BluetoothAdapter.ACTION_STATE_CHANGED) {
                when (intent.getIntExtra(BluetoothAdapter.EXTRA_STATE, BluetoothAdapter.ERROR)) {
                    BluetoothAdapter.STATE_OFF -> BluetoothAdapter.getDefaultAdapter().enable()
                    BluetoothAdapter.STATE_TURNING_OFF -> {
                    }
                    BluetoothAdapter.STATE_ON -> Handler(Looper.getMainLooper()).postDelayed(
                        { bluetoothTools.startScan(SCAN_DURATION_MILLI) },
                        1000
                    )
                    BluetoothAdapter.STATE_TURNING_ON -> {
                    }
                    BluetoothAdapter.ERROR -> {
                    }
                }
            }
        }
    }

    fun stopScan() {
        viewModelScope.launch {
            bluetoothTools.stopScan()
            _isScanning.postValue(false)
            try {
                getApplication<Application>().unregisterReceiver(broadcastReceiver)
            } catch (exception: IllegalArgumentException) {
                Log.e(
                    "broadcast_unregister",
                    "stopScan: bluetoothToolsViewModel: receiver wasn't registered because a scan wasn't started."
                )
            }
        }
    }

    override fun onCleared() {
        super.onCleared()
        bluetoothTools.setCallback(null)
    }
}