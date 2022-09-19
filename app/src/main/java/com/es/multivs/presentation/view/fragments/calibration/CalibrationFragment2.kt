package com.es.multivs.presentation.view.fragments.calibration

import android.bluetooth.BluetoothDevice
import android.content.Context
import android.os.Bundle
import android.os.CountDownTimer
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.fragment.app.commit
import androidx.lifecycle.lifecycleScope
import com.bumptech.glide.Glide
import com.bumptech.glide.load.resource.drawable.DrawableTransitionOptions
import com.es.multivs.R
import com.es.multivs.data.utils.AppUtils
import com.es.multivs.data.utils.Constants
import com.es.multivs.data.utils.MeasurementUtils
import com.es.multivs.data.utils.SharedPrefs
import com.es.multivs.databinding.CalibrationsFragment2Binding
import com.es.multivs.presentation.view.fragments.calibration.CalibrationFragment.Companion.BELT_MIDDLE
import com.es.multivs.presentation.view.fragments.calibration.CalibrationFragment.Companion.BLUETOOTH_DEVICE_KEY
import com.es.multivs.presentation.view.fragments.calibration.CalibrationFragment.Companion.BP_MAC
import com.es.multivs.presentation.view.fragments.calibration.CalibrationFragment.Companion.ES0008_MAC
import com.es.multivs.presentation.view.fragments.calibration.CalibrationFragment.Companion.PATCH_MIDDLE
import com.es.multivs.presentation.view.fragments.calibration.CalibrationFragment.Companion.PATCH_UPPER_LEFT
import com.es.multivs.presentation.view.viewmodels.MultiVsViewModel
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.util.concurrent.TimeUnit

@AndroidEntryPoint
class CalibrationFragment2 : Fragment() {

    lateinit var mBusyCallback: BusyCallback
    private lateinit var countDownTimer: CountDownTimer
    private lateinit var calibrationTimer: CountDownTimer

    var wornCorrectly = false
    private lateinit var mBinding: CalibrationsFragment2Binding
    private var isTesting: Boolean = false
    private var isCountdownTimer = true

    //    private val multiVsViewModel: MultiVsViewModel by navGraphViewModels(R.id.navigation)
    private val multiVsViewModel: MultiVsViewModel by activityViewModels()

    private var mBluetoothDevice: BluetoothDevice? = null
    private lateinit var _handler: Handler
    private var wornIncorrectlyCounter: Int = 0

    private var mIsConnected = true
    private var mWornCorrectlyCounter = 0

    companion object {
        fun newInstance() = CalibrationFragment2()
        const val TAG = "CalibrationFragment2"
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        mBusyCallback = parentFragment as BusyCallback
        arguments?.getParcelable<BluetoothDevice>(BLUETOOTH_DEVICE_KEY)?.let {
            mBluetoothDevice = it
            multiVsViewModel.lastDevice = it
        }

        arguments?.getString(ES0008_MAC)?.let {
            multiVsViewModel.es008Mac = it
        }

        arguments?.getString(BP_MAC)?.let {
            multiVsViewModel.bpMac = it
        }

        mBinding = CalibrationsFragment2Binding.inflate(inflater, container, false)
       //mBinding.instructions.text = getString(R.string.connecting_device_please_wait)
        mBusyCallback.onInstruction(getString(R.string.connecting_device_please_wait))
        return mBinding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        Log.d("_LIFECYCLE", "onViewCreated: $TAG")
        _handler = Handler(Looper.getMainLooper())
        mBinding.closeBtn.setOnClickListener {
            _handler.removeCallbacks(deviceConnectRunnable)
            mBusyCallback.onClose("CalibrationFragment2")
        }

        lifecycleScope.launch {
            val devicePosition = multiVsViewModel.getCalibrationDevicePositionAsync()
            displayBlueMan(devicePosition.await())
            /*   if (!multiVsViewModel.wasConnected) {*/
        }
//        mBinding.instructions.text = getString(R.string.connecting_multivs)

        mBusyCallback.onResponse()
        initObservers()
        initListeners()
    }

