package com.es.multivs.presentation.view.fragments.measurements

import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import com.es.multivs.R
import com.es.multivs.data.bledevices.BleDeviceTypes
import com.es.multivs.data.utils.ButtonDirection
import com.es.multivs.data.utils.Constants
import com.es.multivs.data.utils.InputChangeWatcher
import com.es.multivs.data.utils.autoCleared
import com.es.multivs.databinding.BpFragmentBinding
import com.es.multivs.presentation.view.fragments.MeasurementListener
import com.es.multivs.presentation.view.fragments.measurements.manualinputs.ManualBpFragment
import com.es.multivs.presentation.view.fragments.measurements.manualinputs.ManualHrFragment
import com.es.multivs.presentation.view.viewmodels.BluetoothViewModel
import com.es.multivs.presentation.view.viewmodels.BpViewModel
import com.es.multivs.presentation.view.viewmodels.MeasurementViewModel
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

/**
 * Created by Marko on 11/2/2021.
 * Etrog Systems LTD.
 */
@AndroidEntryPoint
class BpFragment : Fragment() {

    private var mBinding: BpFragmentBinding by autoCleared()

    companion object {
        fun newInstance(): BpFragment = BpFragment()
    }

    // viewModels
    private val bpViewModel: BpViewModel by viewModels()
    private val measurementViewModel: MeasurementViewModel by activityViewModels()
    private val bluetoothViewModel: BluetoothViewModel by viewModels()

    // data
    private var mSYS = 0
    private var mDIA = 0
    private var mHeartRate = 0
    private lateinit var _handler: Handler

    // input & listeners
    private var measurementListener: MeasurementListener? = null

    private var isManual: Int = 0


    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _handler = Handler(Looper.getMainLooper())
        mBinding = BpFragmentBinding.inflate(inflater, container, false)
        return mBinding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        lifecycleScope.launch {
            isManual = measurementViewModel.getIsManual()
            if (isManual == 0) {
                mBinding.bpManualTv.isClickable = false
                mBinding.heartRateTv.isClickable = false
            }
        }

