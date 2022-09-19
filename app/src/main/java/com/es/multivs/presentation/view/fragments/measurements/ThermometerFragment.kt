package com.es.multivs.presentation.view.fragments.measurements

import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import com.es.multivs.R
import com.es.multivs.data.bledevices.BleDeviceTypes
import com.es.multivs.data.utils.AppUtils
import com.es.multivs.data.utils.ButtonDirection
import com.es.multivs.data.utils.Constants.isManualEntry
import com.es.multivs.data.utils.InputChangeWatcher
import com.es.multivs.databinding.ThermometerFragmentBinding
import com.es.multivs.presentation.view.fragments.MeasurementListener
import com.es.multivs.presentation.view.fragments.measurements.manualinputs.ManualTemperatureFragment
import com.es.multivs.presentation.view.viewmodels.BluetoothViewModel
import com.es.multivs.presentation.view.viewmodels.MeasurementViewModel
import com.es.multivs.presentation.view.viewmodels.ThermometerViewModel
import dagger.hilt.android.AndroidEntryPoint
import com.es.multivs.data.utils.autoCleared
import kotlinx.coroutines.launch

@AndroidEntryPoint
class ThermometerFragment : Fragment() {

    private var mBinding: ThermometerFragmentBinding by autoCleared()

    private val measurementViewModel: MeasurementViewModel by activityViewModels()
    private val bluetoothViewModel: BluetoothViewModel by viewModels()
    private val thermometerViewModel: ThermometerViewModel by viewModels()

    private lateinit var _handler: Handler

    private var mTemptInteger: Int = 97
    private var mTemptDecimal: Int = 0
    private var mThermometerResult: String = ""

    private var isManual: Int = 0

    private var isThermometerConnected = false
    private var measurementListener: MeasurementListener? = null

    companion object {
        fun newInstance(): ThermometerFragment = ThermometerFragment()
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _handler = Handler(Looper.getMainLooper())
        mBinding = ThermometerFragmentBinding.inflate(inflater, container, false)
        return mBinding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        lifecycleScope.launch {
            isManual = measurementViewModel.getIsManual()
            if (isManual == 0) {
                mBinding.thermometerTv.isClickable = false
            }
        }

        initViews()
        initMeasurementObservers()


        initInputListeners()
        initClickListeners()
        initBluetoothObserver()
        initObservers()
    }

    private fun initMeasurementObservers() {
        measurementViewModel.temperature.observe(viewLifecycleOwner, {
            if (!it.isNullOrEmpty()) {
                mBinding.thermometerTv.text = getString(R.string.temperature_results, it)
                measurementListener?.handleDirection(ButtonDirection.NEXT, true)
            }
        })
    }

    private fun initBluetoothObserver() {
        bluetoothViewModel.bluetoothDevice.observe(viewLifecycleOwner, { bluetoothDevice ->
            bluetoothDevice?.let {
                bluetoothViewModel.stopScan()
                thermometerViewModel.connectDevice(it)
            }
        })

        bluetoothViewModel.rescan.observe(viewLifecycleOwner, { rescan ->
            if (rescan) {
                AppUtils.makeSnackbar(mBinding.bltTitle, "Couldn't find device, rescanning")
                val mac: String = BleDeviceTypes.getDeviceMAC(BleDeviceTypes.ES_021)!!.uppercase()
//                bluetoothViewModel.startScan(mac)
                bluetoothViewModel.resetScan()
            }
        })
    }

    private fun initObservers() {
        thermometerViewModel.thermometerTemperature.observe(viewLifecycleOwner, {
            isManualEntry=0
            mThermometerResult = it.toString()
            mBinding.thermometerTv.text = mThermometerResult.plus(" F°")
        })

        thermometerViewModel.isThermometerConnected.observe(viewLifecycleOwner, { isConnected ->
            if (isConnected) {
                mBinding.thermometerInstructions.text =
                    getString(R.string.device_ready_take_measurements)
            } else {
                mBinding.thermometerInstructions.text = getString(R.string.turn_on_thermometer)
            }
            mBinding.thermometerProgressBar.visibility = View.INVISIBLE
        })

        thermometerViewModel.isThermometerConnecting.observe(viewLifecycleOwner, { isConnected ->
            if (isConnected) {
                mBinding.thermometerInstructions.text =
                    getString(R.string.connecting_device_please_wait)
                mBinding.thermometerProgressBar.visibility = View.VISIBLE
            }
        })
    }


    private fun initClickListeners() {

        mBinding.thermometerTv.setOnClickListener {
            ManualTemperatureFragment {
                mThermometerResult = it
                mBinding.thermometerTv.text = it.plus(" F°")
            }.show(childFragmentManager, ManualTemperatureFragment.TAG)
        }

        mBinding.closeBtn.setOnClickListener {
            measurementListener?.onCloseMeasurement(BleDeviceTypes.ES_021)
        }
    }

    private fun initInputListeners() {

        val watcher = InputChangeWatcher {
            val temperature = mBinding.thermometerTv.text.toString().trim()
            measurementListener?.handleDirection(
                ButtonDirection.NEXT,
                temperature.isNotEmpty()
            )
        }

        mBinding.thermometerTv.addTextChangedListener(watcher.inputWatcher)
    }

    private fun initViews() {
        mBinding.thermometerProgressBar.visibility = View.INVISIBLE
    }

    override fun onResume() {
        super.onResume()
        measurementListener = parentFragment as MeasurementListener

        val mac = BleDeviceTypes.getDeviceMAC(BleDeviceTypes.ES_021)?.uppercase()
        mac?.let {
            if (it.isNotEmpty()) {
                thermometerViewModel.setUpListener()
                _handler.postDelayed({
                    bluetoothViewModel.startScan(mac)
                }, 1500)
            }
        }
    }

    override fun onPause() {
        super.onPause()
        _handler.removeCallbacksAndMessages(null)
        thermometerViewModel.closeDevice()
        bluetoothViewModel.stopScan()
        measurementListener = null
        if (mThermometerResult.isNotEmpty()) {
            measurementViewModel.saveThermometerResults(mThermometerResult)
        }
    }
}