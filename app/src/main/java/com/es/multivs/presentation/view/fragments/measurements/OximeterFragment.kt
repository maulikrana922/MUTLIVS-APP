package com.es.multivs.presentation.view.fragments.measurements

import android.os.Bundle
import android.os.CountDownTimer
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import com.es.multivs.R
import com.es.multivs.data.bledevices.BleDeviceTypes
import com.es.multivs.data.utils.*
import com.es.multivs.data.utils.Constants.isManualEntry
import com.es.multivs.databinding.OximeterFragmentBinding
import com.es.multivs.presentation.view.fragments.MeasurementListener
import com.es.multivs.presentation.view.fragments.measurements.manualinputs.ManualHrFragment
import com.es.multivs.presentation.view.fragments.measurements.manualinputs.ManualSpO2Fragment
import com.es.multivs.presentation.view.listener.OximeterListener
import com.es.multivs.presentation.view.viewmodels.BluetoothViewModel
import com.es.multivs.presentation.view.viewmodels.IntervalsViewModel
import com.es.multivs.presentation.view.viewmodels.MeasurementViewModel
import com.es.multivs.presentation.view.viewmodels.OximeterViewModel
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.util.concurrent.TimeUnit

/**
 * Created by Marko on 11/2/2021.
 * Etrog Systems LTD.
 */
@AndroidEntryPoint
class OximeterFragment : Fragment() {

    private var mBinding: OximeterFragmentBinding by autoCleared()

    companion object {
        //fun newInstance(fragmentName: String): OximeterFragment = OximeterFragment()
        fun newInstance(fragmentName: String): OximeterFragment {
            val frag = OximeterFragment()
            val bundle = Bundle()
            bundle.putString("fragmentName", fragmentName)
            frag.arguments = bundle
            return frag
        }
    }

    private val measurementViewModel: MeasurementViewModel by activityViewModels()
    private val bluetoothViewModel: BluetoothViewModel by viewModels()
    private val oximeterViewModel: OximeterViewModel by viewModels()
    private val intervalsViewModel: IntervalsViewModel by activityViewModels()
    private var measurementListener: MeasurementListener? = null
    private var oximeterListener: OximeterListener? = null
    private lateinit var countDownTimerDis: CountDownTimer
    private var isConnectDevice = true
    private var mSpO2 = 0
    private var mHeartRate = 0
    private var mSpo2List = ArrayList<Int>()
    private var mHeartRateList = ArrayList<Int>()

    private var _handler: Handler? = Handler(Looper.getMainLooper())
    private var countDownTimer: CountDownTimer? = null
    private var isManual: Int = 0
    private lateinit var fragmentName: String

    private var spo2Dialog: ManualSpO2Dialog? = null
//    private var hrDialog: ManualHrDialog? = null

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        mBinding = OximeterFragmentBinding.inflate(inflater, container, false)
        return mBinding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        fragmentName = arguments?.getString("fragmentName").toString()
        if (fragmentName == Constants.IntervalsFragment) {
            mBinding.oximeterHeartRateTv.setCompoundDrawables(null, null, null, null)
            mBinding.oximeterSpo2Tv.setCompoundDrawables(null, null, null, null)
            mBinding.closeBtn.visibility = View.GONE
        } else
            lifecycleScope.launch {
                isManual = measurementViewModel.getIsManual()
                if (isManual == 0) {
                    mBinding.oximeterHeartRateTv.isClickable = false
                    mBinding.oximeterSpo2Tv.isClickable = false
                }
            }

