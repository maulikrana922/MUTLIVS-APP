package com.es.multivs.presentation.view.fragments.measurements

import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.ListPopupWindow
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
import com.es.multivs.data.utils.autoCleared
import com.es.multivs.databinding.GlucometerFragmentBinding
import com.es.multivs.presentation.view.fragments.MeasurementListener
import com.es.multivs.presentation.view.fragments.measurements.manualinputs.ManualGlucoseFragment
import com.es.multivs.presentation.view.viewmodels.BluetoothViewModel
import com.es.multivs.presentation.view.viewmodels.GlucometerViewModel
import com.es.multivs.presentation.view.viewmodels.MeasurementViewModel
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch

/**
 * Created by Marko on 11/8/2021.
 * Etrog Systems LTD.
 */
@AndroidEntryPoint
class GlucometerFragment : Fragment() {

    private var mBinding: GlucometerFragmentBinding by autoCleared()
    private val glucometerViewModel: GlucometerViewModel by viewModels()
    private val measurementViewModel: MeasurementViewModel by activityViewModels()
    private val bluetoothViewModel: BluetoothViewModel by viewModels()
    private var measurementListener: MeasurementListener? = null

    private lateinit var listPopupWindow: ListPopupWindow
    private lateinit var mEventTag: String
    private var mEventTagIndex = -1
    private var mIsGlucometerConnected = false
    private var mGlucoseLevel = 0
    private lateinit var _handler: Handler

    companion object {
        fun newInstance(): GlucometerFragment = GlucometerFragment()
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _handler = Handler(Looper.getMainLooper())
        mBinding = GlucometerFragmentBinding.inflate(inflater, container, false)
        return mBinding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)


        lifecycleScope.launch {
            val isManual = measurementViewModel.getIsManual()
            if (isManual == 0) {
                mBinding.glucometerTv.isClickable = false
            }
        }

        setPopUpMenu()
        initViews()
        initClickListeners()
        initAndSearchForGlucose()
        initMeasurementObservers()
        initGlucoseObservers()

