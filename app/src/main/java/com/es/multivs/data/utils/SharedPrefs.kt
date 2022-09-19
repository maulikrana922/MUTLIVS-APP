package com.es.multivs.data.utils

import android.content.Context
import android.content.SharedPreferences
import androidx.annotation.UiThread
import androidx.annotation.WorkerThread

class SharedPrefs {

    companion object {
        private lateinit var prefs: SharedPreferences
        const val PREFS_NAME = "Etrog"

        private val editor: SharedPreferences.Editor
            get() = prefs.edit()

        @WorkerThread
        fun loadAppPrefs(context: Context) {
            prefs = context.getSharedPreferences(PREFS_NAME, 0)
        }

        @UiThread
        fun setIntParam(key: String?, value: Int) {
            try {
                editor.putInt(key, value).apply()
            } catch (ex: Exception) {
                ex.printStackTrace()
            }
        }

        fun getIntParam(key: String?, defaultValue: Int): Int {
            return prefs.getInt(key, defaultValue)
        }
    }
}