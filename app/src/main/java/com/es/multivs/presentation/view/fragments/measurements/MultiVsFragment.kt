package com.es.multivs.presentation.view.fragments.measurements

import android.app.AlertDialog
import android.bluetooth.BluetoothManager
import android.content.Context
import android.os.*
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.animation.Animation
import android.view.animation.AnimationUtils
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Observer
import androidx.lifecycle.lifecycleScope
import com.bumptech.glide.Glide
import com.bumptech.glide.load.resource.drawable.DrawableTransitionOptions
import com.es.multivs.R
import com.es.multivs.data.bledevices.BleDeviceTypes
import com.es.multivs.data.bledevices.multivs.PatchData
import com.es.multivs.data.database.sets.TestType
import com.es.multivs.data.utils.*
import com.es.multivs.databinding.MultivsFragmentBinding
import com.es.multivs.presentation.view.fragments.MeasurementListener
import com.es.multivs.presentation.view.fragments.MultiVSResultsDialogFragment
import com.es.multivs.presentation.view.fragments.calibration.CalibrationFragment
import com.es.multivs.presentation.view.viewmodels.BluetoothViewModel
import com.es.multivs.presentation.view.viewmodels.MultiVsViewModel
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.util.concurrent.TimeUnit
import javax.inject.Inject

/**
 * Created by Marko on 10/21/2021.
 * Etrog Systems LTD.
 */
@AndroidEntryPoint
class MultiVsFragment : Fragment() {

    companion object {
        fun newInstance(scheduleTimestamp: String?, fragmentName: String): MultiVsFragment {
            val frag = MultiVsFragment()
            val bundle = Bundle()
            bundle.putString("schedule_timestamp", scheduleTimestamp)
            bundle.putString("fragmentName", fragmentName)
            frag.arguments = bundle

            return frag
        }

    }

    private var scheduleTimestamp: String? = null

    @Inject
    lateinit var scheduleUpdates: ScheduleUpdatesManager

    private val CONNECTION_TIME_LIMIT = 30000L

    private var measurementListener: MeasurementListener? = null
    private val multiVsViewModel: MultiVsViewModel by viewModels()
    private val bluetoothViewModel: BluetoothViewModel by viewModels()
    private lateinit var binding: MultivsFragmentBinding
    private var isDialogOpen = false

    private lateinit var animationHeartIn: Animation
    private lateinit var animationHeartExit: Animation
    private lateinit var animationResp: Animation

    private lateinit var alertPostingResults: AlertDialog
    private var cancelDialog: AlertDialog? = null

    private val _handler = Handler(Looper.getMainLooper())

    private var isTesting: Boolean = false
    private var isAlertDialog: Boolean = false
    private var _ecgSampleCounter: Int = 0
    private var _ppgSampleCounter: Int = 0
    private lateinit var countDownTimer: CountDownTimer
    private var isProgressBar = false
    private var heartRate = 0
    private var temperature = 0f
    private var finalTemperature: String? = null
    private var stepsCount = 0
    private var respiratory = 0
    private var diastolic: String? = null
    private var systolic: String? = null
    private var isECGOn = false
    private var sampleAmount: Int = 0
    private var wornIncorrectlyCounter: Int = 0
    private lateinit var testType: TestType
    private lateinit var fragmentName: String
    private var isCountdownTimer = true
    private var mac = ""
    private var buttonClick = true
    private var mIsConnected = true