    private fun initListeners() {
        mBinding.blueMan.setOnClickListener {
            if (::calibrationTimer.isInitialized)
                calibrationTimer.cancel()
            initializeIsWornCorrectlyObserver()
            //mBinding.instructions.text = getString(R.string.checking_device_placement)
            mBusyCallback.onInstruction(getString(R.string.checking_device_placement))
            it.visibility = View.GONE
        }
    }

    private fun startCalibrationTimer() {
        val time= when {
            (SharedPrefs.getIntParam(Constants.Measurement, 0) == 3 && SharedPrefs.getIntParam(Constants.CalibrationSet, 0) == 1) -> 10
            (SharedPrefs.getIntParam(Constants.Measurement, 0) == 3 && SharedPrefs.getIntParam(Constants.CalibrationSet, 0) == 2) -> 5
            else -> 30
        }
        val millis = when {
            (SharedPrefs.getIntParam(Constants.Measurement, 0) == 3 && SharedPrefs.getIntParam(Constants.CalibrationSet, 0) == 1) -> TimeUnit.MINUTES.toMillis(10)
            (SharedPrefs.getIntParam(Constants.Measurement, 0) == 3 && SharedPrefs.getIntParam(Constants.CalibrationSet, 0) == 2) -> TimeUnit.MINUTES.toMillis(5)
            else -> TimeUnit.SECONDS.toMillis(30)
        }
                mBinding.tvInfo.visibility = View.VISIBLE
               // val millis = TimeUnit.SECONDS.toMillis(30)
                calibrationTimer = object : CountDownTimer(millis, 1000) {
                    override fun onTick(millis: Long) {
                        val sec = (millis / 1000).toInt() % 60
                        val min = (millis / (1000 * 60) % 60).toInt()
                        val hours = (millis / (1000 * 60 * 60) % 24).toInt()
                        Log.e("timer", String.format("%02d:%02d:%02d", hours, min, sec))
                        if (time==10 || time== 5)
                            mBinding.tvInfo.text = "Next calibration in ${String.format("%02d:%02d",min,sec)} seconds"
                        else  mBinding.tvInfo.text = "Next calibration in ${String.format("%02d",sec)} seconds"

                    }

                    override fun onFinish() {
                        mBinding.tvInfo.visibility = View.GONE
                        mBinding.blueMan.performClick()
                    }
                }
                calibrationTimer.start()
    }

    private fun initializeIsWornCorrectlyObserver() {
        // mBinding.connectProgress.visibility = View.VISIBLE
        // multiVsViewModel.isWornCorrectly.observe(viewLifecycleOwner) { wornCorrectly ->
        try {
            if (wornCorrectly) {
                mWornCorrectlyCounter++
                multiVsViewModel.isWornCorrectly.removeObservers(viewLifecycleOwner)
                multiVsViewModel.isConnected.removeObservers(viewLifecycleOwner)

                mBinding.connectProgress.visibility = View.INVISIBLE
//                    Navigation.findNavController(mBinding.instructions)
                mBusyCallback.onInstruction("")
//                        .navigate(R.id.action_calibrationFragment2_to_calibrationFragment3)
                parentFragmentManager.commit {
                    setCustomAnimations(
                        R.anim.fade_in,
                        R.anim.fade_out,
                        R.anim.fade_in,
                        R.anim.fade_out
                    )
                    replace(
                        R.id.calibration_container,
                        CalibrationFragment3.newInstance(),
                        CalibrationFragment3.TAG
                    )
//                        remove(parentFragmentManager.findFragmentByTag("CalibrationFragment2")!!)
//                        add(
//                            R.id.calibration_container,
//                            CalibrationFragment3.newInstance(),
//                            "CalibrationFragment3"
//                        )
                }

                //  }
//            if (mIsConnected)
//                _handler.postDelayed(deviceConnectRunnable, 5000)
//            mIsConnected = false
            } else {
                mIsConnected = true
                mWornCorrectlyCounter = 0
                _handler.postDelayed(positionFaultRunnable, 15000)
            }
        }catch (e:Exception){
         e.printStackTrace()
        }
        //}
    }

