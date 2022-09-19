package com.es.multivs.data.models

interface ScheduleItem {

    fun showContent(): String

    fun getItemType(): String

    fun getTitle(): String

    /**
     * returns a string representation of the timestamp given from the server.
     * for example: "08:45"
     */
    fun getTime(): String

    fun isItemActive(): Boolean

    fun taskDone(): Boolean
}