package com.es.multivs.data.models

import com.es.multivs.data.network.netmodels.ReportsMeasurements
import com.es.multivs.data.network.netmodels.ReportsMedication

class ReportsModel(val error: String? = null) {
    val measurementsList: ArrayList<ReportsMeasurements.Message> = arrayListOf()
    val medicationsList: ArrayList<ReportsMedication.Message.Medication> = arrayListOf()
}