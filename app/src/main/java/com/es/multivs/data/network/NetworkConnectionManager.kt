package com.es.multivs.data.network

import android.annotation.SuppressLint
import android.annotation.TargetApi
import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.net.wifi.WifiManager
import android.os.Build
import android.provider.Settings
import android.util.Log
import java.lang.Exception

class NetworkConnectionManager(base: Context) {
    private val context: Context?
    val isNetworkAvailable: Boolean
        get() {
            val cm = context!!.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
            val networkInfo = cm.activeNetworkInfo
            return networkInfo != null && networkInfo.isConnected
        }

    // need ACCESS_NETWORK_STATE permission
    @get:TargetApi(Build.VERSION_CODES.M)
    @get:SuppressLint("LongLogTag")
    val isNetworkOnline: Boolean
        get() {
            var isOnline = false
            try {
                if (context == null) Log.e(
                    "NetworkConnectionManager",
                    "isNetworkOnline: context was null :( "
                )
                val manager =
                    context!!.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
                val capabilities =
                    manager.getNetworkCapabilities(manager.activeNetwork) // need ACCESS_NETWORK_STATE permission
                isOnline =
                    capabilities != null && capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED)
            } catch (e: Exception) {
                e.printStackTrace()
            }
            return isOnline
        }


    companion object {
        fun isWifiAvailable(context: Context?): Boolean {
            if (context == null) return false
            val connectivityManager =
                context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
            val activeNetwork = connectivityManager.activeNetworkInfo
            return activeNetwork != null && activeNetwork.type == ConnectivityManager.TYPE_WIFI
        }

        fun isMobileNetworkAvailable(context: Context): Boolean {
            val connectivityManager =
                context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
            val activeNetwork = connectivityManager.activeNetworkInfo
            return activeNetwork != null && activeNetwork.type == ConnectivityManager.TYPE_MOBILE
        }

        fun isWifiAdapterOn(context: Context): Boolean {
            val wifiMgr =
                context.applicationContext.getSystemService(Context.WIFI_SERVICE) as WifiManager
            return wifiMgr.isWifiEnabled
        }

        fun isMobileNetworkAdapterOn(context: Context): Boolean {
            return Settings.Secure.getInt(context.contentResolver, "mobile_data", 1) == 1
        }
    }

    init {
        context = base.applicationContext
    }
}