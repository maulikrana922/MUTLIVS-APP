package com.es.multivs.data.bledevices.glucometer

import android.bluetooth.*
import android.content.Context
import android.os.Handler
import android.os.Looper
import android.util.Log
import java.lang.StringBuilder
import java.nio.ByteBuffer
import java.sql.Timestamp
import java.text.ParseException
import java.text.SimpleDateFormat
import java.util.*
import kotlin.collections.ArrayList
import kotlin.math.roundToInt

/**
 * Created by Marko on 11/8/2021.
 * Etrog Systems LTD.
 */
class GlucometerManager(private val context: Context, private var listener:GlucometerListener?) {

    companion object {
        private const val STATE_DISCONNECTED = 0
        private const val STATE_CONNECTED = 2
        private val UUID_NOTIFY = UUID.fromString("00002902-0000-1000-8000-00805f9b34fb")
        const val SEQUENCE_NUMBER_LSB_INDEX = 1
    }

    var mBluetoothGatt: BluetoothGatt? = null
    private val handler: Handler = Handler(Looper.getMainLooper())
    private var mDataList = ArrayList<GlucoseMeterData>()
    private var mIsDeviceFound = false


    fun connectDevice(bluetoothDevice: BluetoothDevice?) {
        if (bluetoothDevice != null) {
            mBluetoothGatt = bluetoothDevice.connectGatt(context, false, gattCallback)
            handler.post { listener?.onGlucometerConnecting(true) }
        } else {
            Log.e("glucosecheck", "connectDevice: bluetoothDevice is null")
        }
    }

    /**
     * This queue is for writing values into the descriptor.
     * Writings must be queued in order to be received correctly.
     */
    private val bleCommandQueue: Queue<BleCommand> = LinkedList()

    private fun nextCommandQueue() {
        val bleCommand: BleCommand = bleCommandQueue.poll() ?: return
        writeDescriptor(bleCommand.descriptor)
    }

    private fun writeDescriptor(descriptor: BluetoothGattDescriptor) {
        mBluetoothGatt!!.writeDescriptor(descriptor)
    }

    private val gattCallback: BluetoothGattCallback = object : BluetoothGattCallback() {
        override fun onConnectionStateChange(gatt: BluetoothGatt, status: Int, newState: Int) {
            super.onConnectionStateChange(gatt, status, newState)
            if (newState == STATE_CONNECTED) {
                gatt.discoverServices()
                if (listener != null) {
                    handler.post { listener?.onGlucometerConnected(true) }
                }
            } else if (newState == STATE_DISCONNECTED) {
                if (listener != null) {
                    handler.post { listener?.onGlucometerConnected(false) }
                }
            }
        }

        override fun onServicesDiscovered(gatt: BluetoothGatt, status: Int) {
            super.onServicesDiscovered(gatt, status)
            val service = gatt.getService(UUID.fromString("00001808-0000-1000-8000-00805f9b34fb"))
            if (service == null) {
            }

            // glucose measurement context
            val characteristic = Objects.requireNonNull(service)
                .getCharacteristic(UUID.fromString("00002a34-0000-1000-8000-00805f9b34fb"))
            if (characteristic == null) {
            } else {
                gatt.setCharacteristicNotification(characteristic, true)
            }
            val descriptor = Objects.requireNonNull(characteristic).getDescriptor(UUID_NOTIFY)
            if (descriptor == null) {
            } else {
                descriptor.value = BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE
            }

            //offer command
            bleCommandQueue.offer(BleCommand(descriptor!!))

            //glucose measurement
            val characteristic2 =
                service!!.getCharacteristic(UUID.fromString("00002a18-0000-1000-8000-00805f9b34fb"))
            gatt.setCharacteristicNotification(characteristic2, true)
            val descriptor2 = characteristic2.getDescriptor(UUID_NOTIFY)
            if (descriptor2 == null) {
            }
            Objects.requireNonNull(descriptor2).value =
                BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE

            // offer command
            bleCommandQueue.offer(BleCommand(descriptor2!!))
            val characteristic3 =
                service.getCharacteristic(UUID.fromString("00002a52-0000-1000-8000-00805f9b34fb"))
            gatt.setCharacteristicNotification(characteristic3, true)
            val descriptor3 = characteristic3.getDescriptor(UUID_NOTIFY)
            if (descriptor3 == null) {
            }
            Objects.requireNonNull(descriptor3).value =
                BluetoothGattDescriptor.ENABLE_INDICATION_VALUE

            // offer command
            bleCommandQueue.offer(BleCommand(descriptor3!!))

            // next command queue
            nextCommandQueue()
        }

        override fun onCharacteristicWrite(
            gatt: BluetoothGatt,
            characteristic: BluetoothGattCharacteristic,
            status: Int
        ) {
            super.onCharacteristicWrite(gatt, characteristic, status)
            val data = characteristic.value

            // print at least 2 digits, prepend it with 0's if there's less.
            if (data != null && data.size > 0) {
                val stringBuilder = StringBuilder(data.size)
                for (byteChar in data) stringBuilder.append(String.format("%02X ", byteChar))
            }
        }

        override fun onDescriptorWrite(
            gatt: BluetoothGatt,
            descriptor: BluetoothGattDescriptor,
            status: Int
        ) {
            super.onDescriptorWrite(gatt, descriptor, status)
            val writeCharRecordAccessControlPoint = gatt
                .getService(UUID.fromString("00001808-0000-1000-8000-00805f9b34fb"))
                .getCharacteristic(UUID.fromString("00002a52-0000-1000-8000-00805f9b34fb"))

            /* Get stored records count.
             Refer to "Bluetooth Low Energy Blood Glucose Profile Specification" document. */
            val data = ByteArray(2)
            data[0] = 0x04 // Report number of stored records
            data[1] = 0x01 // All records
            writeCharRecordAccessControlPoint.value = data
            gatt.writeCharacteristic(writeCharRecordAccessControlPoint)
        }

        override fun onCharacteristicChanged(
            gatt: BluetoothGatt,
            characteristic: BluetoothGattCharacteristic
        ) {
            super.onCharacteristicChanged(gatt, characteristic)
            val value = characteristic.value
            val glucoseMeterData = GlucoseMeterData()
            if (value[0].toInt() == 5) {
                val numOfRecords: Int = GlucometerUtils.byteArrayToInt(
                    byteArrayOf(
                        0, 0,
                        value[3], value[2]
                    )
                )
                getTodayLatestRecords(numOfRecords)
            }
            if (characteristic.uuid.toString().uppercase().contains("2A52")) {
                if (value[0].toInt() == 6) {
                    if (listener != null) {
                        handler.postDelayed({
                            listener?.onGlucometerDataReceived(
                                mDataList
                            )
                        }, 500)
                    }
                }
            }
            if (characteristic.uuid.toString().uppercase().contains("2A18")) {
                val parsedFloatValue = (GlucometerUtils.parseSFLOATtoDouble(
                    byteArrayOf(
                        value[11], value[10]
                    )
                ) * 100000).roundToInt().toDouble()

                val glucose = parsedFloatValue.toString()

                glucoseMeterData.glucoseInMgDl = glucose

                mDataList.add(GlucoseMeterData(glucose, getMeasurementTimestamps(value)))
            } else if (characteristic.uuid.toString().uppercase()
                    .contains("2A34")
            ) { // TODO: maybe useless
                if (value.size < 4) {
                    return
                }
                val sequenceNumber: Int = GlucometerUtils.byteArrayToInt(
                    byteArrayOf(
                        0, 0,
                        value[SEQUENCE_NUMBER_LSB_INDEX + 1], value[SEQUENCE_NUMBER_LSB_INDEX]
                    )
                )
            }
        }
    }

