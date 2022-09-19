package com.es.multivs.data.bledevices.spyhmomanometer

import android.bluetooth.*
import android.bluetooth.le.ScanCallback
import android.bluetooth.le.ScanFilter
import android.bluetooth.le.ScanSettings
import android.content.Context
import android.os.CountDownTimer
import android.os.Handler
import android.os.Looper
import android.util.Log
import com.es.multivs.data.bledevices.BleDeviceTypes
import com.example.bluetoothlibrary.BluetoothLeClass
import com.example.bluetoothlibrary.Config

import com.example.bluetoothlibrary.entity.ConnectBleServiceInfo
import com.example.bluetoothlibrary.entity.Peripheral
import com.example.bluetoothlibrary.entity.SampleGattAttributes
import com.example.bluetoothlibrary.entity.SycnBp
import kotlinx.coroutines.*


/**
 * Created by Marko on 10/31/2021.
 * Etrog Systems LTD.
 */
class BpManager(
    private var context: Context,
    private var config: Config,
) {

    interface BpCallback {
        fun onValidDataReceived(sys: Int, dia: Int, pr: Int)
        fun onDeviceReady(isReady: Boolean)
        fun onMeasuring(isMeasuring: Boolean)
        fun onFinish()
    }

    var isQuit = false
    private var isDeviceFound = false
    private lateinit var deviceID: String
    private var mBluetoothAdapter: BluetoothAdapter? = null
    private var _callback: BpCallback? = null
    private var scanCallback: ScanCallback? = null

    private var mSYS = 0
    private var mDIA = 0
    private var mPR = 0

    private val _handler: Handler = Handler(Looper.getMainLooper())

    /**
     * Classes needed for connecting with ble device.
     */
    private val bleClass: BluetoothLeClass = BluetoothLeClass(context)
    private var bleDevice: BleDevice? = BleDevice()
    private var resolveWbp: ResolveWbp? = ResolveWbp()
    private val peripheralsCopy = ArrayList<Peripheral>()


    fun setCallback(callback: BpCallback) {
        _callback = callback
    }

    /**
     * This timer finishes when the 'onMeasurementfin' function isn't triggered in the last second.
     * When the timer ends, the measurement ends.
     */
    private var countDownTimer: CountDownTimer? = null

    fun connectToBLT(mac: String) {
        deviceID = mac
        _callback?.onDeviceReady(true)
        bleClass.setBluetoothGattCallback()
        bleClass.setOnServiceDiscoverListener(mOnServiceDiscover)
        bleClass.setOnsetDevicePreipheral(mOnSetDevicePeripheral)
        bleClass.setOnDataAvailableListener(mOnDataAvailable)
        bleClass.setOnConnectListener(onConnectListener)
        bleClass.setOnDisconnectListener(onDisconnectListener)
        resolveWbp!!.setWBPDataListener(onWBPDataListener)
        mOnSetDevicePeripheral.setDevicePreipheral(
            bleDevice?.bluetoothDevice,
            51,
            "BltSerial",
            1.0f
        )

        val isConnected = bleClass.connect(mac)
    }

    private var mOnServiceDiscover: BluetoothLeClass.OnServiceDiscoverListener? =
        BluetoothLeClass.OnServiceDiscoverListener {
            val connectServiceInfo = ConnectBleServiceInfo()
            val connectingDevice = config.connectPreipheralOpsition.bluetooth
            connectServiceInfo.deviceName = connectingDevice
            connectServiceInfo.serviceUUID = SampleGattAttributes.SeviceIDfbb0
            connectServiceInfo.charateUUID = SampleGattAttributes.GetCharacteristicIDfbb2
            connectServiceInfo.charateReadUUID = SampleGattAttributes.GetCharacteristicIDfbb1
            connectServiceInfo.conectModel = BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE
            displayGattServices(bleClass.supportedGattServices, connectServiceInfo)
        }

    private var mOnDataAvailable: BluetoothLeClass.OnDataAvailableListener? = object :
        BluetoothLeClass.OnDataAvailableListener {

        override fun onCharacteristicRead(
            gatt: BluetoothGatt,
            characteristic: BluetoothGattCharacteristic
        ) {
            val data = characteristic.value
            resolveWbp!!.resolveBPData2(data, bleClass) //resolve data from wep
        }

        override fun onCharacteristicWrite(
            gatt: BluetoothGatt,
            characteristic: BluetoothGattCharacteristic
        ) {
        }
    }

    var onWBPDataListener: ResolveWbp.OnWBPDataListener? = object : ResolveWbp.OnWBPDataListener {
        override fun onMeasurementBp(var1: Int) {
            countDownTimer?.cancel()
            _handler.post { countDownTimer?.start() }
        }

        /**
         * This function is triggered at the end of the measurement.
         * If device is still connected, consecutive measurements will also trigger this function for each data received.
         * @param SYS Systolic
         * @param DIA Diastolic
         * @param PR Pulse Rate
         * @param isguestmode User type.
         */
        override fun onMeasurementfin(var1: Int, var2: Int, var3: Int, var4: Boolean?) {
            if (!(var1 == 0 || var2 == 0 || var3 == 0)) {
                mSYS = var1
                mDIA = var2
                mPR = var3

                _callback?.onValidDataReceived(mSYS, mDIA, mPR)
            }
        }

        override fun onErroer(var1: Any?) {
            if (var1.toString() == "1"){
                _callback?.onMeasuring(false)
            }
        }

//        parameter:
//        Btbattey: battey power。
//        bleState:Working mode 0 still, 1 is working.
//        version:Firmware version
//        devState:Operation mode 00 is a single measurement, 01 continuous measurement, 08 is idle state.

        /**
         * @param var1 battery power
         * @param var2 working mode - mode 0 still, 1 is working.
         * @param var3 firmware version
         * @param var4 operation mode - 00 is a single measurement, 01 continuous measurement, 08 is idle state.
         */
        override fun onState(var1: Int, var2: String?, var3: String?, var4: String?) {

            if (var2 == "0") {
                _handler.post { _callback?.onMeasuring(false) }
            } else {
                _handler.post { _callback?.onMeasuring(true) }
            }
        }

        override fun onSycnBp(var1: ArrayList<SycnBp?>?) {}

        override fun onTime(var1: String?) {}

        override fun onUser(var1: Int) {}
    }

    private var onDisconnectListener: BluetoothLeClass.OnDisconnectListener? =
        BluetoothLeClass.OnDisconnectListener {
            if (!isQuit) {
                it.connect()
            }
            Handler(Looper.getMainLooper()).post { _callback?.onMeasuring(false) }
        }

    fun ScanForDevice() {
        isDeviceFound = false
        val scanSettings: ScanSettings = buildScanSettings()
        val scanFilterList: MutableList<ScanFilter> = ArrayList()
        val deviceMAC: String? = BleDeviceTypes.getDeviceMAC(BleDeviceTypes.ES_022)
        val scanFilter = ScanFilter.Builder().setDeviceAddress(deviceMAC).build()
        scanFilterList.add(scanFilter)
        if (mBluetoothAdapter == null) {
            val bluetoothManager =
                context.getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager
            mBluetoothAdapter = bluetoothManager.adapter
        }
        mBluetoothAdapter?.bluetoothLeScanner?.startScan(scanFilterList, scanSettings, scanCallback)
        //TODO create callback

    }

    private var onConnectListener: BluetoothLeClass.OnConnectListener? =
        BluetoothLeClass.OnConnectListener {
            Handler(Looper.getMainLooper()).post {
                _callback?.onDeviceReady(true)
            }
        }

    fun stopScan() {
        if (mBluetoothAdapter!!.bluetoothLeScanner != null) {
            isQuit = true
            mBluetoothAdapter!!.bluetoothLeScanner.stopScan(scanCallback)
            mBluetoothAdapter!!.bluetoothLeScanner.stopScan(scanCallback)
        }
    }

    /**
     * coroutine for closing bp device
     */
    val job = SupervisorJob()
    private val scope = CoroutineScope(job + Dispatchers.IO)

    fun closeBluetoothGatt() {
        scope.launch {
            isQuit = true

//        if (_callback != null) {
//            _callback = null
//        }
//        if (scanCallback != null) {
//            scanCallback = null
//        }
            mOnServiceDiscover = null
            mOnDataAvailable = null
            onConnectListener = null
            onDisconnectListener = null
            onWBPDataListener = null
            bleClass.disconnect()
            delay(200)
            bleClass.close()
            job.complete()
        }
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

    private var mOnSetDevicePeripheral =
        BluetoothLeClass.OnsetDevicePreipheral { device, model, SN, protocolVer ->
            val peripheral = Peripheral()
            peripheral.bluetooth = bleDevice?.deviceName
            peripheral.preipheralMAC = bleDevice?.address
            when (model) {
                1 -> peripheral.model = "WT1"
                2 -> peripheral.model = "WT2"
                3 -> peripheral.model = "WT3"
                48 -> peripheral.model = "M70C"
                51 -> {
                    peripheral.model = "WBP202"
                    ResolveWbp.WBPMODE = 1
                }
                57 -> {
                    peripheral.model = "WBP204"
                    ResolveWbp.WBPMODE = 1
                }
                70 -> peripheral.model = "WF100"
                71 -> peripheral.model = "WF200"
                else -> {
                }
            }
            //it is just for wbp
            if (ResolveWbp.WBPMODE != -1) {
                peripheral.webMode = ResolveWbp.WBPMODE
            }
            peripheral.preipheralSN = SN
            peripheral.name = "Smart thermometer"
            peripheral.brand = "Wearcare"
            peripheral.manufacturer = "blt"
            peripheral.isActivation = 0
            peripheral.protocolVer = protocolVer
            peripheral.remark = ""
            synchronized(peripheralsCopy) {
                if (peripheralsCopy.size == 0) {
                    peripheralsCopy.add(peripheral)
                } else {
                    var isTrue = false
                    for (i in peripheralsCopy.indices) {
                        val preipheral3 = peripheralsCopy[i]
                        if (preipheral3.preipheralSN == SN) {
                            isTrue = true
                        }
                    }
                    if (!isTrue) {
                        peripheralsCopy.add(peripheral)
                    }
                }
                Log.e("the connecting devie", peripheral.toString())
                bleClass.setBLEService(peripheral.preipheralMAC)
                config.connectPreipheralOpsition = peripheral // Set to be current device
                Log.e(
                    " the current device",
                    "" + config.connectPreipheralOpsition
                        .preipheralMAC + "" + config.connectPreipheralOpsition.bluetooth
                )
                Log.e("version of the device", "" + peripheral.model)
            }
        }

    private fun displayGattServices(
        gattServices: List<BluetoothGattService>?,
        serviceInfo: ConnectBleServiceInfo
    ) {
        if (gattServices == null) {
            return
        }
        var uuid: String
        for (gattService in gattServices) {
            uuid = gattService.uuid.toString()
            if (serviceInfo.serviceUUID == uuid) {
                val gattCharacteristics = gattService.characteristics
                for (gattCharacteristic in gattCharacteristics) {
                    uuid = gattCharacteristic.uuid.toString()
                    if (uuid == serviceInfo.charateReadUUID) {
                        bleClass.setCharacteristicNotification(
                            gattCharacteristic,
                            true,
                            serviceInfo
                        )
                        bleClass.readCharacteristic(gattCharacteristic)
                        return
                    }
                    if (uuid == serviceInfo.charateUUID) {
                        bleClass.setCharacteristicNotification(
                            gattCharacteristic,
                            true,
                            serviceInfo
                        )
                        return
                    }
                }
            }
        }
    }


    init {
        bleClass.initialize()
        countDownTimer = object : CountDownTimer(1500, 1500) {
            override fun onTick(l: Long) {}
            override fun onFinish() {}
        }
    }
}