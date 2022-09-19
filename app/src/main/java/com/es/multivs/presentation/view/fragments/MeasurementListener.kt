package com.es.multivs.presentation.view.fragments

import com.es.multivs.data.utils.ButtonDirection

/**
 * Created by Marko on 10/21/2021.
 * Etrog Systems LTD.
 */
interface MeasurementListener {

    fun handleDirection(direction: ButtonDirection, enable: Boolean)

    fun onBusy(busy: Boolean, msg: String = "")

    fun onCloseMeasurement(tag: String, errorMessage: String? = null)
}