        lifecycleScope.launch(Dispatchers.Main) {
            initBpObservers()
            initAndSearchForBp()
            initMeasurementObserver()
            initClickListeners()


            createInputWatcher()
        }
    }

    private fun initBpObservers() {
        bpViewModel.pulseRate.observe(viewLifecycleOwner, { pulseRate ->
            Constants.isManualEntry = 0
            mHeartRate = pulseRate
            mBinding.heartRateTv.text = getString(R.string.hr_manual_text, pulseRate)
        })

        bpViewModel.bpResults.observe(viewLifecycleOwner, {
            Constants.isManualEntry = 0
            mSYS = it.getSys()
            mDIA = it.getDia()
            mBinding.bpManualTv.text = getString(R.string.bp_manual_text, mSYS, mDIA)
            mBinding.bltProgressBar.visibility = View.INVISIBLE
        })

        bpViewModel.isBioLightConnected.observe(viewLifecycleOwner, { isConnected ->
            if (isConnected) {
                if (mBinding.bpManualTv.text.trim().isNotEmpty()
                    && mBinding.heartRateTv.text.trim().isNotEmpty()
                ) {
                    mBinding.bltInstructions.text =
                        getString(R.string.measure_complete_turn_off_device)
                } else {
                    mBinding.bltInstructions.text = getString(R.string.device_is_connected)
                }
            } else {
                mBinding.bltInstructions.text = getString(R.string.bpm_instructions)
                mBinding.bltProgressBar.visibility = View.INVISIBLE
            }
        })

        bpViewModel.isBioLightMeasuring.observe(viewLifecycleOwner, { isMeasuring ->
            if (isMeasuring) {
                mBinding.bltInstructions.text = getString(R.string.measuring_please_wait)
                mBinding.bltProgressBar.visibility = View.VISIBLE
            } else {
                mBinding.bltInstructions.text = getString(R.string.press_bp_to_start)
                mBinding.bltProgressBar.visibility = View.INVISIBLE
            }
        })
    }

    private fun initAndSearchForBp() {
        bluetoothViewModel.bluetoothDevice.observe(viewLifecycleOwner, {
            bluetoothViewModel.stopScan()
            bpViewModel.connectDevice(it.address)
        })

    }

    private fun initMeasurementObserver() {
        measurementViewModel.bpCuffResults.observe(viewLifecycleOwner, { results ->
            results?.let {
                val hasSYS = it.sys != 0
                val hasDIA = it.dia != 0
                val hasHeartRate = it.heartRate != 0

                if (hasSYS && hasDIA) {
                    mSYS = it.sys
                    mDIA = it.dia
                    mBinding.bpManualTv.text = getString(R.string.bp_manual_text, mSYS, mDIA)
                }

                if (hasHeartRate) {
                    mHeartRate = it.heartRate
                    mBinding.heartRateTv.text = getString(R.string.hr_manual_text, mHeartRate)
                }

                if (hasSYS && hasDIA && hasHeartRate) {
                    measurementListener?.handleDirection(ButtonDirection.NEXT, true)
                    mBinding.bltInstructions.text =
                        getString(R.string.measure_complete_turn_off_device)
                }
            }
        })
    }

    private fun initClickListeners() {
        mBinding.bpManualTv.setOnClickListener {
            ManualBpFragment { sys, dia ->
                mSYS = sys
                mDIA = dia
                mBinding.bpManualTv.text = getString(R.string.bp_manual_text, mSYS, mDIA)
            }.show(childFragmentManager, ManualBpFragment.TAG)
        }

        mBinding.heartRateTv.setOnClickListener {
            ManualHrFragment { heartRate ->
                mHeartRate = heartRate
                mBinding.heartRateTv.text = getString(R.string.hr_manual_text, mHeartRate)
            }.show(childFragmentManager, ManualHrFragment.TAG)
        }

        mBinding.closeBtn.setOnClickListener {
            bpViewModel.closeDevice()
            bluetoothViewModel.stopScan()
            lifecycleScope.launch {
                if (mSYS > 0 && mDIA > 0 && mHeartRate > 0) {
                    measurementViewModel.saveBpCuffResults(mSYS, mDIA, mHeartRate)
                }
                measurementListener?.onCloseMeasurement(BleDeviceTypes.ES_022)
            }
        }
    }

    private fun createInputWatcher() {

        val watcher = InputChangeWatcher {
            val bp = mBinding.bpManualTitle.text.toString().trim()
            val hr = mBinding.heartRateTv.text.toString().trim()
            val isInputOK = bp.isNotEmpty() && hr.isNotEmpty()

            measurementListener?.handleDirection(ButtonDirection.NEXT, isInputOK)

            if (isInputOK) {
                mBinding.bltInstructions.text = getString(R.string.measure_complete_turn_off_device)
            } else {
                mBinding.bltInstructions.text = getString(R.string.bpm_instructions)
            }
        }

        mBinding.bpManualTv.addTextChangedListener(watcher.inputWatcher)
        mBinding.heartRateTv.addTextChangedListener(watcher.inputWatcher)
    }

    override fun onResume() {
        super.onResume()

        measurementListener = parentFragment as MeasurementListener
        bpViewModel.initializeDevice()
        val mac = BleDeviceTypes.getDeviceMAC(BleDeviceTypes.ES_022)?.uppercase()
        mac?.let {
            if (it.isNotEmpty()) {
                _handler.postDelayed({
                    bluetoothViewModel.startScan(it)
                }, 2000)
            }
        }

        measurementListener?.handleDirection(ButtonDirection.NEXT, false)
    }

    override fun onPause() {
        super.onPause()
        _handler.removeCallbacksAndMessages(null)
        bluetoothViewModel.stopScan()
        bpViewModel.closeDevice()
        if (mSYS > 0 && mDIA > 0 && mHeartRate > 0) {
            Log.d("_SUMMARY", "saving bg results: sys/dia: $mSYS / $mDIA | hr: $mHeartRate")
            measurementViewModel.saveBpCuffResults(mSYS, mDIA, mHeartRate)
        }

        measurementListener = null
    }
}