        lifecycleScope.launch {
            initCountDownTimer()
            initViews()


            createInputWatcher()
            initClickListeners()
            initMeasurementObserver()
            initAndSearchForOximeter()
            initOxiObservers()
        }
    }

    private fun initCountDownTimer() {
        countDownTimer = object : CountDownTimer(20000, 1000) {
            override fun onTick(l: Long) {
                mBinding.oximeterInstructions.text =
                    getString(R.string.oxi_measuring_please_wait, (l / 1000))
            }

            override fun onFinish() {
                mBinding.oximeterInstructions.text = getString(R.string.measuring_complete)
            }
        }
    }

    private fun initAndSearchForOximeter() {
        bluetoothViewModel.bluetoothDevice.observe(viewLifecycleOwner) { device ->
            device?.let {
                bluetoothViewModel.stopScan()
                oximeterViewModel.connectDevice(it)
            }
        }

        bluetoothViewModel.isScanning.observe(viewLifecycleOwner) { isScanning ->
            if (isScanning) {
                mBinding.oximeterInstructions.text = getString(R.string.searching_for_oximeter)
            }
        }

        bluetoothViewModel.rescan.observe(viewLifecycleOwner) { rescan ->
            if (rescan) {
                if (isConnectDevice)
                    startTimer()
                isConnectDevice = false
                AppUtils.makeSnackbar(mBinding.closeBtn, "Couldn't find device, rescanning")
                val mac: String = BleDeviceTypes.getDeviceMAC(BleDeviceTypes.ES_020)!!.uppercase()
                bluetoothViewModel.startScan(mac)
                bluetoothViewModel.resetScan()
            }
        }
    }

    private fun initOxiObservers() {
        oximeterViewModel.spO2.observe(viewLifecycleOwner) { spo2 ->
            spo2?.let {
                mSpo2List.add(it)
            }
        }
        oximeterViewModel.pulseRate.observe(viewLifecycleOwner) { pulseRate ->
            pulseRate?.let {
                mHeartRateList.add(pulseRate)
            }
        }

        oximeterViewModel.isOximeterConnected.observe(viewLifecycleOwner) { isConnected ->
            isConnected?.let {
                if (it) {
                    if (::countDownTimerDis.isInitialized) {
                        countDownTimerDis.cancel()
                        isConnectDevice = true
                    }

                    mBinding.oxiProgressBar.visibility = View.VISIBLE
                    mBinding.oximeterInstructions.text = getString(R.string.preparing_measurement)
                    Constants.isIntervals = true
                    countDownTimer?.start()
                    startMeasurement()
                }
            }
        }

        intervalsViewModel.uploadStatus.observe(viewLifecycleOwner) { uploadStatus ->
            if (uploadStatus.success) {
                oximeterViewModel.closeDevice()
                bluetoothViewModel.stopScan()
                countDownTimer?.cancel()
                _handler?.removeCallbacksAndMessages(null)
                if (Constants.IntervalsFragment == fragmentName)
                    measurementListener?.onCloseMeasurement(
                        BleDeviceTypes.ES_020,
                        getString(R.string.successully_uploaded)
                    )
            } else {
                oximeterViewModel.closeDevice()
                bluetoothViewModel.stopScan()
                countDownTimer?.cancel()
                _handler?.removeCallbacksAndMessages(null)
                measurementListener?.onCloseMeasurement(BleDeviceTypes.ES_020, uploadStatus.message)
            }
        }
    }

    private fun startTimer() {
        val millis = TimeUnit.MINUTES.toMillis(1)
        countDownTimerDis = object : CountDownTimer(millis, 1000) {
            override fun onTick(millis: Long) {
//                val sec = (millis / 1000).toInt() % 60
//                val min = (millis / (1000 * 60) % 60).toInt()
//                val hours = (millis / (1000 * 60 * 60) % 24).toInt()
//                Log.e("Time",String.format(
//                    "%02d:%02d:%02d",
//                    hours,
//                    min,
//                    sec
//                ))
            }

            override fun onFinish() {
                Constants.isDisConnectDevice = true
                lifecycleScope.launch(Dispatchers.Main) {
                    oximeterViewModel.closeDevice()
                    bluetoothViewModel.stopScan()
                    countDownTimer?.cancel()
                    _handler?.removeCallbacksAndMessages(null)
                    measurementListener?.onCloseMeasurement(BleDeviceTypes.ES_020)
                }
            }
        }
        countDownTimerDis.start()
    }

    private fun startMeasurement() {
        _handler?.postDelayed(object : Runnable {
            override fun run() {
                mBinding.oxiProgressBar.visibility = View.INVISIBLE
                processOximeterData()
                _handler?.removeCallbacks(this)
            }
        }, 20000)
    }

    private fun processOximeterData() {
        if (mSpo2List.size != 0 && mHeartRateList.size != 0) {
            mSpo2List.sort()
            mHeartRateList.sort()
            mSpO2 = mSpo2List[mSpo2List.size / 2]
            mHeartRate = mHeartRateList[mHeartRateList.size / 2]
            if (fragmentName == Constants.IntervalsFragment)
                lifecycleScope.launch(Dispatchers.IO) {
                    measurementViewModel.saveOximeterResults(mSpO2, mHeartRate)
                }
            isManualEntry = 0
            Log.e("SPO2", mSpO2.toString())
            mBinding.oximeterSpo2Tv.text = getString(R.string.measurement_result, mSpO2)
            mBinding.oximeterHeartRateTv.text = getString(R.string.hr_manual_text, mHeartRate)
            oximeterViewModel.closeOximeterDataParser()

            if (fragmentName == Constants.IntervalsFragment)
                Handler(Looper.getMainLooper()).postDelayed({
                    lifecycleScope.launch {
                        val results = measurementViewModel.getNonMultiVSResults()
                        intervalsViewModel.postResults(results)
                    }
                }, 5000)

        } else {
            if (activity != null) {
                Toast.makeText(
                    requireActivity(),
                    getString(R.string.measurement_failed_big),
                    Toast.LENGTH_SHORT
                ).show()
            }
        }
    }

    private fun initMeasurementObserver() {
        measurementViewModel.oximeter.observe(viewLifecycleOwner) { results ->
            results?.let {
                if (it.spo2 != 0 && it.heartRate != 0) {
                    mBinding.oximeterSpo2Tv.text =
                        getString(R.string.measurement_result, it.spo2)
                    mBinding.oximeterHeartRateTv.text =
                        getString(R.string.hr_manual_text, it.heartRate)
                    if (fragmentName == Constants.MeasurementsFragment)
                        measurementListener?.handleDirection(ButtonDirection.NEXT, true)
                }
            }
        }
    }

    private fun initClickListeners() {

        mBinding.oximeterSpo2Tv.setOnClickListener {
            ManualSpO2Fragment { spo2 ->
                mSpO2 = spo2
                mBinding.oximeterSpo2Tv.text = getString(R.string.measurement_result, mSpO2)
            }.show(childFragmentManager, ManualSpO2Fragment.TAG)
        }

        mBinding.oximeterHeartRateTv.setOnClickListener {
            ManualHrFragment { heartRate ->
                mHeartRate = heartRate
                mBinding.oximeterHeartRateTv.text = getString(R.string.hr_manual_text, mHeartRate)
            }.show(childFragmentManager, ManualHrFragment.TAG)
        }

        mBinding.closeBtn.setOnClickListener {
            oximeterViewModel.closeDevice()
            bluetoothViewModel.stopScan()
            countDownTimer?.cancel()
            _handler?.removeCallbacksAndMessages(null)
            measurementListener?.onCloseMeasurement(BleDeviceTypes.ES_020)
        }
    }

    private fun createInputWatcher() {

        val watcher = InputChangeWatcher {
            val spo2 = mBinding.oximeterSpo2Tv.text.toString().trim()
            val hr = mBinding.oximeterHeartRateTv.text.toString().trim()
            val isInputOK = spo2.isNotEmpty() && hr.isNotEmpty()
            if (fragmentName == Constants.MeasurementsFragment)
                measurementListener?.handleDirection(ButtonDirection.NEXT, isInputOK)

            if (isInputOK) {
                mBinding.oximeterInstructions.text = getString(R.string.measuring_complete)
            } else {
                mBinding.oximeterInstructions.text = getString(R.string.oxi_instructions)
            }
        }

        mBinding.oximeterSpo2Tv.addTextChangedListener(watcher.inputWatcher)
        mBinding.oximeterHeartRateTv.addTextChangedListener(watcher.inputWatcher)
    }

    private fun initViews() {
        mBinding.oxiProgressBar.visibility = View.INVISIBLE
    }

    override fun onPause() {
        super.onPause()

        oximeterViewModel.closeDevice()
        bluetoothViewModel.stopScan()
        countDownTimer?.cancel()
        countDownTimer = null
        if (mSpO2 > 0 && mHeartRate > 0) {
            if (fragmentName == Constants.MeasurementsFragment)
                measurementViewModel.saveOximeterResults(mSpO2, mHeartRate)
        }
        _handler?.removeCallbacksAndMessages(null)
    }

    override fun onResume() {
        super.onResume()
        measurementListener = parentFragment as MeasurementListener
        if (Constants.IntervalsFragment == fragmentName)
            oximeterListener = parentFragment as OximeterListener
        oximeterViewModel.initializeDevice()
        val mac = BleDeviceTypes.getDeviceMAC(BleDeviceTypes.ES_020)
        mac?.let {
            _handler?.postDelayed({
                bluetoothViewModel.startScan(mac)
            }, 1500)
        }
        if (fragmentName == Constants.MeasurementsFragment)
            measurementListener?.handleDirection(ButtonDirection.NEXT, false)
    }
}