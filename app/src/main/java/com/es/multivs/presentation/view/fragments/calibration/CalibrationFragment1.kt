package com.es.multivs.presentation.view.fragments.calibration

import android.bluetooth.BluetoothManager
import android.content.Context
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.app.AlertDialog
import androidx.core.os.bundleOf
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.fragment.app.commit
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import com.es.multivs.R
import com.es.multivs.data.bledevices.BleDeviceTypes
import com.es.multivs.data.utils.AppUtils
import com.es.multivs.data.utils.Constants
import com.es.multivs.databinding.CalibrationsFragment1Binding
import com.es.multivs.presentation.view.fragments.calibration.CalibrationFragment.Companion.BLUETOOTH_DEVICE_KEY
import com.es.multivs.presentation.view.fragments.calibration.CalibrationFragment.Companion.BP_MAC
import com.es.multivs.presentation.view.fragments.calibration.CalibrationFragment.Companion.ES0008_MAC
import com.es.multivs.presentation.view.viewmodels.BluetoothViewModel
import com.es.multivs.presentation.view.viewmodels.UserDetailsViewModel
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.*
import java.lang.Runnable

@AndroidEntryPoint
class CalibrationFragment1 : Fragment() {

    private var bluetoothRefreshAlert: AlertDialog? = null

    companion object {
        fun newInstance(): CalibrationFragment1 = CalibrationFragment1()
        const val TAG = "CalibrationFragment1"
    }

    private lateinit var mBusyCallback: BusyCallback

    lateinit var binding: CalibrationsFragment1Binding

    private val bluetoothViewModel: BluetoothViewModel by viewModels()
    private val userDetailsViewModel: UserDetailsViewModel by activityViewModels()

    private lateinit var _handler: Handler
    private var _bpMAc: String? = null
    private var _es008Mac: String? = null
    private var deviceFound = false

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        binding = CalibrationsFragment1Binding.inflate(inflater, container, false)