    private val preTestTime: CountDownTimer = object : CountDownTimer(5000, 1000) {
        override fun onTick(l: Long) {
            binding.multivsInstructions.text =
                getString(R.string.starting_test_in).plus(" ${(l / 1000)}")
            if (l < 1000) {
                cancel()
                onFinish()
            }
        }

        override fun onFinish() {
            hideAndDisableView(binding.multivsStartBtn)
            // binding.multivsHeartImg.startAnimation(animationHeartIn)
            //showAndEnableView(binding.multivsHeartImg)
            showAndEnableView(binding.progressBar)
            binding.multivsInstructions.text = getString(R.string.measuring_please_wait)
            if (!isProgressBar) {
                startTimer()
                isProgressBar = true
            }
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {

        binding = MultivsFragmentBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        scheduleTimestamp = arguments?.getString("schedule_timestamp")
        fragmentName = arguments?.getString("fragmentName").toString()


        initAnimations()
        initViews()
        initListeners()
        initObservers()

//        graphHandler = GraphHandler(requireActivity(), binding.ecgGraph, binding.ppgGraph)

        lifecycleScope.launch(Dispatchers.IO) {
            val testTypeDeferred = multiVsViewModel.getTestTypeAsync()
            testType = testTypeDeferred.await()

            sampleAmount = if (testType.isECGChecked && testType.isPPGChecked) {
                if (Constants.MeasurementDuration == "60 sec") 24500 else 12500
            } else {
                if (Constants.MeasurementDuration == "60 sec") 12500 else 6500
            }

            binding.progressBar.max = sampleAmount
        }
    }

    private fun initAnimations() {
        animationHeartIn = AnimationUtils.loadAnimation(requireContext(), R.anim.heart_enter)
        animationHeartIn.fillAfter = true
        animationHeartExit = AnimationUtils.loadAnimation(requireContext(), R.anim.heart_exit)
        animationResp = AnimationUtils.loadAnimation(requireContext(), R.anim.respiratory)
    }

    private var connectionErrorRunnable = Runnable {
        if (Constants.MeasurementsFragment == fragmentName)
            AppUtils.makeErrorSnackbar(binding.multivsInstructions, "MULTIVS not found", 5000)

        lifecycleScope.launch(Dispatchers.Main) {
            multiVsViewModel.closeDevice()
            delay(1000)
            measurementListener?.onCloseMeasurement(
                BleDeviceTypes.ES_008,
                getString(R.string.failed_to_connect_device)
            )
        }
    }

    private fun initBluetoothObserver() {
        bluetoothViewModel.bluetoothDevice.observe(viewLifecycleOwner) {
            it?.let {
                binding.multivsInstructions.text = getString(R.string.connecting_device_please_wait)
                bluetoothViewModel.bluetoothDevice.removeObservers(viewLifecycleOwner)
                _handler.postDelayed(connectionErrorRunnable, CONNECTION_TIME_LIMIT)
                multiVsViewModel.initBleDevice(it)
            }
        }
    }

    private fun initTest() {
        initPatchDataObserver()
//        showGraphs()
        isTesting = true
        multiVsViewModel.startTest(false)
//        timersHandler.startTestTimer()
    }

    private fun initPatchDataObserver() {
        multiVsViewModel.getPatchData().observe(viewLifecycleOwner, patchDataObserver)
    }

    private val patchDataObserver: Observer<PatchData> = object : Observer<PatchData> {
        override fun onChanged(data: PatchData) {
            data.let {
                isTesting = true
                if (it.isEcg) {
                    _ecgSampleCounter += it.ecgSampleList.size
                }
                if (it.isPpg) {
                    _ppgSampleCounter += it.ppgSampleList.size
                }

                if ((_ecgSampleCounter + _ppgSampleCounter) >= sampleAmount) {
                    binding.progressBar.progress = sampleAmount
                    multiVsViewModel.getPatchData().removeObserver(this)
                    _ecgSampleCounter = 0
                    _ppgSampleCounter = 0
                    onTestFinishedSuccessfully()
                    if (::countDownTimer.isInitialized)
                        countDownTimer.cancel()
                    isProgressBar = false
                } else {
                    binding.progressBar.progress = (_ecgSampleCounter + _ppgSampleCounter)
                }


//                graphHandler?.let { handler ->
//                    if (!handler.isStartedReadFromQueue) {
//                        handler.startReadingFromQueue(it)
//                    }
//                    handler.addSamplesToQueue(it)
//                }
            }
        }

    }

    private fun initObservers() {
        bluetoothViewModel.isScanning.observe(viewLifecycleOwner) {
            if (it) {
                binding.multivsInstructions.text = getText(R.string.search_for_multivs)
            }
        }

        multiVsViewModel.isConnected.observe(viewLifecycleOwner) { isConnected ->
            if (isConnected) {
                Constants.isIntervals = true
                _handler.removeCallbacks(connectionErrorRunnable)
                binding.multivsInstructions.text =
                    getString(R.string.multivs_connected_testing_position)
                initializeIsWornCorrectlyObserver()
            }
        }

        multiVsViewModel.getUserLatestRead().observe(viewLifecycleOwner) {
            if (!isTesting) {
                heartRate = it.message.heartBeat
                respiratory = it.message.respiratorRateT
                diastolic = it.message.bpDiastolic
                systolic = it.message.bpSystolic
                isECGOn = testType.isECGChecked
                finalTemperature = it.message.temperature

                _handler.post {
                    alertPostingResults.dismiss()
                    showAndEnableView(binding.multivsHeart)
                    binding.progressBar.progress = 0
                    hideAndDisableView(binding.progressBar)
                    binding.multivsInstructions.text = getString(R.string.successully_uploaded)

                    scheduleUpdates.measurementsComplete(scheduleTimestamp ?: "")

                    if (fragmentName == Constants.IntervalsFragment) {
                        alertPostingResults.dismiss()
                        multiVsViewModel.closeDevice()
                        if (Constants.isMULTIVS) {
                            measurementListener?.onCloseMeasurement(
                                BleDeviceTypes.ES_008,
                                getString(R.string.successully_uploaded)
                            )
                        } else measurementListener?.handleDirection(ButtonDirection.NEXT, true)
                    } else {
                        measurementListener?.handleDirection(ButtonDirection.NEXT, true)
                        if (!isDialogOpen)
                            showResults()
                    }
                }
            } else {
                if (fragmentName == Constants.IntervalsFragment) {
                    alertPostingResults.dismiss()
                    if (Constants.isMULTIVS) {
                        multiVsViewModel.closeDevice()
                        measurementListener?.onCloseMeasurement(
                            BleDeviceTypes.ES_008,
                            getString(R.string.successully_uploaded)
                        )
                    } else measurementListener?.handleDirection(ButtonDirection.NEXT, true)
                }
            }
        }

        multiVsViewModel.isMeasurementFailed().observe(viewLifecycleOwner) {
            it?.let {
                Log.e("Status result", it.message)
                if (it.message.isEmpty()) {
                    onTestCancelled(getString(R.string.measurement_failed))
                } else {
                    onTestCancelled(it.message)
                }
            }
        }

        multiVsViewModel.getTemperature().observe(viewLifecycleOwner) {

            it?.let {
                if (multiVsViewModel.isPosting) {
                    temperature = it
                }
            }
        }

        multiVsViewModel.getSteps().observe(viewLifecycleOwner) {
            it?.let {
                stepsCount = it
            }
        }

        multiVsViewModel.getIsBadData().observe(viewLifecycleOwner) {
            AppUtils.showAlertFragment(
                parentFragmentManager,
                "MULTIVS",
                getString(R.string.bad_data_remeasure)
            )
            onTestCancelled("Bad data detected")
        }

        bluetoothViewModel.rescan.observe(viewLifecycleOwner) {
            bluetoothViewModel.stopScan()
            bluetoothViewModel.resetScan()
            if (!isAlertDialog) {
                AppUtils.makeErrorSnackbar(binding.multivsInstructions, "MULTIVS not found", 5000)

                isAlertDialog = true

                Handler(Looper.getMainLooper()).postDelayed({
                    try {
                        Constants.isDisConnectDevice = true
                        measurementListener?.onBusy(true, "Quitting measurements, please wait")
                        multiVsViewModel.stopAndClose()
                    } catch (e: Exception) {

                    } finally {
                        measurementListener?.onBusy(false, " ")
                        measurementListener?.onCloseMeasurement(BleDeviceTypes.ES_008)
                    }
                }, 5000)
            }
        }
    }

    private fun showResults() {
        isDialogOpen = true
        alertPostingResults.dismiss()

        val resultBundle = Bundle()

        resultBundle.putString("temperature_result", finalTemperature)
        resultBundle.putInt("heart_rate_result", heartRate)
        resultBundle.putInt("respiratory_result", respiratory)
        resultBundle.putString("diastolic_result", diastolic)
        resultBundle.putString("systolic_result", systolic)
        resultBundle.putInt("steps_result", stepsCount)
        resultBundle.putBoolean("is_ecg_on", isECGOn)

        val resultsFragment: MultiVSResultsDialogFragment =
            MultiVSResultsDialogFragment.newInstance(resultBundle)
        resultsFragment.show(childFragmentManager, "multivs_results_dialog")

    }

    override fun onResume() {
        super.onResume()
        initBluetoothObserver()
        mac = BleDeviceTypes.getDeviceMAC(BleDeviceTypes.ES_008)!!.uppercase()
        if (mac.isNotEmpty()) {
            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.P) {
                _handler.postDelayed(btRunnable, 2000)
            } else {
                lifecycleScope.launch {
                    delay(2000)
                    bluetoothViewModel.startScan(mac)
                }
            }
        }

        measurementListener = parentFragment as MeasurementListener?

        multiVsViewModel.multivsActive = true
        initObservers()
    }

    override fun onPause() {
        super.onPause()
        isTesting = false
        _handler.removeCallbacks(connectionErrorRunnable)
        _handler.removeCallbacks(btRunnable)
        multiVsViewModel.clearHandler()
        measurementListener = null
        bluetoothViewModel.stopScan()
        if (isTesting) {
            multiVsViewModel.stopTest(true)
        }
        multiVsViewModel.closeDevice()
        multiVsViewModel.multivsActive = false
    }

    private val btRunnable = Runnable {
        lifecycleScope.launch {
            val mBluetoothManager =
                activity?.getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager
            val mBluetoothAdapter = mBluetoothManager.adapter

            AppUtils.showBluetoothRefreshAlert(requireContext())
            delay(300)
            mBluetoothAdapter.disable()
            _handler.postDelayed(object : Runnable {
                override fun run() {
                    if (!mBluetoothAdapter.isEnabled) {
                        mBluetoothAdapter.enable()
                        _handler.postDelayed(this, 2000)
                    } else {
                        AppUtils.hideBluetoothRefreshAlert()
                        bluetoothViewModel.startScan(mac)
                    }
                }
            }, 2500)
        }
    }

    private fun initListeners() {

        animationHeartExit.setAnimationListener(object : Animation.AnimationListener {
            override fun onAnimationStart(animation: Animation) {}
            override fun onAnimationEnd(animation: Animation) {
                // binding.multivsHeartImg.clearAnimation() // clear animation
                binding.multivsHeartImg.visibility = View.INVISIBLE
            }

            override fun onAnimationRepeat(animation: Animation) {}
        })

        animationHeartIn.setAnimationListener(object : Animation.AnimationListener {
            override fun onAnimationStart(animation: Animation) {}
            override fun onAnimationEnd(animation: Animation) {
                // binding.multivsHeartImg.startAnimation(animationResp) // start breathing animation
                binding.multivsHeartImg.visibility = View.VISIBLE
            }

            override fun onAnimationRepeat(animation: Animation) {}
        })

        binding.multivsHeart.setOnClickListener {
            isDialogOpen = false
            initTest()
            hideSensorPositionImages()
            hideAndDisableView(binding.multivsStartBtn)
            showAndEnableView(binding.multivsCancelBtn)
            preTestTime.start()
        }

        // binding.multivsHeart.setOnClickListener(imageListener)

        binding.multivsCancelBtn.setOnClickListener {
            if (::countDownTimer.isInitialized)
                countDownTimer.cancel()
            onTestCancelled(getString(R.string.measurement_cancelled))
        }

        if (fragmentName == Constants.IntervalsFragment) {
            hideAndDisableView(binding.multivsCancelBtn)
            binding.closeBtn.visibility = View.GONE
        }

        binding.closeBtn.setOnClickListener {
            _handler.removeCallbacks(deviceConnectRunnable)
            if (::countDownTimer.isInitialized)
                countDownTimer.cancel()
            stopECGGraph(true)
            try {
                bluetoothViewModel.stopScan()
                measurementListener?.onBusy(true, "Quitting measurements, please wait")

                multiVsViewModel.stopAndClose()

            } catch (e: Exception) {

            } finally {
                measurementListener?.onBusy(false, " ")
                measurementListener?.onCloseMeasurement(BleDeviceTypes.ES_008)
            }

        }
    }

    private fun initViews() {

        Glide.with(requireActivity()).load(R.drawable.multivs_logo).into(binding.multivsLogo)
        binding.multivsCancelBtn.isEnabled = false
        binding.multivsStartBtn.visibility = View.GONE
        binding.multivsStartBtn.isEnabled = false
    }

    private val imageListener = View.OnClickListener {
        showAndEnableView(binding.multivsStartBtn)
    }

    private fun onTestCancelled(message: String) {
        Log.e("Status", message)
        isTesting = false
        multiVsViewModel.stopTest(true)

        // binding.multivsHeartImg.startAnimation(animationHeartExit)
        binding.multivsHeartImg.visibility = View.INVISIBLE
        hideAndDisableView(binding.multivsCancelBtn)
        preTestTime.cancel()


        val alertDialog = AppUtils.showAlertDialog(
            parentFragmentManager,
            getString(R.string.multi_vs),
            message
        )
        if (fragmentName == Constants.IntervalsFragment && Constants.isMULTIVS)
            Handler(Looper.getMainLooper()).postDelayed({
                alertDialog.dismiss()
            }, 5000)

        binding.progressBar.progress = 0
        _ecgSampleCounter = 0
        _ppgSampleCounter = 0
        hideAndDisableView(binding.progressBar)

//        stopECGGraph(true)
        if (::alertPostingResults.isInitialized)
            alertPostingResults.dismiss()
//        if (graphHandler != null) {
//            graphHandler.stopGraph();
//        }
    }

    private fun stopECGGraph(hide: Boolean) {
        if (hide) {
            binding.ecgGraph.visibility = View.GONE
            binding.ppgGraph.visibility = View.GONE
        }
//        graphHandler?.stopGraph()
    }

    private fun hideSensorPositionImages() {
        binding.multivsHeart.visibility = View.GONE
    }

    private fun initializeIsWornCorrectlyObserver() {
        multiVsViewModel.isWornCorrectly.observe(viewLifecycleOwner) { isWornCorrectly ->
            interpretSensorPositionOnBody(isWornCorrectly)
        }
    }


    /**
     * This Function determines if the device is worn correctly on the body.<br>
     * If the device <b>is worn correctly</b> and <b>currently testing</b>, then function returns and finishes.<br>
     * If the device <b>is worn correctly</b> and <b>not testing</b>, then start interpretation with <b>true</b> flag.<br>
     * If the device <b>isn't worn correctly</b> and <b>not testing</b>, then start interpretation with <b>false</b> flag.
     * If the device <b>isn't worn correctly</b> and <b>currently testing</b>, then cancels current test and test timers.<br>
     *
     * @param isWornCorrectly A boolean that indicates whether device is properly worn.
     */
    private fun interpretSensorPositionOnBody(isWornCorrectly: Boolean) {

        MeasurementUtils.interpretSensorPositionOnBody(
            isTesting, isWornCorrectly
        ) { status ->
            when (status) {
                MeasurementUtils.TESTING_OK -> {
                }
                MeasurementUtils.TESTING_FAIL -> {
                    isTesting = false
                    // binding.multivsHeartImg.startAnimation(animationHeartExit)
                    binding.multivsHeartImg.visibility = View.INVISIBLE
                    hideAndDisableView(binding.multivsCancelBtn)
                    onTestCancelled(getString(R.string.device_not_placed_well_stopping_measurement))
                    preTestTime.cancel()


                    AppUtils.showAlertFragment(
                        parentFragmentManager,
                        getString(R.string.multi_vs),
                        getString(R.string.device_not_placed_well_stopping_measurement)
                    )

//                    stopECGGraph(true)
//                    if (graphHandler != null) {
//                        graphHandler.stopGraph();
//                    }
                }
                MeasurementUtils.IDLE_FAIL -> {
                    buttonClick = true
                    if (isCountdownTimer)
                        startTimer()

                    isCountdownTimer = false
                    hideAndDisableView(binding.multivsStartBtn)
                    binding.multivsInstructions.text = getString(R.string.checking_device_placement)
                    binding.multivsHeart.visibility = View.GONE
                    binding.progressBar.visibility = View.GONE
                    binding.multivsHeartImg.visibility = View.INVISIBLE
                    binding.multivsHeart.isEnabled = false
                    if (wornIncorrectlyCounter > 2) {
                        AppUtils.makeErrorSnackbarNoAction(
                            binding.multivsInstructions,
                            "The device is not properly attached.",
                        )
                        wornIncorrectlyCounter = 0
                    } else {
                        wornIncorrectlyCounter++
                    }
                    mIsConnected = true
                }
                MeasurementUtils.IDLE_OK -> {
                    showInstructionsOnFirstTest()
                    hideAndDisableView(binding.multivsCancelBtn)
                    wornIncorrectlyCounter = 0
                    if (::countDownTimer.isInitialized) {
                        countDownTimer.cancel()
                        isCountdownTimer = true
                    }
                }
            }
        }

    }

    private fun startTimer() {

        val millis =  if (Constants.MeasurementDuration == "60 sec") TimeUnit.MINUTES.toMillis(2) else TimeUnit.MINUTES.toMillis(1)
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
                if (fragmentName == Constants.IntervalsFragment && !Constants.isMULTIVS)
                    measurementListener?.handleDirection(ButtonDirection.NEXT, true)
                else
                    lifecycleScope.launch(Dispatchers.Main) {
                        if (::alertPostingResults.isInitialized)
                            alertPostingResults.dismiss()
                        delay(1000)
                        measurementListener?.onCloseMeasurement(
                            BleDeviceTypes.ES_008,
                            getString(R.string.failed_to_connect_device)
                        )
                    }
            }
        }
        countDownTimer.start()
    }

    private fun closeDevice() {
        multiVsViewModel.clearHandler()
        bluetoothViewModel.stopScan()
        multiVsViewModel.closeDevice()
        multiVsViewModel.multivsActive = false
    }

    private fun showInstructionsOnFirstTest() {
        if (Constants.IntervalsFragment != fragmentName) {
            binding.multivsInstructions.setText(R.string.multi_vs_init_instructions)
            if (mIsConnected)
                _handler.postDelayed(deviceConnectRunnable, 5000)
            mIsConnected = false
        }

        loadDevicePlacementImages()
    }

    private var deviceConnectRunnable = Runnable {
        AppUtils.makeShowMessage(
            binding.multivsInstructions,
            "MULTIVS is connected,on body"
        )
    }

    private fun onTestFinishedSuccessfully() {
        cancelDialog?.let {
            if (it.isShowing) {
                it.dismiss()
            }
        }
        // stopECGGraph(false)

        binding.multivsInstructions.text = ""
        multiVsViewModel.stopTest(false)
        isTesting = false
        showPostingResultsAlert()
        hideAndDisableView(binding.multivsCancelBtn)
        //binding.multivsHeartImg.startAnimation(animationHeartExit)
        binding.multivsHeartImg.visibility = View.INVISIBLE

        multiVsViewModel.postPatchData()

//        graphHandler.stopGraph();
    }

    private fun loadDevicePlacementImages() {
        if (!binding.multivsStartBtn.isEnabled) {
            binding.multivsHeart.visibility = View.VISIBLE
            binding.multivsHeart.isEnabled = true

            lifecycleScope.launch {
                val devicePosition = multiVsViewModel.getMeasurementDevicePositionAsync()
                if (Constants.MeasurementsFragment == fragmentName)
                    displayBlueMan(devicePosition.await())
                else {
                    if (buttonClick) {
                        buttonClick = false
                        binding.multivsHeart.performClick()
                    }
                }
            }
        }
    }

    private fun displayBlueMan(devicePosition: String) {
        lifecycleScope.launch(Dispatchers.Main) {
            when (devicePosition) {
                CalibrationFragment.PATCH_UPPER_LEFT -> {
                    Glide.with(requireActivity()).load(R.drawable.patch_upper_left)
                        .transition(DrawableTransitionOptions.withCrossFade())
                        .into(binding.multivsHeart)
                }
                CalibrationFragment.BELT_MIDDLE -> {
                    Glide.with(requireActivity()).load(R.drawable.belt_middle)
                        .transition(DrawableTransitionOptions.withCrossFade())
                        .into(binding.multivsHeart)
                }
                CalibrationFragment.PATCH_MIDDLE -> {
                    Glide.with(requireActivity()).load(R.drawable.patch_middle)
                        .transition(DrawableTransitionOptions.withCrossFade())
                        .into(binding.multivsHeart)
                }
            }
        }
    }

    private fun showAndEnableView(v: View) {
        v.visibility = View.VISIBLE
        v.isEnabled = true
    }

    private fun hideAndDisableView(v: View) {
        v.visibility = View.GONE
        v.isEnabled = false
    }

    private fun showPostingResultsAlert() {
        val layoutInflater = LayoutInflater.from(requireContext())
        val promptView: View = layoutInflater.inflate(R.layout.posting_es008_results_alert, null)
        alertPostingResults = AlertDialog.Builder(requireContext()).create()
        alertPostingResults.apply {
            setView(promptView)
            setCancelable(false)
            show()
        }
    }


}

