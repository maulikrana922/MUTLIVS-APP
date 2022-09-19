package com.es.multivs.data.network.netmodels

import com.google.gson.annotations.SerializedName

/**
 * Created by user on 8/19/2021.
 */
class MedicationSchedule {
    @SerializedName("medicationId")
    var medicationID = 0

    @SerializedName("medicationName")
    var medicationName: String = ""

    @SerializedName("strength")
    var strength: String = ""

    @SerializedName("dosageQuantity")
    var dosageQuantity:String = "N/A"
        private set

    @SerializedName("dosageForm")
    var dosageForm: String = ""

    @SerializedName("timeForMedication1")
    var timeForMedication1: String = ""

    @SerializedName("timeForMedication2")
    var timeForMedication2: String = ""

    @SerializedName("timeForMedication3")
    var timeForMedication3: String = ""

    @SerializedName("timeForMedication4")
    var timeForMedication4: String = ""

    @SerializedName("asNeeded")
    var asNeeded: String = ""

    //    @Override
    @SerializedName("frequency")
    var frequency: String = ""

}