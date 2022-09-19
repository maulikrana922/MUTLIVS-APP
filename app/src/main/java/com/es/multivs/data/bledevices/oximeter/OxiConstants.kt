package com.es.multivs.data.bledevices.oximeter

import java.util.*

/**
 * Created by Marko on 11/2/2021.
 * Etrog Systems LTD.
 */
class OxiConstants {

    companion object {
        val UUID_SERVICE_DATA = UUID.fromString("49535343-fe7d-4ae5-8fa9-9fafd205e455")
        val UUID_CHARACTER_RECEIVE = UUID.fromString("49535343-1e4d-4bd9-ba61-23c647249616")
        val UUID_MODIFY_BT_NAME = UUID.fromString("00005343-0000-1000-8000-00805F9B34FB")

        val UUID_CLIENT_CHARACTER_CONFIG = UUID.fromString("00002902-0000-1000-8000-00805f9b34fb")

        const val GITHUB_SITE = "https://github.com/zh2x/SpO2-BLE-for-Android"
    }
}