    /**
     * Function to get the 10 last records in the glucose meter device.
     *
     * @param count number of records
     */
    private fun getTodayLatestRecords(count: Int) {
        if (mBluetoothGatt == null) {
            return
        }
        val characteristic = mBluetoothGatt!!.services[3].characteristics[2]
        val numOfRecordsBytes = ByteArray(2)
        var sequenceNumber: Short = 1
        if (count > 10) sequenceNumber = (count - 9).toShort()
        numOfRecordsBytes[0] = (sequenceNumber.toInt() and 0xff).toByte()
        numOfRecordsBytes[1] = (sequenceNumber.toInt() shr 8 and 0xff).toByte()
        characteristic.value = byteArrayOf(1, 3, 1, numOfRecordsBytes[0], numOfRecordsBytes[1])
        mBluetoothGatt!!.writeCharacteristic(characteristic)
    }

    /**
     * Function to get the Time and date of stored glucose test result.
     *
     * @param bytes byte array received in 'onCharacteristicChanged'
     * @return The time stamp of the measurement in "MM/dd/yyyy HH:mm" format.
     */
    private fun getMeasurementTimestamps(bytes: ByteArray): Timestamp? {
        if (bytes.size < 10) return null
        val year = ByteBuffer.wrap(byteArrayOf(0, 0, bytes[4], bytes[3])).int.toString()
        val month = if (bytes[5] < 10) "0" + bytes[5] else bytes[5].toString()
        val day = if (bytes[6] < 10) "0" + bytes[6] else bytes[6].toString()
        val hour = if (bytes[7] < 10) "0" + bytes[7] else bytes[7].toString()
        val min = if (bytes[8] < 10) "0" + bytes[8] else bytes[8].toString()
        val date = "$year/$month/$day $hour:$min:00"
        val pattern = "yyyy/MM/dd HH:mm:ss"
        val dateFormat = SimpleDateFormat(pattern, Locale.US)
        val parsedDate: Date?
        try {
            parsedDate = dateFormat.parse(date)
            if (parsedDate != null) {
                val millis = parsedDate.time
                return Timestamp(parsedDate.time)
            }
        } catch (e: ParseException) {
            e.printStackTrace()
        }
        return Timestamp(0)
    }

    fun closeGlucometer() {
        mIsDeviceFound = false
        if (mBluetoothGatt != null) {
            mBluetoothGatt!!.disconnect()
            mBluetoothGatt!!.close()
            mBluetoothGatt = null
            listener = null
        }
    }
}