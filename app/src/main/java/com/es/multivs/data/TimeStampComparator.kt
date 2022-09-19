package com.es.multivs.data

import com.es.multivs.data.models.ScheduleItem

class TimeStampComparator : Comparator<ScheduleItem> {

    override fun compare(o1: ScheduleItem?, o2: ScheduleItem?): Int {

        val hourMinutes1: List<String> = o1!!.getTime().split(":")
        val hourMinutes2: List<String> = o2!!.getTime().split(":")

        val o1Hour = hourMinutes1[0].toInt()
        val o1Minutes = hourMinutes1[1].toInt()

        val o2Hour = hourMinutes2[0].toInt()
        val o2Minutes = hourMinutes2[1].toInt()

        return if (o1Hour > o2Hour) {
            1
        } else if (o1Hour < o2Hour) {
            -1
        } else o1Minutes.compareTo(o2Minutes)
    }
}