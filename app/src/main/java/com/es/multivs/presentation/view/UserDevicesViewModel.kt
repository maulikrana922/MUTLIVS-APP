package com.es.multivs.presentation.view

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.es.multivs.data.bledevices.BleDeviceTypes
import com.es.multivs.data.network.netmodels.UserDevices
import com.es.multivs.data.repository.UserDeviceRepository
import com.es.multivs.data.utils.AppUtils
import dagger.hilt.android.lifecycle.HiltViewModel

import retrofit2.Response
import java.util.ArrayList
import javax.inject.Inject

/*
 * created by marko katziv
 * Etrog Systems LTD.
 */

/**
 * A viewModel class that is responsible for storing the fetched user devices from the server.
 */
@HiltViewModel
class UserDevicesViewModel @Inject constructor(
    private val deviceRepo: UserDeviceRepository,
    application: Application
) :
    AndroidViewModel(application) {

    private var deviceList: MutableLiveData<ArrayList<String>> = MutableLiveData()
    fun getDevices(): LiveData<ArrayList<String>> {
        return deviceList
    }

    suspend fun loadDevices() {
        val isInternetAvailable = AppUtils.isInternetAvailable(getApplication())
        if (isInternetAvailable) {

            val response: Response<UserDevices> = deviceRepo.fetchUserDevices()

            if (response.isSuccessful) {
                response.body()?.let {
                    if (it.deviceInfoList != null) {
                        BleDeviceTypes.setUserDevices(it)
                        setDeviceTypes(it)
                    }
                }
            } else {
                //TODO:
            }

        } else {
            //TODO:
        }

    }

    private fun setDeviceTypes(userDevices: UserDevices) {
        val deviceOrder = ArrayList<String>()
        val deviceInfoList: List<UserDevices.DeviceInfo> = userDevices.deviceInfoList

        for (deviceInfo in deviceInfoList) {
            deviceOrder.add(deviceInfo.deviceID.substring(0, 6)) // clean device id.
        }
        deviceList.postValue(deviceOrder)
    }
}
