package com.es.multivs.data.utils

import android.Manifest
import android.app.Activity
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothManager
import android.content.Context
import android.content.Intent
import android.location.LocationManager
import android.location.LocationRequest
import android.os.Build
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.ActivityResultRegistry
import androidx.activity.result.IntentSenderRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.location.LocationManagerCompat
import com.google.android.gms.common.api.ResolvableApiException
import com.google.android.gms.location.*
import com.google.android.gms.tasks.Task
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine

/*
* created by marko
Etrog Systems LTD. 10/03/2021.
*/

/**
 * Class for handling bluetooth and location requests.
 * The bluetooth request fires first and then continues to the location request.
 * All requests are done in a coroutine.
 * @param resultRegistry ActivityResultRegistry
 * @see ActivityResultRegistry
 */
class BluetoothAndLocationHandler(private val resultRegistry: ActivityResultRegistry) {
    private val handlers = mutableListOf<ActivityResultLauncher<*>>()

    suspend fun requestBluetoothActivation(): Boolean {
        return suspendCoroutine { continuation ->
            val enableBtIntent = Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE)

            val launcher = resultRegistry.register(
                BLUETOOTH_ON_REQUEST,
                ActivityResultContracts.StartActivityForResult()
            ) { result ->
                continuation.resume(
                    result.resultCode == Activity.RESULT_OK
                )
            }

            handlers.add(launcher)
            launcher.launch(enableBtIntent)
        }
    }

    suspend fun requestLocationPermission(): Boolean {
        return suspendCoroutine { cont ->
            val launcher = resultRegistry.register(
                LOCATION_REQUEST, ActivityResultContracts.RequestPermission()
            ) {
                cont.resumeWith(Result.success(it))
            }
            handlers.add(launcher)
            launcher.launch(Manifest.permission.ACCESS_FINE_LOCATION)
        }
    }

    private suspend fun requestLocationActivation(intentSenderRequest: IntentSenderRequest): Boolean {
        return suspendCoroutine { continuation ->
            val launcher = resultRegistry.register(
                LOCATION_REQUEST,
                ActivityResultContracts.StartIntentSenderForResult()
            ) {
                continuation.resume(it.resultCode == Activity.RESULT_OK)
            }

            handlers.add(launcher)
            launcher.launch(intentSenderRequest)
        }
    }

    suspend fun enableLocation(context: Context): Boolean = suspendCoroutine { cont ->
        val locationSettingsRequest = LocationSettingsRequest.Builder().setNeedBle(true)
            .addLocationRequest(com.google.android.gms.location.LocationRequest.create().apply {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                    priority = LocationRequest.QUALITY_HIGH_ACCURACY
                }
            }).build()

        val client: SettingsClient = LocationServices.getSettingsClient(context)
        val task: Task<LocationSettingsResponse> =
            client.checkLocationSettings(locationSettingsRequest)

        task.addOnSuccessListener {
            cont.resume(true)
        }

        task.addOnFailureListener { exception ->
            if (exception is ResolvableApiException &&
                exception.statusCode == LocationSettingsStatusCodes.RESOLUTION_REQUIRED
            ) {
                val intentSenderRequest =
                    IntentSenderRequest.Builder(exception.resolution).build()

                CoroutineScope(cont.context).launch {
                    val result = requestLocationActivation(intentSenderRequest)
                    cont.resume(result)
                }
            } else {
                cont.resume(false)
            }
        }
    }

    /**
     * Checks if location is enabled
     * @param context Context
     */
    fun isLocationEnabled(context: Context): Boolean {
        val locationManager = context.getSystemService(Context.LOCATION_SERVICE) as LocationManager
        return LocationManagerCompat.isLocationEnabled(locationManager)
    }

    /**
     * Checks if bluetooth is enabled
     * @param context Context
     */
    fun isBluetoothEnabled(context: Context): Boolean {

        val bluetoothManager =
            context.getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager
        return bluetoothManager.adapter.isEnabled
    }

    /**
     * Unregisters the ActivityResultLaunchers
     */
    fun unregisterHandlers() {
        handlers.forEach {
            it.unregister()
        }
    }

    companion object {
        private const val BLUETOOTH_ON_REQUEST = "ES008_BLUETOOTH_REQUEST"
        private const val LOCATION_REQUEST = "ES008_LOCATION_REQUEST"
    }
}