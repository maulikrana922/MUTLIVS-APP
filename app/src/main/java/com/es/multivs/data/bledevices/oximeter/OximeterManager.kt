package com.es.multivs.data.bledevices.oximeter

import android.bluetooth.*
import android.content.Context
import android.os.Handler
import android.os.Looper

/**
 * Created by Marko on 11/2/2021.
 * Etrog Systems LTD.
 */
class OximeterManager(
    private val context: Context,
    private var listener: OximeterListener?
) {

    private var mDataParser: DataParser? = null

    private val _handler = Handler(Looper.getMainLooper())
    private var mBluetoothGatt: BluetoothGatt? = null
    private var mGattCallback: BluetoothGattCallback? = null
    private var mBluetoothGattService: BluetoothGattService? = null

    init {
        setGattCallback()
    }

    fun initDataParser() {
        mDataParser = DataParser(object : DataParser.onPackageReceivedListener {
            override fun onOxiParamsChanged(oxiParams: DataParser.OxiParams) {
                _handler.post {
                    if (listener != null) {
                        listener!!.onDataReceived(oxiParams.spo2, oxiParams.pulseRate)
                    }
                }
            }

            override fun onPlethWaveReceived(amp: Int) {}

        })

        mDataParser?.start()
    }

    private fun setGattCallback() {
        mGattCallback = object : BluetoothGattCallback() {
            override fun onConnectionStateChange(gatt: BluetoothGatt, status: Int, newState: Int) {
                super.onConnectionStateChange(gatt, status, newState)
                if (newState == BluetoothProfile.STATE_CONNECTED) {
                    gatt.discoverServices()
                }
                if (newState == BluetoothProfile.STATE_DISCONNECTED) {
                    _handler.post { listener!!.onOximeterConnected(false) }
                }
            }

            override fun onServicesDiscovered(gatt: BluetoothGatt, status: Int) {
                super.onServicesDiscovered(gatt, status)
                _handler.post {
                    initDataParser()
                    listener!!.onOximeterConnected(true)
                }
                mBluetoothGattService = gatt.getService(OxiConstants.UUID_SERVICE_DATA)
                val characteristic =
                    mBluetoothGattService?.getCharacteristic(OxiConstants.UUID_CHARACTER_RECEIVE)
                gatt.setCharacteristicNotification(characteristic, true)
                val descriptor =
                    characteristic?.getDescriptor(OxiConstants.UUID_CLIENT_CHARACTER_CONFIG)
                descriptor?.value = BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE
                gatt.writeDescriptor(descriptor)
            }

            override fun onCharacteristicRead(
                gatt: BluetoothGatt,
                characteristic: BluetoothGattCharacteristic,
                status: Int
            ) {
                super.onCharacteristicRead(gatt, characteristic, status)
            }

            override fun onCharacteristicChanged(
                gatt: BluetoothGatt,
                characteristic: BluetoothGattCharacteristic
            ) {
                super.onCharacteristicChanged(gatt, characteristic)
                mDataParser?.add(characteristic.value)
            }
        }
    }

    fun closeDataParser() {
        if (mDataParser != null) {
            mDataParser?.stop()
        }
    }

    fun closeBluetoothGatt() {
        if (mBluetoothGatt != null) {
            mBluetoothGatt?.disconnect()
            mBluetoothGatt?.close()
            mBluetoothGatt = null
        }

        listener = null
        mBluetoothGattService = null
    }

    fun connectDevice(bluetoothDevice: BluetoothDevice) {
        Handler(Looper.getMainLooper()).post {
            mBluetoothGatt = bluetoothDevice.connectGatt(context, false, mGattCallback)
        }
    }
}