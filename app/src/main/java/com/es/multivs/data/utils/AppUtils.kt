package com.es.multivs.data.utils

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.graphics.Color
import android.graphics.Typeface
import android.location.LocationManager
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.os.BatteryManager
import android.os.Build
import android.text.format.DateFormat
import android.view.LayoutInflater
import android.view.View
import android.widget.TextView
import android.widget.Toast
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import androidx.core.location.LocationManagerCompat
import androidx.core.os.bundleOf
import androidx.fragment.app.FragmentManager
import com.es.multivs.BaseApplication
import com.es.multivs.R
import com.es.multivs.presentation.view.fragments.AlertDialogFragment
import com.es.multivs.presentation.view.fragments.LocationAlertDialogFragment
import com.es.multivs.presentation.view.fragments.VideoAlertDialogFragment
import com.google.android.material.snackbar.Snackbar
import java.net.NetworkInterface
import java.text.DecimalFormat
import java.text.SimpleDateFormat
import java.util.*

/*
* created by Marko
Etrog Systems LTD. 31/8/2021.
*/
class AppUtils {
    companion object {
        lateinit var application: BaseApplication
        private var bluetoothRefreshAlert: androidx.appcompat.app.AlertDialog? = null

        private var snackbar: Snackbar? = null

        const val NOTIFICATION_CHANNEL_ID = "1001"
        private const val NOTIFICATION_CHANNEL_NAME = "MULTIVS channel"
        var decimalFormat: DecimalFormat = DecimalFormat("##.#")

        fun makeErrorSnackbarNoAction(
            v: View,
            message: String,
            duration: Int = Snackbar.LENGTH_SHORT,
        ) {

            snackbar = Snackbar.make(v, message, duration).apply {
                setTextColor(Color.RED)
                setBackgroundTint(Color.WHITE)
                setText(message)
                setMaxInlineActionWidth(2)
                val sbTv =
                    view.findViewById<TextView>(com.google.android.material.R.id.snackbar_text)
                sbTv.textSize = 21f
                sbTv.maxLines = 2
                sbTv.setTypeface(sbTv.typeface, Typeface.BOLD)

                show()
            }
        }

        fun makeShowMessage(
            v: View,
            message: String,
            duration: Int = Snackbar.LENGTH_SHORT,
        ) {

            snackbar = Snackbar.make(v, message, duration).apply {
                setTextColor(ContextCompat.getColor(context, R.color.boris_color))
                setBackgroundTint(Color.WHITE)
                setText(message)
                setMaxInlineActionWidth(2)
                val sbTv =
                    view.findViewById<TextView>(com.google.android.material.R.id.snackbar_text)
                sbTv.textSize = 21f
                sbTv.maxLines = 2
                sbTv.setTypeface(sbTv.typeface, Typeface.BOLD)
                show()
            }
        }

        fun makeErrorSnackbar(
            v: View,
            message: String,
            duration: Int = Snackbar.LENGTH_INDEFINITE,
        ) {

            snackbar = Snackbar.make(v, message, duration).apply {
                setActionTextColor(Color.BLACK)
                setTextColor(Color.RED)

                setBackgroundTint(Color.WHITE)
                setText(message).setAction("OK") { /*this.dismiss()*/ }
                setMaxInlineActionWidth(2)
                val sbTv =
                    view.findViewById<TextView>(com.google.android.material.R.id.snackbar_text)
                sbTv.textSize = 21f
                sbTv.maxLines = 2
                sbTv.setTypeface(sbTv.typeface, Typeface.BOLD)

                show()
            }

        }

        fun makeInfoSnackbar(
            v: View,
            message: String,
            duration: Int = Snackbar.LENGTH_INDEFINITE,
        ) {

            snackbar = Snackbar.make(v, message, 10000).apply {
                setTextColor(Color.rgb(0, 113, 189))
                setBackgroundTint(Color.WHITE)
                setText(message).setAction("OK") { /*this.dismiss()*/ }
                setActionTextColor(Color.BLACK)

                val sbTv =
                    view.findViewById<TextView>(com.google.android.material.R.id.snackbar_text)
                sbTv.textSize = 21f
                sbTv.maxLines = 2
                sbTv.setTypeface(sbTv.typeface, Typeface.BOLD)

                show()
            }
        }

        fun makeSnackbar(
            v: View,
            message: String,
            duration: Int = Snackbar.LENGTH_LONG,
        ) {
            snackbar = Snackbar.make(v, message, duration).apply {
                setActionTextColor(Color.BLACK)
                setBackgroundTint(Color.WHITE)
                setTextColor(Color.rgb(0, 113, 189))
                val sbTv =
                    view.findViewById<TextView>(com.google.android.material.R.id.snackbar_text)
                sbTv.textSize = 21f
                sbTv.maxLines = 2
                sbTv.setTypeface(sbTv.typeface, Typeface.BOLD)

                show()
            }
        }

        fun dismissSnackbar() {
            snackbar?.let {
                if (it.isShown) it.dismiss()
            }
        }

        fun makeToast(context: Context, msg: String, duration: Int = Toast.LENGTH_LONG) {
            Toast.makeText(context, msg, Toast.LENGTH_LONG).show()
        }

        fun showLocationAlertFragment(
            manager: FragmentManager,
        ) {
            val alertSheet = LocationAlertDialogFragment.newInstance().apply {
                arguments = bundleOf(
                    Pair(AlertDialogFragment.TITLE, "Location is turned off"),
                    Pair(
                        AlertDialogFragment.BODY,
                        "Location must be enabled for bluetooth scanning and location tracking"
                    )
                )
            }

            alertSheet.isCancelable = false
            alertSheet.show(manager, "LocationAlertFragment")
        }

        fun showAlertFragment(manager: FragmentManager, title: String, message: String) {
            val alertSheet = AlertDialogFragment.newInstance().apply {
                arguments = bundleOf(
                    Pair(AlertDialogFragment.TITLE, title), Pair(AlertDialogFragment.BODY, message)
                )
            }

            alertSheet.isCancelable = false
            alertSheet.show(manager, "AlertFragment")

        }

        fun showAlertDialog(
            manager: FragmentManager,
            title: String,
            message: String
        ): AlertDialogFragment {
            val alertSheet = AlertDialogFragment.newInstance().apply {
                arguments = bundleOf(
                    Pair(AlertDialogFragment.TITLE, title), Pair(AlertDialogFragment.BODY, message)
                )
            }
            alertSheet.isCancelable = false
            alertSheet.show(manager, "AlertFragment")
            return alertSheet
        }

        fun showBluetoothRefreshAlert(context: Context) {
            bluetoothRefreshAlert =
                androidx.appcompat.app.AlertDialog.Builder(context).create()
            bluetoothRefreshAlert?.apply {
                val layoutInflater = LayoutInflater.from(context)
                val promptView: View = layoutInflater.inflate(R.layout.refresh_layout, null)
                setView(promptView)
                setCancelable(false)
                show()
            }
        }

        fun hideBluetoothRefreshAlert() {
            bluetoothRefreshAlert?.apply {
                if (isShowing) {
                    dismiss()
                }
            }
        }

        fun getCurrentDate(): String {
            val c = Calendar.getInstance().time
            val df = SimpleDateFormat("dd-MMM-yyyy", Locale.getDefault())
            return df.format(c)
        }

        fun getBatteryPercentage(context: Context): Int {
            val iFilter = IntentFilter(Intent.ACTION_BATTERY_CHANGED)
            val batteryStatus = context.registerReceiver(null, iFilter)
            val level = batteryStatus?.getIntExtra(BatteryManager.EXTRA_LEVEL, -1) ?: -1
            val scale = batteryStatus?.getIntExtra(BatteryManager.EXTRA_SCALE, -1) ?: -1
            val batteryPct = level / scale.toFloat()

            return (batteryPct * 100).toInt()
        }

        fun getMacAddress(): String {
            // TODO: 22/03/2020 delete this line when finished testing!
            //if(1>0) return "8C:DD:76:49:33:E3";  //EYAL
            //if (1 > 0) return "00:08:22:3c:b8:fb";  //BORIS
            //if (1 > 0) return "08:28:b9:bd:49:60";
            //if (1 > 0) return "12:12:12:12:12:12";
            //if (1 > 0) return "1212";

            try {
                val all: List<NetworkInterface> =
                    Collections.list(NetworkInterface.getNetworkInterfaces())
                for (nif in all) {
                    if (!nif.name.equals("wlan0", ignoreCase = true)) continue
                    val macBytes = nif.hardwareAddress ?: return ""
                    val res1 = StringBuilder()
                    for (b in macBytes) {
                        // res1.append(Integer.toHexString(b & 0xFF) + ":");
                        res1.append(String.format("%02X:", b))
                    }
                    if (res1.isNotEmpty()) {
                        res1.deleteCharAt(res1.length - 1)
                    }
                    return res1.toString()
                }
            } catch (ex: Exception) {
                //handle exception
            }
            return ""
        }

        /**
         * Check if network is available
         */
        fun isInternetAvailable(context: Context): Boolean {
            var result = false
            val connectivityManager =
                context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                val networkCapabilities = connectivityManager.activeNetwork ?: return false
                val actNw =
                    connectivityManager.getNetworkCapabilities(networkCapabilities) ?: return false
                result = when {
                    actNw.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) -> true
                    actNw.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) -> true
                    actNw.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET) -> true
                    else -> false
                }
            } else {
                connectivityManager.activeNetworkInfo?.run {
                    result = when (type) {
                        ConnectivityManager.TYPE_WIFI -> true
                        ConnectivityManager.TYPE_MOBILE -> true
                        ConnectivityManager.TYPE_ETHERNET -> true
                        else -> false
                    }

                }
            }
            return result
        }

        @Deprecated("not in use")
        fun isNetworkAvailable(context: Context?): Boolean {
            if (context == null) return false
            val connectivityManager =
                context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                val capabilities =
                    connectivityManager.getNetworkCapabilities(connectivityManager.activeNetwork)
                if (capabilities != null) {
                    when {
                        capabilities.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) -> {
                            return true
                        }
                        capabilities.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) -> {
                            return true
                        }
                        capabilities.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET) -> {
                            return true
                        }
                    }
                }
            } else {
                val activeNetworkInfo = connectivityManager.activeNetworkInfo
                if (activeNetworkInfo != null && activeNetworkInfo.isConnected) {
                    return true
                }
            }
            return false
        }

        fun isLocationEnabled(context: Context): Boolean {
            val locationManager =
                context.getSystemService(Context.LOCATION_SERVICE) as LocationManager
            return LocationManagerCompat.isLocationEnabled(locationManager)
        }

        fun createNotificationChannel(context: Context) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                val channel = NotificationChannel(
                    NOTIFICATION_CHANNEL_ID,
                    NOTIFICATION_CHANNEL_NAME,
                    NotificationManager.IMPORTANCE_HIGH
                )

                NotificationManagerCompat.from(context).createNotificationChannel(channel)
            }
        }

        fun getCurrentDayOfWeek(): String {
            val calendar = Calendar.getInstance()
            return when (calendar[Calendar.DAY_OF_WEEK]) {
                Calendar.SUNDAY -> {
                    "Sunday"
                }
                Calendar.MONDAY -> {
                    "Monday"
                }
                Calendar.TUESDAY -> {
                    "Tuesday"
                }
                Calendar.WEDNESDAY -> {
                    "Wednesday"
                }
                Calendar.THURSDAY -> {
                    "Thursday"
                }
                Calendar.FRIDAY -> {
                    "Friday"
                }
                Calendar.SATURDAY -> {
                    "Saturday"
                }
                else -> ""
            }
        }

        fun getDateFromTimestamp(timestamp: Long, context: Context): String {
            val formatter = if (DateFormat.is24HourFormat(context)) SimpleDateFormat(
                "MM/dd HH:mm",
                Locale.ENGLISH
            ) else SimpleDateFormat("MM/dd h:mm aa", Locale.US)
            return formatter.format(Date(timestamp * 1000))
        }

        fun showVideoAlertDialog(
            manager: FragmentManager
        ): VideoAlertDialogFragment {
            val alertSheet = VideoAlertDialogFragment.newInstance()
            alertSheet.isCancelable = false
            alertSheet.show(manager, "VideAlertFragment")
            return alertSheet
        }

        fun calculateCalibrationSet(value: Int): Int {
            return when {
                4 <= value ->  {
                    if (value.mod(3) == 0) return 3
                    else value.mod(3)
                }
                else -> value
            }
        }
        fun calculateCalibrationSetForError(value: Int): Int {
            return when {
                4 <= value ->  {
                    if (value.mod(3) == 0) return 3
                    else value.mod(3)
                }
                1 == value -> 0
                else -> value
            }
        }
    }
}


