package com.es.multivs.data.repository

import android.util.Log
import androidx.lifecycle.MutableLiveData
import com.es.multivs.data.bledevices.BleDeviceTypes
import com.es.multivs.data.database.gateway.GatewayDbHelper
import com.es.multivs.data.models.MeasurementModel
import com.es.multivs.data.network.TokenKeeper
import com.es.multivs.data.network.netmodels.UserSchedule
import com.es.multivs.data.network.retrofit.Api
import com.es.multivs.data.utils.Constants
import retrofit2.Response
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Created by Marko on 10/21/2021.
 * Etrog Systems LTD.
 */
@Singleton
class UserScheduleRepository @Inject constructor(
    private val gatewayDbHelper: GatewayDbHelper,
    private val api: Api
) {

    var showTimerLiveData: MutableLiveData<Boolean> = MutableLiveData()

    interface UserScheduleListener {
        fun onResponse(measurementModel: MeasurementModel?)
        fun onFailure()
    }

    private var listener: UserScheduleListener? = null

    fun setOnResponseListener(listener: UserScheduleListener?) {
        this.listener = listener
    }

    suspend fun fetchUserSchedule(): MeasurementModel {
        val measurementModel = MeasurementModel()
        val username: String = gatewayDbHelper.getUsername()
        val baseURL: String = gatewayDbHelper.getBaseURL()
        val url = baseURL + Constants.GET_USER_SCHEDULE + username

        val response: Response<List<UserSchedule>> =
            api.getUserSchedule(url, "Bearer ${TokenKeeper.instance?.token}")

        if (response.isSuccessful) {
            Log.e("response", response.message())
            val measureList = response.body()
            if (measureList != null && measureList.isNotEmpty()) {
                Log.e("calibration:",measureList[0].duration)
                Log.e("measurement:",measureList[1].duration)
                Constants.MeasurementDuration = measureList[1].duration
                Constants.CalibrationDuration = measureList[0].duration
                measureList.forEach {
                    if (it.deviceType == BleDeviceTypes.ES_008){
                        Constants.interval = it.interval
                        Log.e("status User interval", it.interval)
                        if (it.interval.isNotEmpty() && (it.ecgData.isChecked || it.ppgData.isChecked)) {
                            Constants.deviceType = it.deviceType
//                            Constants.interval = it.interval
                            Constants.isCheckedEcg = it.ecgData.isChecked
                            Constants.isCheckedPpg = it.ppgData.isChecked
                        }
                       // Log.e("isCalibration",it.is_calibration_required.toString())

                    }
//                    Log.e("data", it.deviceType.plus(" , ").plus(it.interval))
                }

                if (Constants.interval.isNotEmpty() && (Constants.isCheckedEcg || Constants.isCheckedPpg)) {
                    Log.e("status User repo", "true")
                    showTimerLiveData.postValue(true)
                    Constants.isIntervalsOn = true
                } else {
                    Log.e("status User repo", "false")
                    showTimerLiveData.postValue(false)
                    Constants.isIntervalsOn = false
                }


                val data = measureList.filter {
                    it.deviceType == BleDeviceTypes.ES_020 && it.interval.isEmpty()
                }
                if (data.isNotEmpty())
                    Constants.isMULTIVS =
                        data[0].deviceType == BleDeviceTypes.ES_020 && data[0].interval.isEmpty()
                else Constants.isMULTIVS = false
//                Log.e("isIntervalsOn", Constants.isIntervalsOn.toString())

                for (userSchedule in measureList) {
                    if (userSchedule.frequency != 0) {
                        measurementModel.frequency = userSchedule.frequency
                    } else if (userSchedule.isCalibration == "false") { // schedule for 008 measurement

                        measurementModel.addPatchSchedule(userSchedule)
                        measurementModel.userSchedule.ecgData.isChecked =
                            userSchedule.ecgData.isChecked

                        measurementModel.userSchedule.measurementDevicePosition =
                            userSchedule.measurementDevicePosition

                        measurementModel.userSchedule.ppgData.isChecked =
                            userSchedule.ppgData.isChecked

                        measurementModel.userSchedule.hearRateData.isChecked =
                            userSchedule.hearRateData.isChecked

                        measurementModel.userSchedule.respirationData.isChecked =
                            userSchedule.respirationData.isChecked

                        measurementModel.userSchedule.temperatureData.isChecked =
                            userSchedule.temperatureData.isChecked

                        measurementModel.userSchedule.stepsData.isChecked =
                            userSchedule.stepsData.isChecked

                    } else if (userSchedule.isCalibration == "true") { // schedule for 008 calibrations
                        val devicePosition: String = userSchedule.measurementDevicePosition
                        measurementModel.userSchedule.calibrationDevicePosition = devicePosition

                        val testType: String = userSchedule.testType
                        measurementModel.userSchedule.testType = testType
                        val badDataDetection: String = userSchedule.badDataDetection
                        measurementModel.userSchedule.badDataDetection = badDataDetection
                        measurementModel.userSchedule.is_calibration_required = userSchedule.is_calibration_required

                        measurementModel.calibrationSchedule.set(userSchedule)
                    } else {
                        measurementModel.addNonPatchSchedule(userSchedule)
                    }
                }
            }
        } else {
            return MeasurementModel(error = "something went wrong")
        }

        return measurementModel
    }
}