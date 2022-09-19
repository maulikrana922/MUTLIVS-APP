package com.es.multivs.data.bledevices.glucometer

import java.nio.ByteBuffer
import kotlin.math.pow

/**
 * Created by Marko on 11/8/2021.
 * Etrog Systems LTD.
 */
class GlucometerUtils {

    companion object {

        fun getEventFromIndex(index: Int): String {
            var event = ""
            when (index) {
                0 -> event = "Before meal"
                1 -> event = "After meal"
                2 -> event = "Exercise"
                3 -> event = "Medications"
                4 -> event = "Sick"
                5 -> event = "Other"
            }
            return event
        }

        fun byteArrayToInt(b: ByteArray): Int {
            return ByteBuffer.wrap(b).int
        }

        fun parseSFLOATtoDouble(sFloatArray: ByteArray): Double {
            if (sFloatArray.size != 2) return (-1).toDouble()
            val value = ByteBuffer.wrap(sFloatArray).short // default is big indian
            return getMantissa(value).toDouble() * 10.0.pow(getExponent(value).toDouble())
        }

        private fun getExponent(value: Short): Short {
            return if (value < 0) { // if exponent should be negative
                ((value.toInt() shr 12 and 0x0F or 0xF0).toByte()).toShort()
            } else (value.toInt() shr 12 and 0x0F).toShort()
        }

        private fun getMantissa(value: Short): Int {
            return if (value.toInt() and 0x0800 != 0) { // if mantissa should be negative
                (value.toInt() and 0x0FFF or 0xF000)
            } else (value.toInt() and 0x0FFF)
        }
    }
}