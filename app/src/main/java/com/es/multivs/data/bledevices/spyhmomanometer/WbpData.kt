package com.es.multivs.data.bledevices.spyhmomanometer

import android.content.Context
import com.example.bluetoothlibrary.BluetoothLeClass

/**
 * Created by Marko on 11/1/2021.
 * Etrog Systems LTD.
 */
interface WbpData {

//    fun resolveBPData2(var1: ByteArray?, var2: BluetoothLeClass?)

    fun resolveALiBPData(var1: ByteArray?, var2: Context?)

    fun setWBPDataListener(var1: ResolveWbp.OnWBPDataListener?)

    fun getNowDateTime(var1: BluetoothLeClass?)

    fun SendForAll(var1: BluetoothLeClass?)

    fun onSingleCommand(var1: BluetoothLeClass?)

    fun onStopBleCommand(var1: BluetoothLeClass?)

    fun sendUserInfoToBle(var1: BluetoothLeClass?)
}