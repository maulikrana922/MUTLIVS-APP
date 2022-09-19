package com.es.multivs.data.utils

import com.es.multivs.data.bledevices.BleDeviceTypes
import com.es.multivs.data.database.entities.*
import com.es.multivs.data.models.MedicationScheduleItem
import com.es.multivs.data.models.ScheduleItem
import com.es.multivs.data.models.Survey
import java.lang.StringBuilder
import java.util.*
import kotlin.collections.ArrayList
import kotlin.collections.HashMap

/**
 * Created by Marko on 10/19/2021.
 * Etrog Systems LTD.
 */
class ScheduleUtils {
    companion object {

        fun parseDeviceList(list: ArrayList<String>): String {
            val builder = StringBuilder()

            for (str in list) {
                if (str.contains("MULTIVS")) {
                    builder.append(str).append("\n")
                } else {
                    val measurement = parseDevice(str)
                    builder.append(measurement).append("\n")
                }
            }

            return builder.toString().trim()
        }

        private fun parseDevice(str: String): String {
            var string = ""
            when (str) {
                BleDeviceTypes.ES_022 -> {
                    string = BleDeviceTypes.ES_022_NAME
                }
                BleDeviceTypes.ES_020 -> {
                    string = BleDeviceTypes.ES_020_NAME
                }
                BleDeviceTypes.ES_021 -> {
                    string = BleDeviceTypes.ES_021_NAME
                }
                BleDeviceTypes.ES_023 -> {
                    string = BleDeviceTypes.ES_023_NAME
                }
                BleDeviceTypes.ES_024 -> {
                    string = BleDeviceTypes.ES_024_NAME
                }
            }

            return string
        }

        fun doesMeasurementExist(
            dbItems: List<MeasurementData>?,
            item: MeasurementData
        ): MeasurementData? {

            dbItems?.let {
                for (md in dbItems) {
                    if (item == md) {
                        return md
                    }
                }
            }
            return null
        }

        fun doesMedicationExist(
            dbItems: List<MedicationData>?,
            item: MedicationData
        ): MedicationData? {
            dbItems?.let {
                for (md in dbItems) {
                    if (item == md) {
                        return md
                    }
                }
            }

            return null
        }

        fun doesSurveyExist(
            dbItems: List<SurveyScheduleItem>?,
            item: SurveyScheduleItem
        ): SurveyScheduleItem? {
            dbItems?.let {
                for (survey in dbItems) {
                    if (survey == item) {
                        return survey
                    }
                }
            }
            return null
        }

        fun doesCalibrationExist(
            dbItems: List<CalibrationData>?,
            item: CalibrationData
        ): CalibrationData? {
            dbItems?.let {
                for (cd in dbItems) {
                    if (item == cd) {
                        return cd
                    }
                }
            }
            return null
        }

        fun findActiveCalibration(list: List<CalibrationData>): String {
            val tempList = ArrayList<CalibrationData>(list)
            val twoHoursMilli = 7200000L

            for (i in tempList.indices.reversed()) {
                val item: CalibrationData = tempList[i]

                val timeNowMilli = getCurrentTimeOfDay()
                val scheduleTimeInMillis: Long = timeStampToMillis(item.timeStamp)
                val offset = timeNowMilli + twoHoursMilli
                val isTaskDone: Boolean = item.isTaskDone
                if (!isTaskDone) {
                    if (timeNowMilli <= scheduleTimeInMillis) {
                        if (offset >= scheduleTimeInMillis) {
                            list[i].isActive = true
                            return item.timeStamp
                        }
                    } else {
                        if (scheduleTimeInMillis + twoHoursMilli >= timeNowMilli) {
                            list[i].isActive = true
                            return item.timeStamp
                        }
                    }
                } else {
                    tempList.removeAt(i)
                }
            }
            return ""
        }

        fun findActiveMeasurement(list: List<MeasurementData>): String {
            val tempList = ArrayList<MeasurementData>(list)
            val twoHoursMilli = 7200000L

            for (i in tempList.indices.reversed()) {
                val item: MeasurementData = tempList[i]

                val timeNowMilli = getCurrentTimeOfDay()
                val scheduleTimeInMillis: Long = timeStampToMillis(item.timeStamp)
                val offset = timeNowMilli + twoHoursMilli
                val isTaskDone: Boolean = item.isTaskDone

                if (!isTaskDone) {

                    if (timeNowMilli <= scheduleTimeInMillis) {
                        if (offset >= scheduleTimeInMillis) {
                            list[i].isActive = true
                            return item.timeStamp
                        }
                    } else {
                        if (scheduleTimeInMillis + twoHoursMilli >= timeNowMilli) {
                            list[i].isActive = true
                            return item.timeStamp
                        }
                    }
                } else {
                    tempList.removeAt(i)
                }
            }
            return ""
        }


        /**
         * Finds active survey
         */
        fun findActiveSurvey(list: List<SurveyScheduleItem>): String {
            val tempList = ArrayList<SurveyScheduleItem>(list)
            val twoHoursMilli = 7200000L

            for (i in tempList.indices.reversed()){

                val item = tempList[i]

                val timeNowMilli = getCurrentTimeOfDay()
                val scheduleTimeInMillis = timeStampToMillis(item.timestamp)
                val offset = scheduleTimeInMillis + twoHoursMilli
                val isTaskDone = item.isTaskDone

                if (!isTaskDone && timeNowMilli in scheduleTimeInMillis..offset){
                    item.isActive = true
                    return item.timestamp
                }else{
                    tempList.removeAt(i)
                }
            }
            return ""
        }

        /**
         * Please refer to "Active Schedule Conditions" confluence under "Application".
         * @param list list of [MedicationScheduleItem].
         * @return timestamp of the active medication schedule.
         */
        fun findActiveMedication(list: List<MedicationScheduleItem>): String {
            val tempList = ArrayList<MedicationScheduleItem>(list)
            val oneHoursMilli = 3600000L

            for (i in tempList.indices.reversed()) {
                val item: MedicationScheduleItem = tempList[i]

                val timeNowMilli = getCurrentTimeOfDay()
                val scheduleTimeInMillis = timeStampToMillis(item.timeStamp)
                val offset = scheduleTimeInMillis + oneHoursMilli
                val isTaskDone = item.isTaskDone

                if (!isTaskDone && timeNowMilli in scheduleTimeInMillis..offset) {
                    item.isActive = true
                    return item.timeStamp
                } else {
                    tempList.removeAt(i)
                }
            }
            return ""
        }


        /**
         * Converts a timestamp in 24h format to milliseconds.
         * @param timeStamp given time in 24h format.
         * @return timestamp representation in milliseconds.
         */
        fun timeStampToMillis(timeStamp: String): Long {
            val oneHourMilli = 3600000L
            val oneMinuteMilli = 60000L
            val hourMinutes = timeStamp.split(":").toTypedArray()
            var minutes = hourMinutes[1]
            var hour = hourMinutes[0]

            /**
             * Replace leading '0' at start of 'hour' and 'minutes'
             * example: 08:05 -> 8:5
             */
            hour = hour.replaceFirst("^0+(?!$)".toRegex(), "")
            minutes = minutes.replaceFirst("^0+(?!$)".toRegex(), "")

            val hourInt = hour.toLong()
            val minutesInt = minutes.toLong()
            return oneHourMilli * hourInt + oneMinuteMilli * minutesInt
        }

        /**
         * get current time in milliseconds from start of day
         */
        fun getCurrentTimeOfDay(): Long {
            val calender = Calendar.getInstance()
            var timeNowMilli = calender.timeInMillis

            calender.apply {
                set(Calendar.HOUR_OF_DAY, 0)
                set(Calendar.MINUTE, 0)
                set(Calendar.SECOND, 0)
                set(Calendar.MILLISECOND, 0)
            }

            timeNowMilli -= calender.timeInMillis

            return timeNowMilli
        }

        /**
         * window is set to 00:0o to 00:30
         */
        fun isMidnightUpdateWindow(): Boolean {
            val current = Calendar.getInstance()

            val before = Calendar.getInstance()
            before.set(Calendar.HOUR_OF_DAY, 0)
            before.set(Calendar.MINUTE, 0)
            before.set(Calendar.SECOND, 0)

            val after = Calendar.getInstance()
            after.set(Calendar.HOUR_OF_DAY, 0)
            after.set(Calendar.MINUTE, 30)
            after.set(Calendar.SECOND, 0)

            return current in before..after
        }

        fun parseScheduleTimeToEpochMillis(item: ScheduleItem?): String {
            item?.let {
                val calendar = Calendar.getInstance()
                calendar.set(Calendar.HOUR_OF_DAY, getValueFromTimeStamp(it.getTime(), 0))
                calendar.set(Calendar.MINUTE, getValueFromTimeStamp(it.getTime(), 1))
                calendar.set(Calendar.SECOND, 0)

                return (calendar.timeInMillis / 1000).toString()
            }

            return System.currentTimeMillis().toString()
        }

        private fun getValueFromTimeStamp(timeStamp: String, position: Int): Int {
            val split = timeStamp.split(":").toTypedArray()
            var value = split[position]
            value = value.replaceFirst("^0+(?!$)".toRegex(), "")
            return value.toInt()
        }

        fun aggregateMedsByTimeStamps(newItemsList: List<MedicationData>): List<MedicationScheduleItem> {
            val listToReturn: ArrayList<MedicationScheduleItem> = ArrayList()
            val map = HashMap<String, Pair<ArrayList<MedicationData>, Boolean>>()

            for (item in newItemsList) {
                if (map.containsKey(item.timeStamp)) {
                    val oldList = map[item.timeStamp]?.first
                    oldList!!.add(item)
                    map[item.timeStamp] = Pair(oldList, item.isTaskDone)
                } else {
                    map[item.timeStamp] = Pair(ArrayList(), item.isTaskDone)
                    map[item.timeStamp]?.first?.add(item)
                }
            }

            for ((key, value) in map) {
                listToReturn.add(MedicationScheduleItem(value.first, value.second))
            }

            return listToReturn
        }

        fun aggregateSurveyListByTimeStamps(list: List<Survey>): List<SurveyScheduleItem> {

            val map = HashMap<String, ArrayList<Survey>>()

            val listToReturn: ArrayList<SurveyScheduleItem> = ArrayList()

            list.forEach {
                if (it.timeOfSurvey1.trim().isNotEmpty()) {
                    if (!map.containsKey(it.timeOfSurvey1)) {
                        map[it.timeOfSurvey1] = ArrayList()
                        map[it.timeOfSurvey1]?.add(it)
                    } else {
                        map[it.timeOfSurvey1]?.add(it)
                    }
                }

                if (it.timeOfSurvey2.trim().isNotEmpty()) {
                    if (!map.containsKey(it.timeOfSurvey2)) {
                        map[it.timeOfSurvey2] = ArrayList()
                        map[it.timeOfSurvey2]?.add(it)
                    } else {
                        map[it.timeOfSurvey2]?.add(it)
                    }
                }

                if (it.timeOfSurvey3.trim().isNotEmpty()) {
                    if (!map.containsKey(it.timeOfSurvey3)) {
                        map[it.timeOfSurvey3] = ArrayList()
                        map[it.timeOfSurvey3]?.add(it)
                    } else {
                        map[it.timeOfSurvey3]?.add(it)
                    }
                }

                if (it.timeOfSurvey4.trim().isNotEmpty()) {
                    if (!map.containsKey(it.timeOfSurvey4)) {
                        map[it.timeOfSurvey4] = ArrayList()
                        map[it.timeOfSurvey4]?.add(it)
                    } else {
                        map[it.timeOfSurvey4]?.add(it)
                    }
                }
            }

            for ((key, value) in map) {
                value.forEach {
                    listToReturn.add(
                        SurveyScheduleItem(
                            key,
                            false,
                            false,
                            it.surveyName,
                            it.questionList,
                            it.surveyID,
                            it.daysOfWeek
                        )
                    )
                }
            }

            return listToReturn
        }

        /**
         * get the time difference between the given schedule and the current time
         */
        fun getTimeDifference(scheduleItem: ScheduleItem): Long {
            val timeNowMilli = getCurrentTimeOfDay()
            val scheduleTime = timeStampToMillis(scheduleItem.getTime())
            return kotlin.math.abs(timeNowMilli - scheduleTime)
        }
    }
}