        createInputWatcher()
    }

    private fun initViews() {

        mEventTag = getString(R.string.before_meal)

        if (!mIsGlucometerConnected) {
            mBinding.glucometerInstructions.text = getString(R.string.glucose_instructions)
        }

        mBinding.glucometerProgressBar.visibility = View.INVISIBLE
    }

    private fun setPopUpMenu() {
        listPopupWindow = ListPopupWindow(requireContext(), null, R.attr.listPopupWindowStyle)
        listPopupWindow.anchorView = mBinding.glucometerEventDd
        val items = listOf(
            getString(R.string.before_meal),
            getString(R.string.after_meal),
            getString(R.string.exercise),
            getString(R.string.medications),
            getString(R.string.sick),
            getString(R.string.other)
        )

        val adapter = ArrayAdapter(requireContext(), R.layout.glucose_list_popup_window_item, items)
        listPopupWindow.setAdapter(adapter)

        listPopupWindow.setOnItemClickListener { _, _, index, _ ->
            mEventTagIndex = index
            setEventTag(mEventTagIndex)
            listPopupWindow.dismiss()
        }
    }

    private fun setEventTag(index: Int) {
        when (index) {
            0 -> {
                mEventTag = getString(R.string.before_meal)
            }
            1 -> {
                mEventTag = getString(R.string.after_meal)
            }
            2 -> {
                mEventTag = getString(R.string.exercise)
            }
            3 -> {
                mEventTag = getString(R.string.medications)
            }
            4 -> {
                mEventTag = getString(R.string.sick)
            }
            5 -> {
                mEventTag = getString(R.string.other)
            }
        }
        mBinding.glucometerEventDd.text = mEventTag
    }

    private fun initMeasurementObservers() {
        measurementViewModel.glucoseData.observe(viewLifecycleOwner) {
            if (it.eventTagIndex >= 0 && it.glucose > 0) {
                mBinding.glucometerTv.text = getString(R.string.glucose_results, it.glucose)
                mGlucoseLevel = it.glucose
                setEventTag(it.eventTagIndex)

                measurementListener?.handleDirection(ButtonDirection.NEXT, true)
            }
        }
    }

    private fun initAndSearchForGlucose() {
        bluetoothViewModel.bluetoothDevice.observe(viewLifecycleOwner) {
            bluetoothViewModel.stopScan()
            glucometerViewModel.connectDevice(it)
        }

        bluetoothViewModel.rescan.observe(viewLifecycleOwner) { rescan ->
            if (rescan) {
                AppUtils.makeSnackbar(mBinding.closeBtn, "Couldn't find device, rescanning")
                val mac: String = BleDeviceTypes.getDeviceMAC(BleDeviceTypes.ES_024)!!.uppercase()
                bluetoothViewModel.startScan(mac)
                bluetoothViewModel.resetScan()
            }
        }
    }

    private fun initGlucoseObservers() {
        glucometerViewModel.glucose.observe(viewLifecycleOwner) {
            if (it.isNotEmpty()) {
                isManualEntry = 0
                val indexOfPeriod: Int = it.indexOf(".")
                mGlucoseLevel = it.substring(0, indexOfPeriod).toInt()
                mBinding.glucometerTv.text = it.substring(0, indexOfPeriod).plus(" mg / dl")
                mBinding.glucometerProgressBar.visibility = View.INVISIBLE
                mBinding.glucometerInstructions.text = getString(R.string.measuring_complete)
            } else {
                mBinding.glucometerProgressBar.visibility = View.INVISIBLE
                mBinding.glucometerInstructions.text =
                    getString(R.string.no_recent_measurements_found)
            }
        }

        glucometerViewModel.isGlucometerConnected.observe(viewLifecycleOwner) { isConnected ->
            if (isConnected) {
                mBinding.glucometerProgressBar.visibility = View.VISIBLE
                mBinding.glucometerInstructions.text =
                    getString(R.string.glucose_connected_fetching_data)
                mIsGlucometerConnected = true
            } else {
                mBinding.glucometerProgressBar.visibility = View.INVISIBLE
                mBinding.glucometerInstructions.text = getString(R.string.connect_glucose)
                mIsGlucometerConnected = false
            }
        }
    }

    private fun createInputWatcher() {

        val watcher = InputChangeWatcher {
            val tag = mBinding.glucometerEventDd.text.toString().trim()
            val glucose = mBinding.glucometerTv.text.toString().trim()
            val isInputOK = tag.isNotEmpty() && glucose.isNotEmpty()

            measurementListener?.handleDirection(ButtonDirection.NEXT, isInputOK)
            if (isInputOK) {
                mBinding.glucometerInstructions.text =
                    getString(R.string.measure_complete_turn_off_device)
            } else {
                mBinding.glucometerInstructions.text = getString(R.string.glucose_instructions)
            }
        }

        mBinding.glucometerTv.addTextChangedListener(watcher.inputWatcher)
        mBinding.glucometerEventDd.addTextChangedListener(watcher.inputWatcher)
    }

    private fun initClickListeners() {
        mBinding.glucometerEventDd.setOnClickListener {
            listPopupWindow.show()
        }

        mBinding.glucometerTv.setOnClickListener {
            ManualGlucoseFragment { glucose ->
                mGlucoseLevel = glucose
                mBinding.glucometerTv.text = getString(R.string.glucose_results, mGlucoseLevel)
            }.show(childFragmentManager, ManualGlucoseFragment.TAG)
        }

        mBinding.closeBtn.setOnClickListener {
            bluetoothViewModel.stopScan()
            glucometerViewModel.closeDevice()
            measurementListener?.onCloseMeasurement(BleDeviceTypes.ES_024)
        }
    }

    override fun onResume() {
        super.onResume()
        measurementListener = parentFragment as MeasurementListener
        glucometerViewModel.initializeDevice()
        Log.d("_SCANNING", "onResume: GLUCOMETER")
        val mac = BleDeviceTypes.getDeviceMAC(BleDeviceTypes.ES_024)?.uppercase()
        mac?.let {
            _handler.postDelayed({
                bluetoothViewModel.startScan(it)
            }, 2000)
        }
    }

    override fun onPause() {
        super.onPause()
        _handler.removeCallbacksAndMessages(null)
        bluetoothViewModel.stopScan()
        glucometerViewModel.closeDevice()
        if (listPopupWindow.isShowing) {
            listPopupWindow.dismiss()
        }

        if (mGlucoseLevel > 0) {
            measurementViewModel.saveGlucometerResults(mGlucoseLevel, mEventTagIndex)
        }


        measurementListener = null


    }
}