package com.es.multivs.data.bledevices.glucometer

import java.util.ArrayList

/**
 * Created by Marko on 11/8/2021.
 * Etrog Systems LTD.
 */
interface GlucometerListener {

    fun onGlucometerDataReceived(measurementList: ArrayList<GlucoseMeterData>)

    fun onGlucometerConnected(isConnected: Boolean)

    fun onGlucometerConnecting(isConnecting: Boolean)

    fun onFailure()
}