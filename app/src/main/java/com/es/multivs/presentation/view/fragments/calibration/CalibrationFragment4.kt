package com.es.multivs.presentation.view.fragments.calibration

import android.content.Context.MODE_PRIVATE
import android.os.Bundle
import android.os.CountDownTimer
import android.os.Handler
import android.os.Looper
import android.text.SpannableString
import android.text.Spanned
import android.text.style.ForegroundColorSpan
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.fragment.app.commit
import androidx.fragment.app.viewModels
import androidx.lifecycle.Observer
import androidx.lifecycle.lifecycleScope
import com.es.multivs.R
import com.es.multivs.data.bledevices.BleDeviceTypes
import com.es.multivs.data.bledevices.multivs.PatchData
import com.es.multivs.data.network.Resource
import com.es.multivs.data.utils.*
import com.es.multivs.data.utils.AppUtils.Companion.calculateCalibrationSet
import com.es.multivs.databinding.CalibrationsFragment4Binding
import com.es.multivs.presentation.view.fragments.calibration.CalibrationFragment3.Companion.DIA
import com.es.multivs.presentation.view.fragments.calibration.CalibrationFragment3.Companion.SYS
import com.es.multivs.presentation.view.viewmodels.BluetoothViewModel
import com.es.multivs.presentation.view.viewmodels.BpViewModel
import com.es.multivs.presentation.view.viewmodels.MultiVsViewModel
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.util.concurrent.TimeUnit
import javax.inject.Inject


/**
 * Created by Marko on 10/31/2021.
 * Etrog Systems LTD.
 */
@AndroidEntryPoint
class CalibrationFragment4 : Fragment() {

    lateinit var mBusyCallback: BusyCallback
    private lateinit var countDownTimer: CountDownTimer

    private val multiVsViewModel: MultiVsViewModel by activityViewModels()
    private val bluetoothViewModel: BluetoothViewModel by viewModels()
    private val bpViewModel: BpViewModel by viewModels()
    private var _handler: Handler = Handler(Looper.getMainLooper())

    private lateinit var mBinding: CalibrationsFragment4Binding
    private var mNotWornCorrectlyCounter: Int = 0
    private var _ecgSampleCounter = 0
    private var _ppgSampleCounter = 0
    private var _isBpWorking = false
    private var preTestTimer: CountDownTimer? = null

    private var cuff_type = ""
    private var isClose = false

    @Inject
    lateinit var scheduleUpdates: ScheduleUpdatesManager

