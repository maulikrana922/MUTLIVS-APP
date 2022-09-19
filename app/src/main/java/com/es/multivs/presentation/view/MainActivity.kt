package com.es.multivs.presentation.view

import android.Manifest
import android.annotation.SuppressLint
import android.content.pm.PackageManager
import android.os.Build
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.View
import android.view.WindowManager
import android.widget.Toast
import androidx.activity.result.ActivityResultCallback
import androidx.activity.result.contract.ActivityResultContracts.RequestMultiplePermissions
import androidx.activity.viewModels
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import com.es.multivs.data.utils.AppUtils
import com.es.multivs.data.utils.BluetoothAndLocationHandler
import com.es.multivs.databinding.ActivityMainBinding
import com.es.multivs.presentation.view.fragments.LogInFragment
import com.es.multivs.presentation.view.viewmodels.UserDetailsViewModel
import com.google.android.gms.location.*
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import com.es.multivs.R
import java.util.*


@AndroidEntryPoint
class MainActivity : AppCompatActivity(), LogInFragment.PostFrequencyInterface {

    private lateinit var locationCallback: LocationCallback
    private lateinit var locationRequest: LocationRequest
    private var handler: Handler = Handler(Looper.getMainLooper())
    private lateinit var binding: ActivityMainBinding

    private val userDetailsViewModel: UserDetailsViewModel by viewModels()
    private val permissionRequests = BluetoothAndLocationHandler(activityResultRegistry)
    override fun onCreate(savedInstanceState: Bundle?) {
        setTheme(R.style.base_theme)
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        val decorView = window.decorView

        val flags = (View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                or View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                or View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                or View.SYSTEM_UI_FLAG_FULLSCREEN
                or View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY)

        decorView.systemUiVisibility = flags

        setImmersiveMode()

        binding = ActivityMainBinding.inflate(layoutInflater)
        lifecycleScope.launch {
            permissionRequests.requestBluetoothActivation()
            permissionRequests.requestLocationPermission()
            permissionRequests.enableLocation(this@MainActivity)
            if (permissionRequests.isLocationEnabled(this@MainActivity)) {
                AppUtils.makeToast(this@MainActivity, "Location access has been granted.")
            } else {
                AppUtils.showLocationAlertFragment(supportFragmentManager)
            }
        }
    }

    private fun startLocationRequests(frequency: Long) {

        locationRequest = LocationRequest.create()
        locationRequest.interval = frequency // get location every 20 minutes
        locationRequest.fastestInterval = frequency
        locationRequest.priority = LocationRequest.PRIORITY_HIGH_ACCURACY

        locationCallback = object : LocationCallback() {

            override fun onLocationResult(p0: LocationResult) {
                onResult(p0)
            }
        }

        if (!checkSelfPermissions()) {
            checkLocationPermission()
        }

        lifecycleScope.launch(Dispatchers.IO) {

            LocationServices.getFusedLocationProviderClient(this@MainActivity)
                .requestLocationUpdates(locationRequest, locationCallback, Looper.getMainLooper())
        }
    }

    private fun onResult(locationResult: LocationResult) {
        lifecycleScope.launch {
            userDetailsViewModel.insertUserLocation(locationResult.lastLocation)
            Log.d(
                "PERIODIC_INFO",
                "onSuccess: lat,lon: " + locationResult.lastLocation.latitude + " , " + locationResult.lastLocation.longitude
            )
            val result = userDetailsViewModel.postPeriodicInfo()
            if (result) {
                AppUtils.makeToast(
                    this@MainActivity,
                    getString(R.string.successfully_uploaded_loc_and_battery),
                    duration = Toast.LENGTH_SHORT
                )
            } else {
                AppUtils.makeToast(
                    this@MainActivity,
                    getString(R.string.failed_to_upload_loc_and_battery),
                    duration = Toast.LENGTH_SHORT
                )
            }
        }
    }

