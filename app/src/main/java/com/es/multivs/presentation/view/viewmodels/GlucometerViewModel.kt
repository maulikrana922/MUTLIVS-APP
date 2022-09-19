package com.es.multivs.presentation.view.viewmodels

import android.app.Application
import android.bluetooth.BluetoothDevice
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.MutableLiveData
import com.es.multivs.data.bledevices.glucometer.GlucometerListener
import com.es.multivs.data.bledevices.glucometer.GlucometerManager
import com.es.multivs.data.bledevices.glucometer.GlucoseMeterData
import dagger.hilt.android.lifecycle.HiltViewModel
import java.text.DateFormat
import java.text.SimpleDateFormat
import java.util.*
import javax.inject.Inject

/**
 * Created by Marko on 11/8/2021.
 * Etrog Systems LTD.
 */
@HiltViewModel
class GlucometerViewModel @Inject constructor(
    application: Application
) : AndroidViewModel(application) {

    val isGlucometerConnected = MutableLiveData<Boolean>()
    val isGlucometerConnecting = MutableLiveData<Boolean>()
    val glucose = MutableLiveData<String>()

    private var glucometerManager:GlucometerManager? = GlucometerManager(application, configureDeviceListener())
    private var listener: GlucometerListener? = null

    fun initializeDevice() {
        listener = configureDeviceListener()
        glucometerManager = GlucometerManager(getApplication(), listener)
    }

    fun connectDevice(device: BluetoothDevice?) {
        if (device == null) {
            Log.e("DEVICE_CONNECTION", "connectDevice: device is null!!")
        } else {
            glucometerManager?.connectDevice(device)
        }
    }

    fun closeDevice() {
        glucometerManager?.closeGlucometer()
        listener = null
        glucometerManager = null
    }

    private fun configureDeviceListener(): GlucometerListener {
        return object : GlucometerListener {
            override fun onGlucometerDataReceived(measurementList: ArrayList<GlucoseMeterData>) {
                if (measurementList.size > 0) {
                    val currentDataList: ArrayList<GlucoseMeterData> =
                        getLastMeasurements(measurementList)
                    if (currentDataList.size > 0) {
                        val glucoseMgDl: String? =
                            currentDataList[currentDataList.size - 1].glucoseInMgDl
                        glucoseMgDl?.let {
                            glucose.postValue(it)
                        }
                        printDataList(currentDataList)
                    } else {
                        glucose.postValue("")
                    }
                }
            }

            override fun onGlucometerConnected(isConnected: Boolean) {
                isGlucometerConnected.postValue(isConnected)
            }

            override fun onGlucometerConnecting(isConnecting: Boolean) {
                isGlucometerConnecting.postValue(isConnecting)
            }

            override fun onFailure() {
                //TODO: on failure
            }
        }
    }

    private fun getLastMeasurements(measurementList: ArrayList<GlucoseMeterData>): ArrayList<GlucoseMeterData> {
        val list = ArrayList<GlucoseMeterData>()
        val sdf: DateFormat = SimpleDateFormat("yyyyMMdd")
        for (data in measurementList) {
            data.timestamp?.let {
                val isToday = sdf.format(it.time) == sdf.format(Date())
                if (isToday) {
                    list.add(data)
                    if (list.size > 10) {
                        list.removeAt(0)
                    }
                }
            }
        }
        return list
    }

    private fun printDataList(list: ArrayList<GlucoseMeterData>) {
        for (data in list) {
            Log.d("glucosecheck",
                "printDataList: " + data.glucoseInMgDl
                    .toString() + " date: " + data.timestamp
            )
        }
    }
}