        // get the host fragment of the NavHostFragment
        mBusyCallback = parentFragment as BusyCallback
        return binding.root
    }

    private fun showBluetoothRefreshAlert() {

        bluetoothRefreshAlert = AlertDialog.Builder(requireContext()).create()
        bluetoothRefreshAlert?.apply {
            val layoutInflater = LayoutInflater.from(requireActivity())
            val promptView: View = layoutInflater.inflate(R.layout.refresh_layout, null)
            setView(promptView)
            setCancelable(false)
            show()
        }
    }

    private fun hideBluetoothRefreshAlert() {
        bluetoothRefreshAlert?.apply {
            if (isShowing) {
                dismiss()
            }
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        Log.d("_LIFECYCLE", "onViewCreated: $TAG")

        binding.closeBtn.setOnClickListener {
            mBusyCallback.onClose(TAG)
        }

        _bpMAc = BleDeviceTypes.getDeviceMAC(BleDeviceTypes.ES_022)?.uppercase()
        _es008Mac = BleDeviceTypes.getDeviceMAC(BleDeviceTypes.ES_008)?.uppercase()
        Log.d("SCANNING", "onViewCreated: SCANNING FOR $_es008Mac")
        _handler = Handler(Looper.getMainLooper())

        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.P) {
            lifecycleScope.launch {
                val mBluetoothManager =
                    requireActivity().getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager
                val mBluetoothAdapter = mBluetoothManager.adapter
                showBluetoothRefreshAlert()
                delay(300)
                mBluetoothAdapter.disable()
                _handler.postDelayed(object : Runnable {
                    override fun run() {
                        Log.d("_BLUETOOTH", "is enabled: ${mBluetoothAdapter.isEnabled}")
                        if (!mBluetoothAdapter.isEnabled) {
                            lifecycleScope.launch {
                                mBluetoothAdapter.enable()
                                delay(2000)
                                hideBluetoothRefreshAlert()
                                initManualInputAndConfigurations()
                            }
                        } else {
                            _handler.postDelayed(this, 2500)
                        }
                    }
                }, 2500)
            }
        } else {
            initManualInputAndConfigurations()
        }
    }

    private fun initManualInputAndConfigurations() {
        userDetailsViewModel.manualInput.observe(viewLifecycleOwner) { isManualInput ->
            isManualInput?.let {
                if (_es008Mac.isNullOrEmpty()) {
                    //binding.tvInstructions.text = getString(R.string.no_calibrations_needed)
                    mBusyCallback.onInstruction(getString(R.string.no_calibrations_needed))
                    binding.closeBtn.apply {
                        visibility = View.VISIBLE
                        // isEnabled = true
                    }
                } else if ((_bpMAc.isNullOrEmpty() && it == 0)) {
//                    binding.tvInstructions.text =
//                        getString(R.string.multi_vs_init_instructions)
                    mBusyCallback.onInstruction(getString(R.string.multi_vs_init_instructions))
                } else {
                    searchForMultiVS()
                }
            }
        }
    }

    private var searchEventErrorRunnable = Runnable {
        AppUtils.makeErrorSnackbar(binding.tvInstructions, "MULTIVS not found", 5000)

        bluetoothViewModel.stopScan()
        Handler(Looper.getMainLooper()).postDelayed({
            //mBusyCallback.onBusy(false)
            mBusyCallback.onClose("CalibrationFragment1")
        }, 3000)

    }

    private fun searchForMultiVS() {
        //mBusyCallback.onBusy(true, getString(R.string.searching_for_multivs))
        lifecycleScope.launch {
            bluetoothViewModel.startScan(_es008Mac!!)
            Log.d("_SCANNING", "searchForMultiVS: SEARCHING FOR MULTIVS IN CALIBRATIONFRAGMENT1")
            _handler.postDelayed({ searchEventErrorRunnable }, 20000)
        }

        bluetoothViewModel.rescan.observe(viewLifecycleOwner) {
            Log.d("_SCANNING", "searchForMultiVS:  rescan OBSERVER")
            searchEventErrorRunnable.run()
        }

        bluetoothViewModel.isScanning.observe(viewLifecycleOwner) {
            Log.e("isScanning", it.toString())
            if (it) {
                //binding.tvInstructions.text = getText(R.string.search_for_multivs)
                mBusyCallback.onInstruction(getString(R.string.search_for_multivs))
            }
        }

        bluetoothViewModel.bluetoothDevice.observe(viewLifecycleOwner) { device ->
            device?.let {
                try {
                    mBusyCallback.onInstruction(getString(R.string.connecting_device_please_wait))
                    //binding.tvInstructions.text = getString(R.string.connecting_device_please_wait)
                    bluetoothViewModel.bluetoothDevice.removeObservers(viewLifecycleOwner)
                    if (!deviceFound) {
                        deviceFound = true
                        // bluetoothViewModel.bluetoothDevice.removeObservers(viewLifecycleOwner)
                        // bluetoothViewModel.stopScan()
                        //binding.tvInstructions.text = getString(R.string.connecting_device_please_wait)
                        //binding.instructions.text = getString(R.string.multivs_found)
                        Constants.isMultivs = true
                        _handler.postDelayed({

                            val bundle = bundleOf(
                                BLUETOOTH_DEVICE_KEY to device,
                                BP_MAC to _bpMAc,
                                ES0008_MAC to _es008Mac
                            )
                            try {
                                binding.tvInstructions.visibility = View.INVISIBLE
                                //mBusyCallback.onBusy(false)
                                parentFragmentManager.commit {
                                    setCustomAnimations(
                                        R.anim.fade_in,
                                        R.anim.fade_out,
                                        R.anim.fade_in,
                                        R.anim.fade_out
                                    )
                                    replace(
                                        R.id.calibration_container,
                                        CalibrationFragment2::class.java,
                                        bundle,
                                        CalibrationFragment2.TAG
                                    )
                                }
                            } catch (e: Exception) {
                                print(e.message)
                            }
                        }, 1500)
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
        }
    }

    override fun onPause() {
        super.onPause()
        Log.d("_LIFECYCLE", "onPause: $TAG")
        mBusyCallback.onInstruction("")
        bluetoothViewModel.stopScan()
        _handler.removeCallbacks(searchEventErrorRunnable)
    }

    override fun onResume() {
        super.onResume()
        Log.d("_LIFECYCLE", "onResume: $TAG")
    }

    override fun onStart() {
        super.onStart()
        Log.d("_LIFECYCLE", "onStart: $TAG")
    }

    override fun onDestroyView() {
        binding.tvInstructions.text = ""
        super.onDestroyView()
        bluetoothViewModel.stopScan()
        Log.d("_LIFECYCLE", "onDestroyView: $TAG")
    }

    override fun onDestroy() {
        super.onDestroy()
        bluetoothViewModel.stopScan()
        Log.d("_LIFECYCLE", "onDestroy: $TAG")
    }
}