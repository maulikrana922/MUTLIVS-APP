package com.es.multivs.data.bledevices.multivs

import android.bluetooth.BluetoothDevice
import android.bluetooth.le.ScanResult
import android.content.Context
import android.os.Handler
import android.os.Looper
import android.util.Log
import com.youhong.cuptime.blesdk.BaseBleOperation
import com.youhong.cuptime.blesdk.J1767BleCallbacks.*
import com.youhong.cuptime.blesdk.J1767BleOperation
import kotlinx.coroutines.suspendCancellableCoroutine
import java.util.*
import kotlin.coroutines.resume
import kotlin.math.roundToInt

/**
 * Created by Marko on 10/20/2021.
 * Etrog Systems LTD.
 */
class MultiVsManager(
    context: Context,

    ) {
    private var multiVsListener: MultiVsListener? = null

    private var handler: Handler = Handler(Looper.getMainLooper())
    private var mBleOperation: J1767BleOperation = J1767BleOperation(context)

    private var ecgSampleList: ArrayList<Int> = ArrayList()
    private var ppgSampleList: ArrayList<Int> = ArrayList()

    private var isTestRunning = false
    private var latestBodyPosition = 0
    private var lastPacketTimeStamp: Long = 0
    private var dataCounter = 0
    private lateinit var lastDevice: BluetoothDevice


    fun closeDevice(disconnect: Boolean) {

        stopTest()

        if (disconnect) {
            if (mBleOperation.isConnected) {
                mBleOperation.disconnect(disconnect_callback)
            }
        }
    }

    fun startEcgPpg(ecgEnable: Boolean, ppgEnable: Boolean) {

        isTestRunning = ecgEnable || ppgEnable
        ecgSampleList.clear()
        ppgSampleList.clear()
        mBleOperation.enableEcgAndPPG(
            10.toByte(),
            (if (ppgEnable && ecgEnable) 0 else if (ecgEnable) 1 else 2).toByte(),
            object : EnableEcgAndPPGCallback {
                override fun onCallback(status: Int) {}

                override fun onDataCallback(data: ByteArray) {
                    lastPacketTimeStamp = System.currentTimeMillis()
                    val ecgSamples = ArrayList<Int>()
                    val ppgSamples = ArrayList<Int>()
                    var i = 1


                    while (i < data.size - 1) {

                        val ecgSample: Int =
                            ((data[i].toInt() shl 8) and 0xFF00) + (data[i + 1].toInt() and 0xFF)
                        val ppgSample: Int =
                            ((data[i + 2].toInt() shl 8) and 0xFF00) + (data[i + 3].toInt() and 0xFF)

                        ecgSampleList.add(ecgSample)
                        ppgSampleList.add(ppgSample)
                        ecgSamples.add(ecgSample)
                        ppgSamples.add(ppgSample)
                        if (ecgSampleList.size % 198 == 0) {
                            val hr: Int =
                                ECGUtil.extractHrFromEcgAlgorithm(198, ecgSampleList, null)
                            if (hr != -1) {
                                if (multiVsListener != null) {
                                    handler.post { multiVsListener?.onHeartRateReceived(hr) }
                                    dataCounter++
                                }
                            }
                        }

                        if (ecgEnable && ecgSampleList.size % (200 * 2) == 0) {
                            //upper limit is 1.5 (mv) , lower limit is below -1.45 (mv)
                            if (ECGUtil.isBadDataInEcg(200, ecgSampleList, 3300, 50)) {
                                handler.post { multiVsListener?.onBadData() }
                                return
                            }
                        }
                        i += 4
                    }

                    val patchData = PatchData()
                    patchData.ecgSampleList = ecgSamples
                    patchData.ppgSampleList = ppgSamples
                    patchData.isEcg = ecgEnable
                    patchData.isPpg = ppgEnable
                    multiVsListener?.onPatchDataReceived(patchData)
                }
            })
    }

    fun stopTest() {
        isTestRunning = false
        if (mBleOperation.isConnected){
            val testStopTime = System.currentTimeMillis()
            mBleOperation.enableEcgAndPPG(0.toByte(), 0.toByte(), object : EnableEcgAndPPGCallback {
                override fun onCallback(status: Int) {}
                override fun onDataCallback(data: ByteArray) {}
            })
        }
    }

    private val disconnect_callback = BaseBleOperation.ResultNotifyCallback { result ->
        if (multiVsListener != null) {
            multiVsListener?.onConnected(false)
            multiVsListener = null
            //            mBleOperation = null;
        }
    }

    private val timeOutCallback = BaseBleOperation.ReconnectingTimeOutCallback {}

    private val reconnect_callback = BaseBleOperation.ResultNotifyCallback {

    }

    private val connectedCallback = BaseBleOperation.ResultNotifyCallback { result: Boolean ->
        if (!result){
            multiVsListener?.onConnected(false)
            return@ResultNotifyCallback
        }

        val operationList: MutableList<Runnable> =
            ArrayList()

        // set time
        operationList.add(Runnable {
            mBleOperation.setTime(Calendar.getInstance(Locale.ENGLISH)) {}
        })

        // get battery level
        operationList.add(Runnable {
            mBleOperation.getBatteryLevel { batteryLevel: Int, batteryVoltage: Int ->
                if (multiVsListener != null) {
                    multiVsListener!!.onBatteryLevelReceived(batteryLevel)
                }
            }
        })

        // check worn correctly
        operationList.add(Runnable {
            mBleOperation.CheckWornCorrectly { isCorrect: Boolean ->
                if (multiVsListener != null) {
                    multiVsListener!!.onWearingStatus(isCorrect)
                }
            }
        })

        // get body position
        operationList.add(Runnable {
            mBleOperation.getBodyPosition { bodyPosition: Int ->
                latestBodyPosition = bodyPosition
                if (multiVsListener != null) {
                    multiVsListener!!.onBodyPositionChanged(bodyPosition)
                }
            }
        })

        // enable temperature
        operationList.add(Runnable {
            mBleOperation.enableTemperatureMeassurement(
                true,
                object : EnableRtTemperatureMessurementCallback {
                    override fun onCallback() {}
                    override fun onDataCallback(ntcTemperature: Float, pcbTemperature: Float) {
                        if (multiVsListener != null) {
                            multiVsListener!!.onTemperatureReceived(ntcTemperature)
                        }
                    }
                })
        })

        // enable heart rate while ecg test
        operationList.add(object : Runnable {
            private var pulseCount = 0
            private var pulseSum = 0
            override fun run() {
                mBleOperation.enableHrReportWhileEcgTest(
                    true,
                    object : EnableHrReportWhileEcgTestCallback {
                        override fun onCallback() {}
                        override fun onDataCallback(hr: Int) {
                            pulseCount++
                            pulseSum += hr
                            if (pulseCount == 3) {
                                val avgHr = (pulseSum.toFloat() / 3).roundToInt()
                                pulseCount = 0
                                pulseSum = 0
                            }
                        }
                    })
            }
        })

        // enable step counting
        operationList.add(Runnable {
            mBleOperation.enableRTStepCounting(object : EnableRTStepCallback {
                override fun onCallback() {
                }

                override fun onDataCallback(
                    step: Int,
                    calories: Int,
                    distance: Int,
                    activityTime: Int
                ) {
                    Log.e("step",step.toString())
                    handler.post {
                        if (multiVsListener != null) {
                            multiVsListener!!.onStepsReceived(step)
                        }
                    }
                }
            })
        })

        // get time - currently not in use
        operationList.add(Runnable {
            mBleOperation.getTime { c: Calendar ->
                val dateTimeString =
                    ("Day: " + c[Calendar.DATE] + ", Month:" + (c[Calendar.MONTH] + 1) + ", Year:" + c[Calendar.YEAR]
                            + ", hour: " + c[Calendar.HOUR] + ", minute: " + c[Calendar.MINUTE] + ", second: " + c[Calendar.SECOND])
            }
        })

        if (mBleOperation.isConnected) {

            for (i in operationList.indices) {
                handler.postDelayed(operationList[i], i * 1000L)
            }

            handler.postDelayed( {
                if (multiVsListener != null) {
                    multiVsListener?.onConnected(result)
                }
            },4200)
        }
    }

    private fun getUUID(result: ScanResult): String {
        return UUID.nameUUIDFromBytes(result.scanRecord!!.bytes).toString()
    }

    fun setAndConnect(device: BluetoothDevice, listener: MultiVsListener?) {
        lastDevice = device
        multiVsListener = listener
        mBleOperation.connect(
            device,
            connectedCallback,
            reconnect_callback,
            timeOutCallback,
            disconnect_callback
        )
    }

    fun hardDisconnect(){
        mBleOperation.hardDisconnect()
    }


    fun disconnect() {
        mBleOperation.disconnect(disconnect_callback)
//        multiVsListener = null
    }

    fun reconnect() {
        if (!mBleOperation.isConnected) {
            reconnect_callback.onResultNotify(true)
        }
    }

    @Deprecated("Not in use anymore")
    suspend fun disableECGAndPPG() {
        return suspendCancellableCoroutine { continuation ->
            mBleOperation.enableEcgAndPPG(0.toByte(), 0.toByte(), object : EnableEcgAndPPGCallback {
                override fun onCallback(status: Int) {
                    if (!continuation.isCompleted) {
                        continuation.resume(Unit)
                    }
                }

                override fun onDataCallback(data: ByteArray) {}
            })
        }
    }

    @Deprecated("Not in use anymore")
    suspend fun disableTemperature() {
        return suspendCancellableCoroutine { continuation ->
            mBleOperation.enableTemperatureMeassurement(
                false,
                object : EnableRtTemperatureMessurementCallback {
                    override fun onCallback() {
                        if (!continuation.isCompleted) {
                            continuation.resume(Unit)
                        }
                    }

                    override fun onDataCallback(ntcTemperature: Float, pcbTemperature: Float) {}
                })
        }
    }

    @Deprecated("Not in use anymore")
    suspend fun disableHeartRate() {
        return suspendCancellableCoroutine { continuation ->
            mBleOperation.enableHrReportWhileEcgTest(
                false,
                object : EnableHrReportWhileEcgTestCallback {
                    override fun onCallback() {
                        if (!continuation.isCompleted) {
                            continuation.resume(Unit)
                        }
                    }

                    override fun onDataCallback(hr: Int) {}
                })
        }
    }

    @Deprecated("Not in use anymore")
    suspend fun disableSteps() {
        return suspendCancellableCoroutine { continuation ->
            mBleOperation.disableRTStepCounting {
                if (!continuation.isCompleted) {
                    continuation.resume(Unit)
                }
            }
        }
    }
}