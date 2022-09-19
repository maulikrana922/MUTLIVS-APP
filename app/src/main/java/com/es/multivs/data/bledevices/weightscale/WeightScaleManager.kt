package com.es.multivs.data.bledevices.weightscale

import aicare.net.cn.iweightlibrary.entity.AlgorithmInfo
import aicare.net.cn.iweightlibrary.entity.BodyFatData
import aicare.net.cn.iweightlibrary.entity.DecimalInfo
import aicare.net.cn.iweightlibrary.entity.WeightData
import aicare.net.cn.iweightlibrary.utils.AicareBleConfig
import aicare.net.cn.iweightlibrary.wby.WBYService
import aicare.net.cn.iweightlibrary.wby.WBYService.WBYBinder
import android.bluetooth.BluetoothDevice
import android.content.*
import android.os.IBinder
import android.util.Log
import java.lang.IllegalArgumentException
import java.lang.ref.WeakReference
import java.math.BigDecimal
import java.text.DecimalFormat

class WeightScaleManager(context: Context, listener: WeightScaleListener) {


    companion object {
        const val STATE_DISCONNECTED = 0
        const val STATE_CONNECTED = 1
        private const val LBS_FACTOR = 2.2046226218
    }

    private var weightScaleListener: WeightScaleListener? = null
    private var isBound = false
    private var mBroadcastReceiver: BroadcastReceiver? = null
    private var contextWeakReference: WeakReference<Context>? = null
    private var binder: WBYBinder? = null

    private var decimalFormat: DecimalFormat = DecimalFormat("##.#")

    init {
        weightScaleListener = listener
        contextWeakReference = WeakReference(context)
        mBroadcastReceiver = createBroadcastReceiver()
        context.registerReceiver(mBroadcastReceiver, createIntentFilter())
    }

    fun connectDevice(device: BluetoothDevice) {
        val serviceIntent = Intent(
            contextWeakReference!!.get(),
            WBYService::class.java
        )
        serviceIntent.putExtra("aicare.net.cn.fatscale.extra.DEVICE_ADDRESS", device.address)
        contextWeakReference!!.get()!!.bindService(
            serviceIntent,
            serviceConnection,
            Context.BIND_AUTO_CREATE
        ) // maybe useless
        contextWeakReference!!.get()!!.startService(serviceIntent)
//        mUuidListSize = device.getUuids().length;
    }

    private val serviceConnection: ServiceConnection = object : ServiceConnection {
        override fun onServiceConnected(name: ComponentName, service: IBinder) {
            binder = service as WBYBinder
            isBound = true
            binder?.syncUnit(AicareBleConfig.UNIT_LB)
        }

        override fun onServiceDisconnected(name: ComponentName) {
            isBound = false
        }
    }

