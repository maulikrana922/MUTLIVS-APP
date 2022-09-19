package com.es.multivs.presentation.view.fragments.calibration

import android.annotation.SuppressLint
import android.os.Bundle
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
import androidx.viewpager2.widget.ViewPager2
import com.es.multivs.R
import com.es.multivs.data.utils.AppUtils
import com.es.multivs.data.utils.Constants
import com.es.multivs.data.utils.SharedPrefs
import com.es.multivs.data.utils.autoCleared
import com.es.multivs.databinding.CalibrationsFragmentBinding
import com.es.multivs.presentation.view.viewmodels.MultiVsViewModel
import com.google.android.material.tabs.TabLayout
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@AndroidEntryPoint
class CalibrationFragment : Fragment(), BusyCallback {

    private var binding: CalibrationsFragmentBinding by autoCleared()
//    private val multiVsViewModel: MultiVsViewModel by navGraphViewModels(R.id.navigation)

    private val multiVsViewModel: MultiVsViewModel by activityViewModels()

    private var isMultiVSConnected = false
    private var tabLayout: TabLayout? = null
    private val handler = Handler(Looper.getMainLooper())

    companion object {
        fun newInstance() = CalibrationFragment()
        const val BLUETOOTH_DEVICE_KEY = "bluetooth_device"
        const val PATCH_UPPER_LEFT = "Patch_Upper_Left"
        const val BELT_MIDDLE = "Belt_Middle"
        const val PATCH_MIDDLE = "Patch_Middle"
        const val BP_MAC = "bp_mac"
        const val ES0008_MAC = "es008_mac"
        const val TAG = "CalibrationFragment"
    }


    @SuppressLint("SetTextI18n")
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {

        binding = CalibrationsFragmentBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        Log.d("_LIFECYCLE", "onViewCreated: $TAG")

        val set = SharedPrefs.getIntParam(Constants.CalibrationSet, 0)
        val measurement = SharedPrefs.getIntParam(Constants.Measurement, 0)

        if (set == 3 && measurement == 3) {
            SharedPrefs.setIntParam(Constants.CalibrationSet, 0)
            SharedPrefs.setIntParam(Constants.Measurement, 0)
        }
        binding.tvCalibration.text = "Done calibration set ${set}/3"
        binding.tvMeasurement.text = "Done measurement ${measurement}/3"

//        multiVsViewModel.isConnected.observe(viewLifecycleOwner, { connected ->
//            if (!connected) {
//                isMultiVSConnected = false
//
//            } else {
//                isMultiVSConnected = true
//            }
//        })
    }


    override fun onBusy(busy: Boolean, msg: String) {
        if (busy) {
            showLoading(msg)
        } else {
            dismissLoading()
        }
    }

    override fun onClose(tag: String) {
        Log.d("_SCANNING", "onClose: $tag")
        lifecycleScope.launch(Dispatchers.Main) {
            //onBusy(true, "Quitting calibrations, please wait")
//                    multiVsViewModel.wasConnected = false
            multiVsViewModel.stopTest(true)
            delay(200)
            multiVsViewModel.closeDevice()

            when (tag) {
                CalibrationFragment1.TAG -> {
                    childFragmentManager.commit {
                        childFragmentManager.findFragmentByTag(CalibrationFragment1.TAG)
                            ?.let { detach(it) }
                        childFragmentManager.findFragmentByTag(CalibrationFragment1.TAG)
                            ?.let { remove(it) }
                    }
                }
                CalibrationFragment2.TAG -> {
                    childFragmentManager.commit {
                        childFragmentManager.findFragmentByTag(CalibrationFragment2.TAG)
                            ?.let { detach(it) }
                        childFragmentManager.findFragmentByTag(CalibrationFragment2.TAG)
                            ?.let { remove(it) }
                    }
                }
                CalibrationFragment3.TAG -> {
                    childFragmentManager.commit {
                        childFragmentManager.findFragmentByTag(CalibrationFragment3.TAG)
                            ?.let { detach(it) }
                        childFragmentManager.findFragmentByTag(CalibrationFragment3.TAG)
                            ?.let { remove(it) }

                    }
                }
                CalibrationFragment4.TAG -> {
                    childFragmentManager.commit {
                        childFragmentManager.findFragmentByTag(CalibrationFragment4.TAG)
                            ?.let { detach(it) }
                        childFragmentManager.findFragmentByTag(CalibrationFragment4.TAG)
                            ?.let { remove(it) }

                    }
                }
            }
            onBusy(false, " ")
            val viewPager2: ViewPager2 = requireActivity().findViewById(R.id.viewpager2)
            viewPager2.setCurrentItem(0, false)
            tabLayout = requireActivity().findViewById(R.id.tab_layout)
            tabLayout?.visibility = View.VISIBLE
        }
    }

    override fun onResponse() {
        binding.tvCalibration.text =
            "Done calibration set ${SharedPrefs.getIntParam(Constants.CalibrationSet, 0)}/3"
        binding.tvMeasurement.text =
            "Done measurement ${SharedPrefs.getIntParam(Constants.Measurement, 0)}/3"
    }

    override fun onInstruction(msg: String) {
        if (msg == "")
            binding.instructions.visibility = View.GONE
        else {
            binding.instructions.visibility = View.VISIBLE
            binding.instructions.text = msg
        }
    }

    override fun onPause() {
        super.onPause()
        binding.instructions.visibility=View.GONE
        Log.d("_LIFECYCLE", "onPause: $TAG")
        multiVsViewModel.multivsActive = false

        AppUtils.dismissSnackbar()
    }

    override fun onResume() {
        super.onResume()
        Log.d("_LIFECYCLE", "onResume: $TAG")
        if (!AppUtils.isLocationEnabled(requireContext())) {
            AppUtils.showLocationAlertFragment(childFragmentManager)
        }
        childFragmentManager.commit {
            add(
                R.id.calibration_container,
                CalibrationFragment1.newInstance(),
                CalibrationFragment1.TAG
            )
        }

        multiVsViewModel.multivsActive = true
    }

    override fun onStart() {
        super.onStart()
        Log.d("_LIFECYCLE", "onStart: $TAG")
    }

    override fun onDestroyView() {
        super.onDestroyView()

        Log.d("_LIFECYCLE", "onDestroyView: $TAG")
    }

    override fun onDestroy() {
        super.onDestroy()
        Log.d("_LIFECYCLE", "onDestroy: $TAG")
    }

    private fun showLoading(msg: String) {
        binding.loadingLayout.visibility = View.VISIBLE
        binding.loadingLayout.isClickable = true
        binding.loadingLayout.isFocusable = true
        binding.info.text = msg
    }

    private fun dismissLoading() {
        binding.loadingLayout.visibility = View.GONE
        binding.loadingLayout.isClickable = false
        binding.loadingLayout.isFocusable = false
        binding.info.text = getString(R.string.empty)
    }
}