    private fun interpretSensorPositionOnBody(isWornCorrectly: Boolean) {

        MeasurementUtils.interpretSensorPositionOnBody(
            isTesting, isWornCorrectly
        ) { status ->
            when (status) {
                MeasurementUtils.TESTING_OK -> {

                }
                MeasurementUtils.TESTING_FAIL -> {
                    isTesting = false
                    mBinding.blueMan.visibility = View.INVISIBLE
                    //mBinding.instructions.text = getString(R.string.checking_device_placement)
                    mBusyCallback.onInstruction(getString(R.string.checking_device_placement))
                    if (wornIncorrectlyCounter > 2) {
                        AppUtils.makeErrorSnackbarNoAction(
                            mBinding.instructions,
                            "The device is not properly attached.",
                        )
                        wornIncorrectlyCounter = 0
                    } else {
                        wornIncorrectlyCounter++
                    }
                }
                MeasurementUtils.IDLE_FAIL -> {
                    Log.e("Result", "IDLE_FAIL")
                    if (isCountdownTimer) {
                        startTimer()
                    }
                    if (!mIsConnected){
                        if (::calibrationTimer.isInitialized)
                            calibrationTimer.cancel()
                        mBinding.tvInfo.visibility = View.GONE
                    }
                    mIsConnected = true
                    isCountdownTimer = false
                    isTesting = false
                    mBinding.blueMan.visibility = View.INVISIBLE
                    mBinding.instructions.text = getString(R.string.checking_device_placement)
                    mBusyCallback.onInstruction("")
                    if (wornIncorrectlyCounter > 2) {
                        AppUtils.makeErrorSnackbarNoAction(
                            mBinding.instructions,
                            "The device is not properly attached.",
                        )
                        wornIncorrectlyCounter = 0
                    } else {
                        wornIncorrectlyCounter++
                    }
                }
                MeasurementUtils.IDLE_OK -> {
                    Log.e("Result", "ok")
                    mBinding.blueMan.visibility = View.VISIBLE
                    mBusyCallback.onInstruction("")
                    mBinding.instructions.text =
                        getString(R.string.multi_vs_init_instructions_calibration)

                    if (mIsConnected) {
                        if (::calibrationTimer.isInitialized)
                            calibrationTimer.cancel()
                        startCalibrationTimer()
                        _handler.postDelayed(deviceConnectRunnable, 1000)
                    }
                    mIsConnected = false
                    _handler.post {
                        // mBusyCallback.onBusy(false)
                    }
                }
            }
        }

    }

    private var positionFaultRunnable = Runnable {
        AppUtils.makeErrorSnackbarNoAction(
            mBinding.instructions,
            "The MULTIVS is not properly attached"
        )
    }

    private var deviceConnectRunnable = Runnable {
        try {
            AppUtils.makeShowMessage(
                mBinding.instructions,
                "MULTIVS is connected,on body"
            )
        } catch (e: Exception) {
            print(e.message)
        }
    }

    private var connectionErrorRunnable = Runnable {
        AppUtils.makeErrorSnackbar(mBinding.instructions, "Could not connect to MULTIVS device")
        multiVsViewModel.closeDevice()
        mBusyCallback.onClose("CalibrationFragment2")

    }

    private fun displayBlueMan(devicePosition: String) {
        lifecycleScope.launch(Dispatchers.Main) {
            when (devicePosition) {
                PATCH_UPPER_LEFT -> {
                    Glide.with(requireActivity()).load(R.drawable.patch_upper_left)
                        .transition(DrawableTransitionOptions.withCrossFade())
                        .into(mBinding.blueMan)
                }
                BELT_MIDDLE -> {
                    Glide.with(requireActivity()).load(R.drawable.belt_middle)
                        .transition(DrawableTransitionOptions.withCrossFade())
                        .into(mBinding.blueMan)
                }
                PATCH_MIDDLE -> {
                    Glide.with(requireActivity()).load(R.drawable.patch_middle)
                        .transition(DrawableTransitionOptions.withCrossFade())
                        .into(mBinding.blueMan)
                }
            }
        }
    }

