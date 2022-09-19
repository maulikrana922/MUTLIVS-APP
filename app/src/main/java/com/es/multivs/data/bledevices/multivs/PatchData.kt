package com.es.multivs.data.bledevices.multivs

import android.util.Log
import java.lang.StringBuilder
import kotlin.collections.ArrayList

/**
 * Created by Marko on 10/20/2021.
 * Etrog Systems LTD.
 */
class PatchData {

     var bodyPosition = 0
     var ecgSampleList = ArrayList<Int>()
     var ppgSampleList = ArrayList<Int>()
     var isEcg = false
     var isPpg = false
     var temperature = 0f
     var steps = 0
     var batteryLevel = 0


    fun getECGDataString(count: Int): String{
        var sampleCounter = 0
        val builder = StringBuilder()
        val lastValues = ecgSampleList.takeLast(count)
        for (i in 0 until count) {
            sampleCounter++
            builder.append(lastValues[i]).append(",")
        }
        val builderString = builder.toString()

        return if (sampleCounter < count) {
            "-1"
        } else builderString.substring(0, builderString.length - 1)

    }

    fun getPPGDataString(count: Int): String{
        val builder = StringBuilder()
        var sampleCounter = 0
        val lastValues = ecgSampleList.takeLast(count)
        for (i in 0 until count) {
            builder.append(lastValues[i]).append(",")
            sampleCounter++
        }
        val builderString = builder.toString()

        return if (sampleCounter < count) {
            "-1"
        } else builderString.substring(0, builderString.length - 1)

    }

    fun zipDataLists(count: Int): String{
        var sampleCounter = 0
        var i = 0
        var list: MutableList<Int> = ArrayList()

        while (i < ecgSampleList.size && i < ppgSampleList.size && i < count) {
            list.add(ecgSampleList[i])
            list.add(ppgSampleList[i])
            sampleCounter += 2
            i++
        }

        if (sampleCounter < count) {
            return "-1"
        }

        val lastValues = list.takeLast(count)
        val builder = StringBuilder()
        for (value in lastValues) {
            builder.append(value).append(",")
        }

        val builderString = builder.toString()
        val finalString = builderString.substring(0, builderString.length - 1)


        return finalString
    }

    fun addAllToECGList(list: List<Int>) {
        ecgSampleList.addAll(list)
    }

    fun addAllToPPGList(list: List<Int>) {
        ppgSampleList.addAll(list)
    }
}