    private fun createBroadcastReceiver(): BroadcastReceiver? {
        return object : BroadcastReceiver() {
            override fun onReceive(context: Context, intent: Intent) {
                val action = intent.action
                val did: Int
                if ("android.bluetooth.adapter.action.STATE_CHANGED" == action) {
                    did = intent.getIntExtra("android.bluetooth.adapter.extra.STATE", -1)
                } else {
                    val result: String?
                    if ("aicare.net.cn.fatscale.action.CONNECT_STATE_CHANGED" == action) {
                        did = intent.getIntExtra("aicare.net.cn.fatscale.extra.CONNECT_STATE", -1)
                        result =
                            intent.getStringExtra("aicare.net.cn.fatscale.extra.DEVICE_ADDRESS")
                        if (did == STATE_DISCONNECTED) {
                            weightScaleListener!!.onWeightScaleConnected(false)
                        } else if (did == STATE_CONNECTED) {
                            weightScaleListener!!.onWeightScaleConnected(true)
                        }
                    } else {
                        val cmd: String?
                        if ("aicare.net.cn.fatscale.action.CONNECT_ERROR" == action) {
                            cmd = intent.getStringExtra("aicare.net.cn.fatscale.extra.ERROR_MSG")
                            val errCode =
                                intent.getIntExtra("aicare.net.cn.fatscale.extra.ERROR_CODE", -1)
                        } else if ("aicare.net.cn.fatscale.action.WEIGHT_DATA" == action) {
                            val weightData =
                                intent.getSerializableExtra("aicare.net.cn.fatscale.extra.WEIGHT_DATA") as WeightData?
                            val weight = AicareBleConfig.getWeight(
                                weightData!!.weight,
                                AicareBleConfig.UNIT_LB,
                                weightData.decimalInfo
                            ).toFloat().toDouble()
                            val truncatedWeight =
                                BigDecimal(weight).setScale(1, BigDecimal.ROUND_HALF_DOWN)
                                    .toDouble()

//                            double weightResult = 0;
//                            if (mUuidListSize == 1) {
//                                weightResult = weightData.getWeight() / 100;
//                            }
//                            else if (mUuidListSize == 2) {
//                                weightResult = weightData.getWeight() / 10;
//                            }

                            weightScaleListener?.onWeightScaleDataReceived(decimalFormat.format(truncatedWeight).toDouble())
                        } else if ("aicare.net.cn.fatscale.action.SETTING_STATUS_CHANGED" == action) {
                            did = intent.getIntExtra(
                                "aicare.net.cn.fatscale.extra.SETTING_STATUS",
                                -1
                            )
                            //onGetSettingStatus(did);
                            if (did == 10) {
                            }
                        } else if ("aicare.net.cn.fatscale.action.RESULT_CHANGED" == action) {
                            did =
                                intent.getIntExtra("aicare.net.cn.fatscale.extra.RESULT_INDEX", -1)
                            result = intent.getStringExtra("aicare.net.cn.fatscale.extra.RESULT")
                        } else if ("aicare.net.cn.fatscale.action.FAT_DATA" == action) {
                            //on error
                            val isHistory = intent.getBooleanExtra(
                                "aicare.net.cn.fatscale.extra.IS_HISTORY",
                                false
                            )
                            val bodyFatData =
                                intent.getSerializableExtra("aicare.net.cn.fatscale.extra.FAT_DATA") as BodyFatData?
                        } else if ("aicare.net.cn.fatscale.action.AUTH_DATA" == action) {
                            val sources =
                                intent.getByteArrayExtra("aicare.net.cn.fatscale.extra.SOURCE_DATA")
                            val bleReturn =
                                intent.getByteArrayExtra("aicare.net.cn.fatscale.extra.BLE_DATA")
                            val encrypt =
                                intent.getByteArrayExtra("aicare.net.cn.fatscale.extra.ENCRYPT_DATA")
                            val isEquals = intent.getBooleanExtra(
                                "aicare.net.cn.fatscale.extra.IS_EQUALS",
                                false
                            )
                        } else if ("aicare.net.cn.fatscale.action.DID" == action) {
                            did = intent.getIntExtra("aicare.net.cn.fatscale.extra.DID", -1)
                        } else if ("aicare.net.cn.fatscale.action.DECIMAL_INFO" == action) {
                            val decimalInfo =
                                intent.getSerializableExtra("aicare.net.cn.fatscale.extra.DECIMAL_INFO") as DecimalInfo?
                        } else if ("aicare.net.cn.fatscale.action.CMD" == action) {
                            cmd = intent.getStringExtra("aicare.net.cn.fatscale.extra.CMD")
                        } else if ("aicare.net.cn.fatscale.action.ALGORITHM_INFO" == action) {
                            val algorithmInfo =
                                intent.getSerializableExtra("aicare.net.cn.fatscale.extra.ALGORITHM_INFO") as AlgorithmInfo?
                            // onGetAlgorithmInfo(algorithmInfo);
                        }
                    }
                }
            }
        }
    }

    private fun createIntentFilter(): IntentFilter {
        val intentFilter = IntentFilter()

        intentFilter.addAction("aicare.net.cn.fatscale.action.WEIGHT_DATA")
        intentFilter.addAction("android.bluetooth.adapter.action.STATE_CHANGED")
        intentFilter.addAction("aicare.net.cn.fatscale.action.CONNECT_STATE_CHANGED")
        intentFilter.addAction("aicare.net.cn.fatscale.action.CONNECT_ERROR")
        intentFilter.addAction("aicare.net.cn.fatscale.action.SETTING_STATUS_CHANGED")
        intentFilter.addAction("aicare.net.cn.fatscale.action.RESULT_CHANGED")
        intentFilter.addAction("aicare.net.cn.fatscale.action.DID")

        intentFilter.addAction("aicare.net.cn.fatscale.action.FAT_DATA")
        intentFilter.addAction("aicare.net.cn.fatscale.action.AUTH_DATA")
        intentFilter.addAction("aicare.net.cn.fatscale.action.DECIMAL_INFO")
        intentFilter.addAction("aicare.net.cn.fatscale.action.CMD")
        intentFilter.addAction("aicare.net.cn.fatscale.action.ALGORITHM_INFO")

        return intentFilter
    }

    fun closeDevice() {
        if (isBound) {
            binder!!.disconnect()

//            contextWeakReference.get().unbindService(serviceConnection);

//            serviceConnection = null;
        }
        try {
            contextWeakReference!!.get()!!.unregisterReceiver(mBroadcastReceiver)
        } catch (e: IllegalArgumentException) {
            Log.d("IllegalArgumentException", "closeDevice: receiver wasn't registered")
        }
        contextWeakReference!!.get()!!.stopService(
            Intent(
                contextWeakReference!!.get(),
                WBYService::class.java
            )
        )
        weightScaleListener = null
    }
}