    private fun initObservers() {

        multiVsViewModel.isConnected.observe(viewLifecycleOwner) { connected ->
            if (connected) {
                /*mBinding.instructions.text =
                    getString(R.string.multivs_connected_testing_position)*/
                mBusyCallback.onInstruction(getString(R.string.multivs_connected_testing_position))
                multiVsViewModel.isWornCorrectly.observe(viewLifecycleOwner) { wornCorrectly ->
                    this.wornCorrectly = wornCorrectly
                    interpretSensorPositionOnBody(wornCorrectly)
                }
                _handler.removeCallbacks(connectionErrorRunnable)

//                mBinding.blueMan.visibility = View.VISIBLE
//                mBinding.instructions.text = getString(R.string.multi_vs_init_instructions_calibration)
//                _handler.post {
//                    mBusyCallback.onBusy(false)
//                }

            } else {
                mIsConnected = true
                Log.e("CalibrationFragment2", "MULTIVS device seems to be disconnected")
            }
        }


    }

    override fun onPause() {
        super.onPause()
        _handler.removeCallbacks(connectionErrorRunnable)
        _handler.removeCallbacks(positionFaultRunnable)
        Log.d("_LIFECYCLE", "onPause: $TAG")
    }

    override fun onResume() {
        Log.d("_SCANNING", "onViewCreated: CalibrationFragment2 - initBleDevice")
        if (multiVsViewModel.multivsActive) {
            // we got here from CalibrationFragment1
            if (mBluetoothDevice != null) {
                // mBusyCallback.onBusy(true, "Connecting to MULTIVS device, please wait")
                _handler.postDelayed(connectionErrorRunnable, 30000)
                multiVsViewModel.initBleDevice(mBluetoothDevice!!)
            } else {
//                Log.d("_SCANNING", "onViewCreated: connecting to last ble device")
//                multiVsViewModel.initLastBleDevice()
            }

        }
        super.onResume()

        Log.d("_LIFECYCLE", "onResume: $TAG")
    }

    override fun onStart() {
        super.onStart()
        Log.d("_LIFECYCLE", "onStart: $TAG")
    }

    override fun onDestroyView() {
        super.onDestroyView()
        multiVsViewModel.closeDevice()
        multiVsViewModel.clearHandler()
        if (::countDownTimer.isInitialized)
            countDownTimer.cancel()
        Log.d("_LIFECYCLE", "onDestroyView: $TAG")
    }

    override fun onDestroy() {
        super.onDestroy()
        if (::countDownTimer.isInitialized)
            countDownTimer.cancel()
        Log.d("_LIFECYCLE", "onDestroy: $TAG")
    }

    private fun startTimer() {
        val millis =
            if (Constants.MeasurementDuration == "60 sec") TimeUnit.MINUTES.toMillis(2) else TimeUnit.MINUTES.toMillis(
                1
            )
        countDownTimer = object : CountDownTimer(millis, 1000) {
            override fun onTick(millis: Long) {
                val sec = (millis / 1000).toInt() % 60
                val min = (millis / (1000 * 60) % 60).toInt()
                val hours = (millis / (1000 * 60 * 60) % 24).toInt()
                // Log.e("timer", String.format("%02d:%02d:%02d", hours, min, sec))
            }

            override fun onFinish() {
                Constants.isDisConnectDevice = true
                closeDevice()
            }
        }
        countDownTimer.start()
    }

    private fun closeDevice() {
        if (::countDownTimer.isInitialized)
            countDownTimer.cancel()
        multiVsViewModel.clearHandler()
        multiVsViewModel.closeDevice()
        multiVsViewModel.multivsActive = false
        Handler(Looper.getMainLooper()).postDelayed({
            // mBusyCallback.onBusy(false)
            mBusyCallback.onClose("CalibrationFragment2")
        }, 3000)
    }
}