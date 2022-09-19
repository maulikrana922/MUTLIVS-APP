package com.es.multivs.data.utils

import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothManager
import android.bluetooth.le.ScanCallback
import android.bluetooth.le.ScanFilter
import android.bluetooth.le.ScanResult
import android.bluetooth.le.ScanSettings
import android.content.Context
import android.os.Handler
import android.os.Looper
import kotlinx.coroutines.*
import java.util.*
import kotlin.coroutines.CoroutineContext

/**
 * Created by Marko on 10/21/2021.
 * Etrog Systems LTD.
 */
class BluetoothTools(context: Context): CoroutineScope {

    interface BluetoothToolsListener {
        fun onDeviceDetected(device: BluetoothDevice)
        fun onScanStarted()
        fun onScanStopped()
        fun onScanTimeout()
    }

    private var _duration: Long = 20

    private var _callback: BluetoothToolsListener? = null
    private var deviceMacAddress: String = ""
    private var bluetoothManager: BluetoothManager? =
        context.getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager

    private var _handler = Handler(Looper.getMainLooper())

    private var stopped = false

    fun setMacAddressToSearch(mac: String) {
        this.deviceMacAddress = mac
    }

    fun startScan(duration: Long) {
        _duration = duration
        stopped = false
        _callback?.onScanStarted()
        val builder = ScanFilter.Builder()
        val emptyFilter = builder.build()
        val filters: MutableList<ScanFilter> = ArrayList()
        filters.add(emptyFilter)
        val scanSettings: ScanSettings = buildScanSettings()

        bluetoothManager?.adapter?.let {
            if (it.bluetoothLeScanner != null && it.isEnabled) {
                _handler.postDelayed({
                    stopScanAndReScan()
                }, duration)

                it.bluetoothLeScanner.startScan(
                    filters,
                    scanSettings,
                    scanCallback
                )
            }
        }
    }

    private fun stopScanAndReScan() {
        launch {
            _callback?.onScanTimeout()
            bluetoothManager?.adapter?.bluetoothLeScanner?.stopScan(scanCallback)
            delay(500)
            startScan(_duration)
        }
    }


    fun stopScan() {
        _handler.removeCallbacksAndMessages(null)
        if (stopped) return
        stopped = true
        bluetoothManager?.adapter?.bluetoothLeScanner?.stopScan(scanCallback)
        _callback?.onScanStopped()
    }
    private val scanCallback: ScanCallback = object : ScanCallback() {
        override fun onScanResult(callbackType: Int, result: ScanResult) {
            super.onScanResult(callbackType, result)
            val discoveredMac = result.device.address
            if (discoveredMac == deviceMacAddress) {
                _handler.removeCallbacksAndMessages(null)
                _callback?.onDeviceDetected(result.device)
                stopScan()
            }
        }

        override fun onScanFailed(errorCode: Int) {
            super.onScanFailed(errorCode)
            _callback?.onScanStopped()
        }
    }

    private fun buildScanSettings(): ScanSettings {
        val builderScanSettings = ScanSettings.Builder()
        builderScanSettings.setScanMode(ScanSettings.SCAN_MODE_LOW_LATENCY)
        builderScanSettings.setReportDelay(0)
        return builderScanSettings.build()
    }

    private fun getUUID(result: ScanResult): String {
        return UUID.nameUUIDFromBytes(result.scanRecord!!.bytes).toString()
    }

    fun setCallback(callback: BluetoothToolsListener?) {
        _callback = callback
    }

    fun getDevice(deviceAddress: String): BluetoothDevice? {
        return bluetoothManager?.adapter?.getRemoteDevice(deviceAddress)
    }


    private val job = SupervisorJob()
    override val coroutineContext: CoroutineContext
        get() = job + Dispatchers.IO
}