    companion object {
        fun newInstance(): CalibrationFragment4 = CalibrationFragment4()
        const val TAG = "CalibrationFragment4"
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        mBusyCallback = parentFragment as BusyCallback
        mBinding = CalibrationsFragment4Binding.inflate(inflater, container, false)

        return mBinding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {

        initWornCorrectlyObserver()
        initMultiVsObservers()

        val isUseDevice = arguments?.getBoolean(CalibrationFragment3.USE_BP_DEVICE, false)

        val sys = arguments?.getInt(SYS, -1)
        val dia = arguments?.getInt(DIA, -1)
        if (sys != -1 && dia != -1) {
            mBinding.bpManualText.visibility = View.VISIBLE
            mBinding.bpManualText.text = getString(R.string.manually_entered_sys_dia_mmhg, sys, dia)
        }

        Log.d("_BP", "onViewCreated: isUseDevice before null check: $isUseDevice")
        isUseDevice?.let { useDevice ->
            Log.d("_BP", "onViewCreated: isUseDevice: $isUseDevice")
            if (useDevice) {
                makeSpannableText()
                mBinding.actionBtn.visibility = View.GONE
                initBluetoothViewModel()
            } else {
                mBinding.actionBtn.visibility = View.VISIBLE
            }
        }

        mBinding.actionBtn.setOnClickListener {

            if (mBinding.actionBtn.text == getString(R.string.start_calibration)) {
                mBinding.actionBtn.text = getString(R.string.cancel_calibration)
                onStartTest()
                //multiVsViewModel.postCalibrationData()
            } else if (mBinding.actionBtn.text == getString(R.string.cancel_calibration)) {
                if (::countDownTimer.isInitialized)
                    countDownTimer.cancel()
                AppUtils.makeErrorSnackbar(
                    mBinding.actionBtn,
                    getString(R.string.calibration_cancelled)
                )
                onCancelCalibration()
            }
        }

        mBinding.closeBtn.setOnClickListener {
            if (::countDownTimer.isInitialized)
                countDownTimer.cancel()
            bluetoothViewModel.stopScan()
            mBusyCallback.onClose("CalibrationFragment4")
        }
    }

    private fun onStartTest() {
        val count = if (Constants.CalibrationDuration == "60 sec") 24500 else 12000
        mBinding.calibrationProgressBar.max = count
        multiVsViewModel.startTest(true)
        initPreTestTimer()
    }

    private fun initBluetoothViewModel() {
        Log.d("_BP", "initBluetoothViewModel: fetching device: ${multiVsViewModel.bpMac!!}")
        bluetoothViewModel.startScan(multiVsViewModel.bpMac!!)
        bluetoothViewModel.bluetoothDevice.observe(viewLifecycleOwner) { device ->
            device?.let {
                Log.d("_BP", "initBluetoothViewModel: got device: $it")
                bpViewModel.initializeDevice()
                initBpObservers()
                bpViewModel.connectDevice(it.address)
                bluetoothViewModel.stopScan()
            }
        }
    }


//    private var isCalibrationBusy = false

    private fun initBpObservers() {
        val count = if (Constants.CalibrationDuration == "60 sec") 24500 else 12000
        mBinding.calibrationProgressBar.max = count
        bpViewModel.isBioLightMeasuring.observe(viewLifecycleOwner) { isMeasuring ->
            Log.d("_BP", "isMeasuring: $isMeasuring ")
            Log.d("_BP", "isBpStarted: $_isBpWorking ")
            isMeasuring?.let {
                if (isMeasuring) {
                    if (!_isBpWorking) {

                        showBpActive()

//                        isCalibrationBusy = true
                        _isBpWorking = true

                        _ecgSampleCounter = 0
                        _ppgSampleCounter = 0
                        mBinding.calibrationProgressBar.progress = 0

                        mBinding.actionBtn.text = getString(R.string.cancel_calibration)
                        mBinding.actionBtn.visibility = View.VISIBLE

                        multiVsViewModel.startTest(true)
                        initPreTestTimer()
                    }
                } else {
                    Log.d("_BP", "initBpObservers: is measuring: " + false)

                    hideBpActive()

                    if (_isBpWorking && (_ecgSampleCounter + _ppgSampleCounter > 0) || isCountDownTimerRunning) {
                        mBinding.instructions.text =
                            getString(R.string.blood_pressure_was_stopped_calibration_cancel)

                        AppUtils.showAlertFragment(
                            parentFragmentManager,
                            getString(R.string.multi_vs_calibration),
                            getString(R.string.blood_pressure_was_stopped_calibration_cancel)
                        )
                        onCancelCalibration()
                    }
                }
            }
        }

        bpViewModel.bpResults.observe(viewLifecycleOwner) {

            multiVsViewModel.mSys = it.getSys()
            multiVsViewModel.mDia = it.getDia()
        }

        bpViewModel.isFinished.observe(viewLifecycleOwner) {
            _isBpFinished = it
            _isBpWorking = false
            hideBpActive()
        }
    }

    private var isBpActiveShowing = false

    private fun showBpActive() {
        if (!isBpActiveShowing) {
            Log.d("_SCANNING", "showBpActive: ")
            mBinding.includeBpActive.motionBpActiveLayout.visibility = View.VISIBLE
            isBpActiveShowing = true
        }
    }

    private fun hideBpActive() {
        if (isBpActiveShowing) {
            Log.d("_SCANNING", "hideBpActive: ")
            mBinding.includeBpActive.motionBpActiveLayout.visibility = View.INVISIBLE
            isBpActiveShowing = false
        }
    }

    private var isMultiVsShowing = false

    private fun showMultiVsActive() {
        if (!isMultiVsShowing) {
            Log.d("_SCANNING", "showMultiVsActive: ")
            mBinding.includeMultivsActive.motionMultivsActive.visibility = View.VISIBLE
        }
        isMultiVsShowing = true
    }

    private fun hideMultiVsActive() {
        if (isMultiVsShowing) {
            Log.d("_SCANNING", "hideMultiVsActive: ")
            mBinding.includeMultivsActive.motionMultivsActive.visibility = View.INVISIBLE
            isMultiVsShowing = false
        }
    }

    private var _isBpFinished = false

    var isCountDownTimerRunning = false
    private fun initPreTestTimer() {

        preTestTimer = object : CountDownTimer(10000L, 1000) {
            override fun onTick(l: Long) {
                isCountDownTimerRunning = true
                mBinding.instructions.text = "${getString(R.string.starting_test_in)} ${l / 1000}"
            }

            override fun onFinish() {
                startTimer()
                isCountDownTimerRunning = false
                mBinding.instructions.text = getString(R.string.calibration_instructions)
            }
        }
        preTestTimer?.start()
    }

    private fun saveSharedPrefrence(calculateCalibrationSet: Int, measurement: Int) {
        Log.e("calibration","$calculateCalibrationSet,$measurement")
        SharedPrefs.setIntParam(Constants.CalibrationSet,calculateCalibrationSet)
        SharedPrefs.setIntParam(Constants.Measurement,measurement)
    }

    private fun initMultiVsObservers() {
        multiVsViewModel.calibrationResultLiveData.observe(requireActivity()) { results ->
            Log.e("data",results?.calibration_result.toString())
                results?.let {
                    mBusyCallback.onBusy(false)
                    mBusyCallback.onResponse()
                        mBinding.instructions.text =
                            getString(R.string.successully_uploaded)
                    when (results.calibration_result) {
                        "Calibration is not valid. please restart the process" -> {
                            saveSharedPrefrence(0, 0)

                            _isBpWorking = false
                            _ecgSampleCounter = 0
                            _ppgSampleCounter = 0
                            preTestTimer?.cancel()
                            resetCalibrationProcess()
                            scheduleUpdates.calibrationComplete()
                        }
                        "self-test failed, need more data" -> {

                            _isBpWorking = false
                            _ecgSampleCounter = 0
                            _ppgSampleCounter = 0
                            preTestTimer?.cancel()
                            resetCalibrationProcess()
                            scheduleUpdates.calibrationComplete()

                        }
                        "Successfull" -> {
                            AppUtils.makeInfoSnackbar(
                                mBinding.instructions,
                                getString(R.string.successully_uploaded)
                            )
                            saveSharedPrefrence(0, 0)
                            mBusyCallback.onClose(TAG)
                        }
                        else -> {
                            AppUtils.makeInfoSnackbar(
                                mBinding.instructions,
                                getString(R.string.successully_uploaded)
                            )

                            _isBpWorking = false
                            _ecgSampleCounter = 0
                            _ppgSampleCounter = 0
                            preTestTimer?.cancel()
                            resetCalibrationProcess()

                            scheduleUpdates.calibrationComplete()
                        }
                    }
                }
        }

        multiVsViewModel.calibrationResults.observe(viewLifecycleOwner) { results ->
            results?.let {
                when (it.status) {
                    Status.LOADING -> {
                        mBusyCallback.onBusy(true, it.msg)
                    }
                    Status.SUCCESS -> {
                        mBusyCallback.onBusy(false)
                        mBusyCallback.onResponse()
                       // AppUtils.makeInfoSnackbar(mBinding.instructions, it.msg)
                        _isBpWorking = false

                        _ecgSampleCounter = 0
                        _ppgSampleCounter = 0

                        preTestTimer?.cancel()
                        resetCalibrationProcess()

                        scheduleUpdates.calibrationComplete()

//                        MainActivity.CALIBRATION_TASK_COMPLETE = true
//                        MainActivity.SHOULD_UPDATE_COMPLETION = true
//                        HealthHubFragment.calibrationTaskCompleted = true
//                        HealthHubFragment.shouldUpdate = true
                    }
                    Status.ERROR -> {
                        mBusyCallback.onResponse()
                        mBusyCallback.onBusy(false)
                        Log.d("_SCANNING", "calibrationResults: ERROR: ${it.msg}")

                        AppUtils.showAlertFragment(
                            parentFragmentManager,
                            getString(R.string.multi_vs_calibration),
                            it.msg
                        )
//                        AppUtils.makeErrorSnackbar(mBinding.instructions, it.msg)

                        _isBpWorking = false

                        _ecgSampleCounter = 0
                        _ppgSampleCounter = 0

                        preTestTimer?.cancel()
                        resetCalibrationProcess()
                    }
                }
                multiVsViewModel.onResultsReceived()
            }
        }

        multiVsViewModel.getIsBadData().observe(viewLifecycleOwner) { isBad ->
            if (isBad && !multiVsViewModel.isPosting) {
                Log.d("_SCANNING", "initMultiVsObservers: BADDATA: $isBad")

//                AppUtils.makeErrorSnackbar(
//                    mBinding.instructions,
//                    getString(R.string.bad_data_recalibrate)
//                )

                AppUtils.showAlertFragment(
                    parentFragmentManager,
                    getString(R.string.multi_vs_calibration),
                    getString(R.string.bad_data_recalibrate)
                )

                mBinding.instructions.text = getString(R.string.bad_data_recalibrate)

                multiVsViewModel.resetBadData()
                onCancelCalibration()
            }
        }

        multiVsViewModel.getPatchData().observe(viewLifecycleOwner, object : Observer<PatchData> {
            override fun onChanged(patchData: PatchData?) {
                if (patchData != null) {

                    Log.d("_SCANNING", "UI RECEIVED PATCH DATA: $patchData")
                    showMultiVsActive()

                    if (patchData.isEcg) {
                        _ecgSampleCounter += patchData.ecgSampleList.size
                    }
                    if (patchData.isPpg) {
                        _ppgSampleCounter += patchData.ppgSampleList.size
                    }

                    val count = if (Constants.CalibrationDuration == "60 sec") 24500 else 12000
                    val sample_count =
                        if (Constants.CalibrationDuration == "60 sec") 24500 else 12500
                    if (_ecgSampleCounter + _ppgSampleCounter >= sample_count) {
                        hideMultiVsActive()
                        mBinding.calibrationProgressBar.progress = count
                        multiVsViewModel.patchData.removeObserver(this)
                        multiVsViewModel.patchData.value = null
                        countDownTimer.cancel()
                        onECGPPGFinishedSuccessfully()
                        multiVsViewModel.stopTest(false)
                    } else {
                        mBinding.calibrationProgressBar.progress =
                            _ecgSampleCounter + _ppgSampleCounter
                    }
                } else {
                    Log.d("_SCANNING", "onChanged: patch data is null")
                }
            }
        })
    }

    private fun resetCalibrationProcess() {
        bluetoothViewModel.stopScan()
        Log.e("error", "reset_calibration")
        try {
            parentFragmentManager.commit {
                setCustomAnimations(
                    R.anim.fade_in,
                    R.anim.fade_out,
                    R.anim.fade_in,
                    R.anim.fade_out
                )
                replace(
                    R.id.calibration_container,
                    CalibrationFragment2.newInstance(),
                    CalibrationFragment2.TAG
                )
            }
        }catch (e:Exception){
            e.printStackTrace()
        }
    }

    private fun onECGPPGFinishedSuccessfully() {
        _handler.post(postCalibrationRunnable)
    }

    private val postCalibrationRunnable = object : Runnable {
        override fun run() {

            if (_isBpWorking) {
                mBinding.instructions.text = getString(R.string.waiting_for_bp_to_finish)
                Log.d("_BP", "bp measurement hasn't finished")
                _handler.postDelayed(this, 1000)
            } else {
                mBinding.instructions.text = getString(R.string.uploading_results)
                Log.d("_BP", "bp measurement is finished")
//                multiVsViewModel.stopTest(false)
                bpViewModel.isBpMeasuring = false
//                multiVsViewModel.peekPatchData()
                // multiVsViewModel.postCalibration(requireActivity())
                //mBusyCallback.onBusy(true, getString(R.string.uploading_data_on_server))
                multiVsViewModel.postCalibrationData()
                _ecgSampleCounter = 0
                _ppgSampleCounter = 0
            }
        }
    }

    private fun onCancelCalibration() {

        multiVsViewModel.stopTest(true)
        bluetoothViewModel.stopScan()
        _ecgSampleCounter = 0
        _ppgSampleCounter = 0
        mBinding.calibrationProgressBar.progress = 0
        mBinding.instructions.text = "Calibration cancelled"
        _isBpWorking = false
        preTestTimer?.cancel()
        resetCalibrationProcess()
    }

    private fun initWornCorrectlyObserver() {

        multiVsViewModel.isWornCorrectly.observe(viewLifecycleOwner) { wornCorrectly ->
            if (!wornCorrectly) {

                mNotWornCorrectlyCounter++
                if (mNotWornCorrectlyCounter >= 2) {

                    if (!multiVsViewModel.isPosting && (!_isBpFinished || multiVsViewModel.isTesting) /*multiVsViewModel.isTesting*/) {

//                        AppUtils.makeErrorSnackbar(
//                            mBinding.instructions,
//                            getString(R.string.device_not_placed_well_stopping_calibration),
//                        )
                        AppUtils.showAlertFragment(
                            parentFragmentManager,
                            getString(R.string.multi_vs_calibration),
                            getString(R.string.device_not_placed_well_stopping_calibration)
                        )

                        onCancelCalibration()

                    } else {
                        mNotWornCorrectlyCounter = 0
                    }
                }
            } else {

                mNotWornCorrectlyCounter = 0
            }
        }
    }

    private fun makeSpannableText() {
        val instructions =
            "Remain seated and relax for 5 minutes, when ready - Turn on BIOLIGHT cuff - to start calibration process"
        val spannable = SpannableString(instructions)
        val green =
            ForegroundColorSpan(ContextCompat.getColor(requireActivity(), R.color.dark_green))
        spannable.setSpan(green, 51, 73, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)

        mBinding.instructions.setText(spannable, TextView.BufferType.SPANNABLE)
    }

    override fun onPause() {
        super.onPause()
        bpViewModel.closeDevice()
        bluetoothViewModel.stopScan()
        Log.d("_BP", "onPause: $TAG")
        _handler.removeCallbacks(postCalibrationRunnable)

        multiVsViewModel.patchData.value = null
    }

    override fun onDestroy() {
        super.onDestroy()
        Log.e("fragment", "destroy call")
        multiVsViewModel.onDestroy()
    }

    private fun startTimer() {
        val millis =  if (Constants.MeasurementDuration == "60 sec") TimeUnit.MINUTES.toMillis(2) else TimeUnit.MINUTES.toMillis(1)
        countDownTimer = object : CountDownTimer(millis, 1000) {
            override fun onTick(millis: Long) {
                val sec = (millis / 1000).toInt() % 60
                val min = (millis / (1000 * 60) % 60).toInt()
                val hours = (millis / (1000 * 60 * 60) % 24).toInt()
                 Log.e("timer", String.format("%02d:%02d:%02d", hours, min, sec))
            }

            override fun onFinish() {
                Constants.isDisConnectDevice = true
                onCancelCalibration()
            }
        }
        countDownTimer.start()
    }
}