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
import com.es.multivs.R
import com.es.multivs.data.bledevices.BleDeviceTypes
import com.es.multivs.data.utils.ButtonDirection
import com.es.multivs.data.utils.Constants
import com.es.multivs.data.utils.Constants.isManualEntry
import com.es.multivs.data.utils.InputChangeWatcher
import com.es.multivs.databinding.WeightscaleFragmentBinding
import com.es.multivs.presentation.view.fragments.MeasurementListener
import com.es.multivs.presentation.view.fragments.measurements.manualinputs.ManualWeightFragment
import com.es.multivs.presentation.view.viewmodels.BluetoothViewModel
import com.es.multivs.presentation.view.viewmodels.MeasurementViewModel
import com.es.multivs.presentation.view.viewmodels.WeightScaleViewModel
import dagger.hilt.android.AndroidEntryPoint
import com.es.multivs.data.utils.autoCleared

/**
 * Created by Marko on 11/8/2021.
 * Etrog Systems LTD.
 */
@AndroidEntryPoint
class WeightScaleFragment : Fragment() {

    private var mBinding: WeightscaleFragmentBinding by autoCleared()

    companion object {
        fun newInstance(): WeightScaleFragment = WeightScaleFragment()
    }

    private val weightScaleViewModel: WeightScaleViewModel by viewModels()
    private val measurementViewModel: MeasurementViewModel by activityViewModels()
    private val bluetoothViewModel: BluetoothViewModel by viewModels()

    private var measurementListener: MeasurementListener? = null
    private var manualWeightInteger = 97
    private var manualWeightDecimal = 0
    private var mWeight = 0.0
    private lateinit var _handler: Handler

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _handler = Handler(Looper.getMainLooper())
        mBinding = WeightscaleFragmentBinding.inflate(inflater, container, false)
        return mBinding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        initViews()


        createInputWatcher()
        initListeners()
        initScaleObservers()
        initMeasurementObservers()
        initAndSearchScale()
    }

    private fun initScaleObservers() {
        weightScaleViewModel.weight.observe(viewLifecycleOwner, {
            isManualEntry = 0
            mBinding.weightScaleTv.text = getString(R.string.weight_scale_results, it.toString())
            mBinding.weightScaleInstructions.text = getString(R.string.measuring_complete)
            mWeight = it
        })

        weightScaleViewModel.isWeightScaleConnected.observe(viewLifecycleOwner, { isConnected ->
            if (isConnected) {
                mBinding.weightScaleInstructions.text =
                    getString(R.string.weight_scale_instructions)
                mBinding.weightScaleProgressBar.visibility = View.INVISIBLE
            } else {
                mBinding.weightScaleInstructions.text =
                    getString(R.string.device_disconnected_trying_reconnect)
                val mac = BleDeviceTypes.getDeviceMAC(BleDeviceTypes.ES_023)
                mac?.let {
                    if (it.isNotEmpty()) {
                        bluetoothViewModel.startScan(mac)
                    }
                }
            }
        })
    }

    private fun initMeasurementObservers() {
        measurementViewModel.weight.observe(viewLifecycleOwner, {
            if (it != null && it.weight > 0.0) {
                Constants.isManualEntry=0
                mBinding.weightScaleTv.text =
                    getString(R.string.weight_scale_results, it.weight.toString())
                measurementListener?.handleDirection(ButtonDirection.NEXT, true)
            }
        })
    }

    private fun initAndSearchScale() {
        bluetoothViewModel.bluetoothDevice.observe(viewLifecycleOwner, { device ->
            bluetoothViewModel.stopScan()
            mBinding.weightScaleInstructions.text = getString(R.string.connecting_to_weight_scale)
            weightScaleViewModel.connectDevice(device)
        })
    }

    private fun initListeners() {
        mBinding.weightScaleTv.setOnClickListener {
            ManualWeightFragment { weight ->
                mWeight = weight
                mBinding.weightScaleTv.text =
                    getString(R.string.weight_scale_results, mWeight.toString())
            }.show(childFragmentManager, ManualWeightFragment.TAG)
        }

        mBinding.closeBtn.setOnClickListener {
            measurementListener?.onCloseMeasurement(BleDeviceTypes.ES_023)
        }
    }

    private fun initViews() {
        mBinding.weightScaleProgressBar.visibility = View.VISIBLE
    }

    private fun createInputWatcher() {
        val watcher = InputChangeWatcher {

            val isInputOK = mBinding.weightScaleTv.text.toString().trim().isNotEmpty()
            if (isInputOK) {
                mBinding.weightScaleInstructions.text = getString(R.string.measuring_complete)
                mBinding.weightScaleProgressBar.visibility = View.INVISIBLE
                measurementListener?.handleDirection(ButtonDirection.NEXT, isInputOK)
            } else {
                mBinding.weightScaleInstructions.text =
                    getString(R.string.weight_scale_instructions)
                mBinding.weightScaleProgressBar.visibility = View.VISIBLE
            }
//            measurementListener?.handleDirection(ButtonDirection.NEXT, isInputOK)
        }

        mBinding.weightScaleTv.addTextChangedListener(watcher.inputWatcher)
    }

    override fun onResume() {
        super.onResume()

        measurementListener = parentFragment as MeasurementListener
        val mac = BleDeviceTypes.getDeviceMAC(BleDeviceTypes.ES_023)?.uppercase()
        Log.d("_SCANNING", "onResume: weight scale: $mac")
        mac?.let {
            if (it.isNotEmpty()) {
                _handler.postDelayed({
                    bluetoothViewModel.startScan(it)
                }, 1500)
            }
        }
    }

    override fun onPause() {
        super.onPause()
        _handler.removeCallbacksAndMessages(null)
        weightScaleViewModel.closeDevice()
        bluetoothViewModel.stopScan()
        measurementListener = null
        if (mWeight > 0) {
            measurementViewModel.saveWeightScaleResults(mWeight)
        }
    }

}