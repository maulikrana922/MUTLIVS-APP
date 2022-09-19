package com.es.multivs.presentation.view.fragments.calibration

import com.es.multivs.data.network.netmodels.CalibrationResponse

/**
 * Created by Marko on 10/27/2021.
 * Etrog Systems LTD.
 */
interface BusyCallback {
    fun onBusy(busy: Boolean, msg: String = "")
    fun onClose(tag:String)
    fun onResponse()
    fun onInstruction(msg:String)
}