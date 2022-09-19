package com.es.multivs.presentation.view.viewmodels

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.es.multivs.data.bledevices.spyhmomanometer.BloodCuffResults
import com.es.multivs.data.bledevices.spyhmomanometer.BpManager
import com.example.bluetoothlibrary.Config
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

/**
 * Created by Marko on 10/31/2021.
 * Etrog Systems LTD.
 */
@HiltViewModel
class BpViewModel @Inject constructor(
    application: Application,

    ) : AndroidViewModel(application) {

    private var bpManager: BpManager? = BpManager(application, application as Config)

    private val _bpResults: MutableLiveData<BloodCuffResults> = MutableLiveData<BloodCuffResults>()
    var bpResults: LiveData<BloodCuffResults> = _bpResults

    private val _pulseRate = MutableLiveData<Int>()
    var pulseRate: LiveData<Int> = _pulseRate

    private val _isBioLightConnected = MutableLiveData<Boolean>()
    var isBioLightConnected: LiveData<Boolean> = _isBioLightConnected

    private val _isFinished = MutableLiveData<Boolean>()
    var isFinished: LiveData<Boolean> = _isFinished

    val isBioLightMeasuring = MutableLiveData<Boolean>()

    fun initializeDevice() {
        bpManager?.setCallback(configureCallback())
    }

    fun closeDevice() {
        bpManager?.let {
            bpManager?.isQuit = true
            it.closeBluetoothGatt()
            _isBioLightConnected.value = false
        }
    }

    fun connectDevice(mac: String) {
        bpManager?.connectToBLT(mac)
    }

    private fun configureCallback(): BpManager.BpCallback {
        return object : BpManager.BpCallback {
            override fun onValidDataReceived(sys: Int, dia: Int, pr: Int) {
                val results = BloodCuffResults(sys, dia)
                _isFinished.postValue(true)
                _bpResults.postValue(results)
                _pulseRate.postValue(pr)
            }

            override fun onDeviceReady(isReady: Boolean) {
                _isBioLightConnected.value = isReady
            }

            override fun onMeasuring(isMeasuring: Boolean) {
                if (isBpMeasuring != isMeasuring) {
                    isBpMeasuring = isMeasuring
                    isBioLightMeasuring.postValue(isMeasuring)
                }
            }

            override fun onFinish() {
                _isFinished.postValue(true)
            }
        }
    }

    var isBpMeasuring = false
}