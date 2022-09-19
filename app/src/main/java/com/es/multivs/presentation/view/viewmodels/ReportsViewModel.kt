package com.es.multivs.presentation.view.viewmodels

import android.app.Application
import android.util.Log
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.AndroidViewModel
import com.es.multivs.R
import com.es.multivs.data.models.ReportsModel
import com.es.multivs.data.network.netmodels.ReportsMeasurements
import com.es.multivs.data.network.netmodels.ReportsMedication
import com.es.multivs.data.repository.ReportsRepository
import com.es.multivs.data.utils.AppUtils
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.withContext
import javax.inject.Inject

/**
 * Created by Dinesh on 03/02/2022.
 */
@HiltViewModel
class ReportsViewModel @Inject constructor(
    application: Application,
    private val reportsRepository: ReportsRepository
) : AndroidViewModel(application) {


    suspend fun getMeasurements(hours: String, requireActivity: FragmentActivity): MutableList<ReportsMeasurements.Message> {
        return withContext(Dispatchers.IO) {
            val list = mutableListOf<ReportsMeasurements.Message>()
            val array = fetchMeasurementsAsync(hours,requireActivity)
            val finalArray = mutableListOf<ReportsMeasurements.Message>()
            for (i in array.indices) {
                if (array[i].blood_glucose != 0F || array[i].body_temperature != 0F || array[i].heart_beat != 0F ||  array[i].spo2 != 0F || array[i].weight != 0F)
                    finalArray.add(array[i])
            }
            val measurementList = async { finalArray }
            list.addAll(measurementList.await())
            list
        }
    }

    suspend fun getMedications(hours: String, requireActivity: FragmentActivity): MutableList<ReportsMedication.Message.Medication> {
        return withContext(Dispatchers.IO) {
            val list = mutableListOf<ReportsMedication.Message.Medication>()
            val measurementList = async { fetchMedicationsAsync(hours,requireActivity) }
            list.addAll(measurementList.await())
            list
        }
    }

    private suspend fun fetchMeasurementsAsync(hours: String, requireActivity: FragmentActivity): List<ReportsMeasurements.Message> {

        val isInternetAvailable = AppUtils.isInternetAvailable(getApplication())
        return if (isInternetAvailable) {
            val measurementsModel = reportsRepository.fetchMeasurements(hours)
            when {
                measurementsModel.error.isNullOrEmpty() -> {
                    handleMeasurementsResults(measurementsModel)
                }
                measurementsModel.error == "Your internet connection is very slow" -> {
                    requireActivity.runOnUiThread {
                        AppUtils.showAlertFragment(
                            requireActivity.supportFragmentManager,
                            requireActivity.getString(R.string.reports),
                            requireActivity.getString(R.string.internet_slow)
                        )
                    }
                    mutableListOf()
                }
                else -> mutableListOf()
            }
        } else {
            mutableListOf()
        }
    }

    private suspend fun fetchMedicationsAsync(hours: String, requireActivity: FragmentActivity): List<ReportsMedication.Message.Medication> {

        val isInternetAvailable = AppUtils.isInternetAvailable(getApplication())
        return if (isInternetAvailable) {
            val medicationModel = reportsRepository.fetchMedications(hours)
            when {
                medicationModel.error.isNullOrEmpty() -> {
                    handleMedicationsResults(medicationModel)
                }
                medicationModel.error == "Your internet connection is very slow" -> {
                    requireActivity.runOnUiThread {
                        AppUtils.showAlertFragment(
                            requireActivity.supportFragmentManager,
                            requireActivity.getString(R.string.reports),
                            requireActivity.getString(R.string.internet_slow)
                        )
                    }
                    mutableListOf()
                }
                else -> {
                    mutableListOf()
                }
            }
        } else {
            mutableListOf()
        }
    }

    private fun handleMeasurementsResults(
        medicationModel: ReportsModel
    ): List<ReportsMeasurements.Message> {
        return medicationModel.measurementsList
    }

    private fun handleMedicationsResults(
        medicationModel: ReportsModel
    ): List<ReportsMedication.Message.Medication> {
        return medicationModel.medicationsList
    }

    fun getDate(timeStamp: String): String {
        Log.e("Date:", "scheduleTimeInMillis")
        return AppUtils.getDateFromTimestamp(timeStamp.toLong(), getApplication())
    }

    fun getBloodPressure(sys: String, dia: String): String {
        val bloodPressure =
            if (sys.isNotEmpty() && dia.isNotEmpty() && sys != "0" && sys != "0.0") {
                if (sys.contains(".")) Math.round(java.lang.Float.valueOf(sys))
                    .toString() + "/" + Math.round(java.lang.Float.valueOf(dia)) else sys.plus("/$dia")
            } else {
                "-"
            }
        return bloodPressure
    }

    fun getHeartRate(heartRate: String): String {
        return if (heartRate.isEmpty() || heartRate == "0" || heartRate == "0.0") "-" else AppUtils.decimalFormat.format(heartRate.toFloat())
    }

    fun getSpo2(spo2: String): String {
        return if (spo2.isEmpty() || spo2 == "0" || spo2 == "0.0") "-" else AppUtils.decimalFormat.format(spo2.toFloat())
    }

    fun getWeight(weight: String): String {
        return if (weight.isEmpty() || weight == "0" || weight == "0.0") "-" else AppUtils.decimalFormat.format(weight.toFloat())
    }

    fun getGlucose(data: String): String {
        return if (data.isEmpty() || data == "0" || data == "0.0") "-" else AppUtils.decimalFormat.format(data.toFloat())
    }

    fun getTemp(temperature: String): String {
        return if (temperature.isEmpty() || temperature == "0" || temperature == "0.0") "-" else AppUtils.decimalFormat.format(temperature.toFloat())
    }
}