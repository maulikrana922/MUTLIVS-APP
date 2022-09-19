package com.es.multivs.data.bledevices.thermometer

import android.bluetooth.*
import android.bluetooth.le.ScanCallback
import android.bluetooth.le.ScanFilter
import android.bluetooth.le.ScanSettings
import android.content.Context
import android.os.Handler
import android.os.Looper
import com.es.multivs.data.bledevices.BleDeviceTypes
import java.nio.charset.StandardCharsets
import java.util.ArrayList

class ThermometerManager(private var context: Context?) {

    private var _listener: ThermometerListener? = null
    private var mBluetoothGatt: BluetoothGatt? = null
    private var mGattCallback: BluetoothGattCallback? = null
    private var scanCallback: ScanCallback? = null
    private var _handler = Handler(Looper.getMainLooper())


    private var mTemperature = 0f


    init {
        setGattCallback()
    }

    private fun setGattCallback() {
        mGattCallback = object : BluetoothGattCallback() {
            override fun onConnectionStateChange(gatt: BluetoothGatt, status: Int, newState: Int) {
                super.onConnectionStateChange(gatt, status, newState)
                if (newState == BluetoothProfile.STATE_CONNECTED) {
                    gatt.discoverServices()
                    Handler(Looper.getMainLooper()).post {
                        _listener?.onThermometerConnected(
                            true
                        )
                    }
                }
                if (newState == BluetoothProfile.STATE_DISCONNECTED) {
                    gatt.close()
                    scanForDevice()
                    Handler(Looper.getMainLooper()).post {
                        _listener?.onThermometerConnected(
                            false
                        )
                    }
                }
            }

            override fun onServicesDiscovered(gatt: BluetoothGatt, status: Int) {
                super.onServicesDiscovered(gatt, status)

                /*
                 * service UUID - cdeacb80-5235-4c07-8846-93a37ee6b86d
                 * characteristic UUID - cdeacb81-5235-4c07-8846-93a37ee6b86d
                 * descriptor UUID - 00002902-0000-1000-8000-00805f9b34fb
                 */
                val gattService = gatt.services[gatt.services.size - 1]
                //                BluetoothGattCharacteristic c = gatt
//                        .getService(UUID.fromString("cdeacb80-5235-4c07-8846-93a37ee6b86d"))
//                        .getCharacteristic(UUID.fromString("cdeacb81-5235-4c07-8846-93a37ee6b86d"));
//
//                gatt.setCharacteristicNotification(c, true);
//                BluetoothGattDescriptor descriptor = c.getDescriptor(UUID.fromString("00002902-0000-1000-8000-00805f9b34fb"));
//                descriptor.setValue(BluetoothGattDescriptor.ENABLE_INDICATION_VALUE);
//
//                //TODO: Check if no handler is needed.
//                gatt.writeDescriptor(descriptor);

                /*
                 * Traverse characteristics in the service that represents the temperature.
                 * enable notifications from characteristics with descriptors so we can get the values.
                 */
                for (characteristic1 in gattService.characteristics) {
                    if (characteristic1.descriptors.size == 0) continue
                    gatt.setCharacteristicNotification(characteristic1, true)
                    for (descriptor1 in characteristic1.descriptors) {
                        descriptor1.value = BluetoothGattDescriptor.ENABLE_INDICATION_VALUE
                        gatt.writeDescriptor(descriptor1) // this line is to tell the characteristics to send me back the data when it is changed.
                    }
                }
            }

            override fun onCharacteristicChanged(
                gatt: BluetoothGatt,
                characteristic: BluetoothGattCharacteristic
            ) {
                super.onCharacteristicChanged(gatt, characteristic)
                val result = String(characteristic.value, StandardCharsets.UTF_8)
                val c = result[result.length - 1]
                if (c == 'F') {
                    mTemperature = result.substring(0, result.length - 1).toFloat()
                }
                _handler.post {
                    _listener?.onThermometerDataReceived(
                        mTemperature
                    )
                }
            }
        }
    }

    fun scanForDevice() {
        val isDeviceFound = false
        val scanSettings: ScanSettings = buildScanSettings()
        val scanFilterList: MutableList<ScanFilter> = ArrayList()
        val scanFilter = ScanFilter.Builder()
            .setDeviceAddress(BleDeviceTypes.getDeviceMAC(BleDeviceTypes.ES_021)).build()
        scanFilterList.add(scanFilter)
    }

    private fun buildScanSettings(): ScanSettings {
        val builderScanSettings = ScanSettings.Builder()
        builderScanSettings.setScanMode(ScanSettings.SCAN_MODE_LOW_LATENCY)
        builderScanSettings.setNumOfMatches(ScanSettings.MATCH_NUM_ONE_ADVERTISEMENT)
        builderScanSettings.setCallbackType(ScanSettings.CALLBACK_TYPE_ALL_MATCHES)
        builderScanSettings.setMatchMode(ScanSettings.MATCH_MODE_AGGRESSIVE)
        builderScanSettings.setReportDelay(0)

        return builderScanSettings.build()
    }

    fun stopScan() {
        if (BluetoothAdapter.getDefaultAdapter().bluetoothLeScanner != null) {
            BluetoothAdapter.getDefaultAdapter().bluetoothLeScanner.stopScan(scanCallback)
            scanCallback = null
        }
    }

    fun closeBluetoothGatt() {

        mBluetoothGatt?.let {
            mBluetoothGatt?.disconnect()
            mBluetoothGatt?.close()
            mBluetoothGatt = null
        }

        _listener?.let {
            _listener = null
        }
    }

    /**
     * Conversion to Celsius in case the device sends the temperature in Fahrenheit format.
     *
     * @param fahrenheitValue value received from the bluetooth device.
     * @return Temperatures in Celsius
     */
    @Deprecated("not in use, ")
    private fun convertFahrenheitToCelsius(fahrenheitValue: Float): Float {
        return 5 * (mTemperature - 32.0f) / 9.0f
    }

    fun setListener(listener: ThermometerListener) {
        _listener = listener
    }

    fun connectDevice(bluetoothDevice: BluetoothDevice) {
        _handler.post {
            _listener?.onThermometerConnecting()
        }

        mBluetoothGatt = bluetoothDevice.connectGatt(context, false, mGattCallback)
    }


}