    private fun checkSelfPermissions(): Boolean {
        return (ActivityCompat.checkSelfPermission(
            this,
            Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED
                && ActivityCompat.checkSelfPermission(
            this,
            Manifest.permission.ACCESS_COARSE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED)
    }

    private fun checkLocationPermission() {
        if (ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_COARSE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED
        ) {
        } else {
            // The registered ActivityResultCallback gets the result of this request.
            requestPermissionsLauncher.launch(
                arrayOf(
                    Manifest.permission.ACCESS_COARSE_LOCATION,
                    Manifest.permission.ACCESS_FINE_LOCATION
                )
            )
        }
    }

    @SuppressLint("MissingPermission")
    private val requestPermissionsLauncher = registerForActivityResult(RequestMultiplePermissions(),
        ActivityResultCallback { result ->

            val coarseLocationGranted = result[Manifest.permission.ACCESS_COARSE_LOCATION]
            val fineLocationGranted = result[Manifest.permission.ACCESS_FINE_LOCATION]

            if (coarseLocationGranted != null && fineLocationGranted != null) {

                if (coarseLocationGranted && fineLocationGranted) {

                    if (ActivityCompat.checkSelfPermission(
                            this@MainActivity,
                            Manifest.permission.ACCESS_FINE_LOCATION
                        ) != PackageManager.PERMISSION_GRANTED
                        && ActivityCompat.checkSelfPermission(
                            this@MainActivity,
                            Manifest.permission.ACCESS_COARSE_LOCATION
                        ) != PackageManager.PERMISSION_GRANTED
                    ) {

                        return@ActivityResultCallback
                    }

                    LocationServices.getFusedLocationProviderClient(this@MainActivity)
                        .requestLocationUpdates(
                            locationRequest,
                            locationCallback,
                            Looper.getMainLooper()
                        )
                }
            } else {

                Toast.makeText(
                    this@MainActivity,
                    "Location permissions were denied.",
                    Toast.LENGTH_SHORT
                ).show()
            }
        })

    override fun onBackPressed() {
//        super.onBackPressed()
    }

    private fun setImmersiveMode() {

        val currentApiVersion = Build.VERSION.SDK_INT

        val flags = (View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                or View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                or View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                or View.SYSTEM_UI_FLAG_FULLSCREEN
                or View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY)

        // This work only for android 4.4+
        if (currentApiVersion >= Build.VERSION_CODES.KITKAT) {

            window.decorView.systemUiVisibility = flags

//            Code below is to handle presses of Volume up or Volume down.
//            Without this, after pressing volume buttons, the navigation bar will
//            show up and won't hide
//
//            Code below is to handle presses of Volume up or Volume down.
//            Without this, after pressing volume buttons, the navigation bar will
//            show up and won't hide
            val decorView = window.decorView
            decorView
                .setOnSystemUiVisibilityChangeListener { visibility ->
                    if (visibility and View.SYSTEM_UI_FLAG_FULLSCREEN == 0) {
                        decorView.systemUiVisibility = flags
                    }
                }
        }

    }

    private fun hideAndroidApplicationBar() {
        val view = window.decorView

        // Hide Android Soft keys
        view.systemUiVisibility = (View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                or View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                or View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                or View.SYSTEM_UI_FLAG_HIDE_NAVIGATION // hide navigation bar
                or View.SYSTEM_UI_FLAG_FULLSCREEN // hide status bar
                or View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY)
    }

    override fun onResume() {
        super.onResume()
        hideAndroidApplicationBar()
    }

    override fun onPause() {
        super.onPause()
        handler.removeCallbacksAndMessages(null)
    }

    override fun onDestroy() {
        super.onDestroy()
        permissionRequests.unregisterHandlers()
        LocationServices.getFusedLocationProviderClient(this)
            .removeLocationUpdates(locationCallback)
    }

    override fun onFrequencyReceived(frequency: Int) {
        startLocationRequests((frequency * 1000).toLong())
    }
}