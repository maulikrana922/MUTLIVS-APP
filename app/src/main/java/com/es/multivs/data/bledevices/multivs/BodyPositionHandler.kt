package com.es.multivs.data.bledevices.multivs

import android.util.Log

/**
 * Created by Marko on 10/20/2021.
 * Etrog Systems LTD.
 */
class BodyPositionHandler constructor(
    private val measurementTime: Int
) {

    private var startTime: Long = -1
    private var mLastBodyPosition = 0
    private var bodyPositionMap: HashMap<Int, Long> = HashMap()

    private fun print() {
        for ((key, value) in bodyPositionMap) {
            Log.d("bodyPositionHandler", "key: $key | value: $value")
        }
    }

    fun resetMap() {
        bodyPositionMap[1] = 0L
        bodyPositionMap[2] = 0L
        bodyPositionMap[3] = 0L
        bodyPositionMap[4] = 0L
        bodyPositionMap[5] = 0L
        bodyPositionMap[6] = 0L
        bodyPositionMap[8] = 0L
    }

    /**
     * Updates a value in the map with the elapsed time since the last call.
     *
     * @param bodyPosition body position key
     */
    fun add(bodyPosition: Int) {
        if (startTime != -1L) {
            val elapsedTimeMillis = System.currentTimeMillis() - startTime
            insertEntry(mLastBodyPosition, elapsedTimeMillis)
            mLastBodyPosition = bodyPosition
        }
        startTime = System.currentTimeMillis()
        mLastBodyPosition = bodyPosition
    }

    /**
     * Updates the value of the given key by adding it the elapsed time
     * since this function was called.
     *
     * @param bodyPosition current body position.
     * @param time         elapsed time since this function was called.
     */
    private fun insertEntry(bodyPosition: Int, time: Long) {
        val lastValue: Long = bodyPositionMap[bodyPosition]!! // never null
        val newValue: Long = lastValue + time
        bodyPositionMap[bodyPosition] = newValue
        print()
    }

    /**
     * The value represented by the returned key must be bigger than half of the test time.
     * If the value is smaller, the returned value is -1.
     *
     * @return body position key
     */
    fun getMaxTimeBodyPosition(): Int {
        val maxValue: Long = -1
        var keyToReturn = -1
        for ((key, value) in bodyPositionMap) {
            if (value > maxValue) {
                if (value > measurementTime / 2) {
                    keyToReturn = key
                }
            }
        }
        return keyToReturn
    }

    init {
        resetMap()
    }
}