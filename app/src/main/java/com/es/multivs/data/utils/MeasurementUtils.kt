package com.es.multivs.data.utils

/**
 * Created by Marko on 11/8/2021.
 * Etrog Systems LTD.
 */
class MeasurementUtils {

    fun interface InterpretOnBodyPosition {
        fun status(status: String)
    }

    companion object {

        // isWornCorrectly && isTesting
        const val TESTING_OK = "InterpretOnBodyPosition.InProgress"

        // !isWornCorrectly && isTesting
        const val TESTING_FAIL = "InterpretOnBodyPosition.TestingFailed"

        // !isWornCorrectly && !isTesting
        const val IDLE_FAIL = "InterpretOnBodyPosition.IdleFailed"

        // isWornCorrectly && !isTesting
        const val IDLE_OK = "InterpretOnBodyPosition.IDLEOK"

        fun interpretSensorPositionOnBody(
            isTesting: Boolean,
            isWornCorrectly: Boolean,
            callback: InterpretOnBodyPosition
        ) {
            if (!isWornCorrectly && isTesting){
                callback.status(TESTING_OK)
            }else if (!isWornCorrectly && isTesting){
                callback.status(TESTING_FAIL)
            }else if (!isWornCorrectly && !isTesting){
                callback.status(IDLE_FAIL)
            }else if (isWornCorrectly && !isTesting){
                callback.status(IDLE_OK)
            }
        }


        fun convertToCelsius(fahrenheit: Float): Float {
            return ((5.0 / 9.0) * (fahrenheit - 32)).toFloat()
        }

        fun convertToFahrenheit(celsius: Float): Float {
            return ((9.0 / 5.0) * celsius + 32).toFloat()
        }
    }


}