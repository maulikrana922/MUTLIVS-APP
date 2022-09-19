package com.es.multivs.data.models

import com.es.multivs.data.database.entities.MedicationData
import java.util.ArrayList
import java.util.HashMap

/**
 * Created by Marko on 10/19/2021.
 * Etrog Systems LTD.
 */

class MedicationModel(val error: String? = null) {

    private val _map: HashMap<String, ArrayList<String>> = HashMap()
    fun getMap() = _map

    val medicationList: ArrayList<MedicationData> = arrayListOf()
}