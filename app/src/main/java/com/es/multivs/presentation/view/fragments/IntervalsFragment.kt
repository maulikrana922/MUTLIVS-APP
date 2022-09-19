package com.es.multivs.presentation.view.fragments

import android.os.Bundle
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
import com.es.multivs.data.bledevices.BleDeviceTypes
import com.es.multivs.data.utils.AppUtils
import com.es.multivs.data.utils.ButtonDirection
import com.es.multivs.data.utils.Constants
import com.es.multivs.data.utils.autoCleared
import com.es.multivs.databinding.FragmentIntervalsBinding
import com.es.multivs.presentation.view.UserDevicesViewModel
import com.es.multivs.presentation.view.fragments.measurements.MultiVsFragment
import com.es.multivs.presentation.view.fragments.measurements.OximeterFragment
import com.es.multivs.presentation.view.listener.OximeterListener
import com.es.multivs.presentation.view.viewmodels.ScheduleViewModel
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch

/**
 * Created by Dinesh on 10/03/2022.
 * Etrog Systems LTD.
 */

@AndroidEntryPoint
class IntervalsFragment : Fragment(), MeasurementListener, OximeterListener {

    companion object {
        fun newInstance() = IntervalsFragment()
        const val TAG = "IntervalsFragment"
    }

    private var binding: FragmentIntervalsBinding by autoCleared()
    private val scheduleViewModel: ScheduleViewModel by activityViewModels()
    private val userDevicesViewModel: UserDevicesViewModel by activityViewModels()

    private var devices: List<String> = mutableListOf()
    private var currentFragIndex = 0
    private var activeTimestamp: String? = null

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentIntervalsBinding.inflate(inflater, container, false)
        initData()
        return binding.root
    }

    private fun initData() {
        binding.closeBtn.setOnClickListener {
            Constants.isIntervalsOn = true
            Constants.isDisConnectDevice = true
            val viewPager2: ViewPager2 = requireActivity().findViewById(R.id.viewpager2)
            viewPager2.setCurrentItem(0, false)
        }
    }

    override fun onResume() {
        super.onResume()
        if (!AppUtils.isLocationEnabled(requireContext())) {
            AppUtils.showLocationAlertFragment(childFragmentManager)
        }
        Log.e("status isIntervalsOn",Constants.isIntervalsOn.toString())
        if (!Constants.isIntervalsOn) {
            showIntervalsMessage()
            binding.closeBtn.visibility = View.VISIBLE
        } else lifecycleScope.launch {
            activeTimestamp = scheduleViewModel.getActiveMeasurement()
            Log.d("MULTIVS", "onResume: $activeTimestamp")
            if (activeTimestamp.isNullOrEmpty()) {
                getAllDeviceTypes()
            } else {
                scheduleViewModel.recentMeasurements.forEach {
                    if (it.timeStamp == activeTimestamp) {
                        devices = configureDeviceIDs(it.deviceList)
                        if (devices[0] == BleDeviceTypes.ES_008)
                            loadFirstMeasurement()
                        else showIntervalsMessage()
                    }
                }

            }
        }
    }

    private fun showIntervalsMessage() {
        binding.noIntervals.noScheduleLayout.transitionToEnd()
        binding.noIntervals.noSchedulesTv.text =
            getString(R.string.no_intervals_active_now)
    }

    private fun getAllDeviceTypes() {
        userDevicesViewModel.getDevices().observe(viewLifecycleOwner) {
            it?.let {
                devices = it
                if (it[0] == BleDeviceTypes.ES_008) {
                    if (childFragmentManager.backStackEntryCount == 0)
                        loadFirstMeasurement()
                } else showIntervalsMessage()
            }
        }
    }

    private fun configureDeviceIDs(devices: ArrayList<String>): ArrayList<String> {
        val listToReturn = ArrayList<String>()
        var es008Added = false
        for (timeStamp in devices) {

            if (timeStamp.contains("MULTIVS")) {
                if (!es008Added) {
                    listToReturn.add(BleDeviceTypes.ES_008)
                    es008Added = true
                }
            } else {
                listToReturn.add(timeStamp)
            }
        }

        return listToReturn
    }

    private fun loadFirstMeasurement() {
        if (currentFragIndex >= devices.size) {
            currentFragIndex = 0;
        }
        val fragment: Fragment = intervalsFragmentFactory(devices[currentFragIndex])
        val transaction = childFragmentManager.beginTransaction()
        transaction.setCustomAnimations(
            R.anim.fade_in,
            R.anim.fade_out,
            R.anim.enter_from_left,
            R.anim.exit_to_right
        )

        transaction.add(R.id.intervals_container, fragment, devices[currentFragIndex])
            .addToBackStack(devices[currentFragIndex])
        transaction.commit()
    }

    private fun intervalsFragmentFactory(s: String): Fragment {
        var fragment: Fragment? = null
        when (s) {
            BleDeviceTypes.ES_020 -> fragment =
                OximeterFragment.newInstance(Constants.IntervalsFragment)
            BleDeviceTypes.ES_008 -> fragment =
                MultiVsFragment.newInstance(activeTimestamp, Constants.IntervalsFragment)
        }
        return fragment!!
    }

    override fun handleDirection(direction: ButtonDirection, enable: Boolean) {
        if (direction == ButtonDirection.NEXT && enable)
            fragmentTransactionSwitchCase(BleDeviceTypes.ES_020, BleDeviceTypes.ES_008)
    }

    override fun onBusy(busy: Boolean, msg: String) {

    }

    override fun onCloseMeasurement(tag: String, errorMessage: String?) {
        errorMessage?.let {
            if (getString(R.string.failed_to_connect_device) == it)
                AppUtils.makeErrorSnackbar(binding.info, it)
            else AppUtils.makeInfoSnackbar(binding.info, it)
        }

        childFragmentManager.commit {
            detach(childFragmentManager.findFragmentByTag(tag)!!)
            remove(childFragmentManager.findFragmentByTag(tag)!!)
            childFragmentManager.popBackStack()
        }
        val viewPager2: ViewPager2 = requireActivity().findViewById(R.id.viewpager2)
        viewPager2.setCurrentItem(0, false)
    }

    private fun fragmentTransactionSwitchCase(currentDeviceID: String, lastDeviceID: String = "") {
        val transaction = childFragmentManager.beginTransaction()
        val frag = intervalsFragmentFactory(currentDeviceID)
        if (lastDeviceID.isNotEmpty()) {
            childFragmentManager.findFragmentByTag(lastDeviceID)
                ?.let {
                    childFragmentManager.beginTransaction().detach(it).commit()
                    childFragmentManager.beginTransaction().remove(it).commit()
                }
        }
        childFragmentManager.popBackStack()
        transaction.setCustomAnimations(
            R.anim.fade_in,
            R.anim.fade_out,
            R.anim.enter_from_left,
            R.anim.exit_to_right
        )
        transaction.add(R.id.intervals_container, frag, currentDeviceID)
            .addToBackStack(currentDeviceID).commit()
    }

    override fun onExitFromOximeter(isSaved: Boolean, message: String) {
        message.let {
            AppUtils.makeInfoSnackbar(binding.info, it)
        }

        childFragmentManager.commit {
            detach(childFragmentManager.findFragmentByTag(tag)!!)
            remove(childFragmentManager.findFragmentByTag(tag)!!)
            childFragmentManager.popBackStack()
        }
        val viewPager2: ViewPager2 = requireActivity().findViewById(R.id.viewpager2)
        viewPager2.setCurrentItem(0, false)
    }

}