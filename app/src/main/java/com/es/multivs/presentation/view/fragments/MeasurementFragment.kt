package com.es.multivs.presentation.view.fragments

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentTransaction
import androidx.fragment.app.activityViewModels
import androidx.fragment.app.commit
import androidx.lifecycle.lifecycleScope
import androidx.viewpager2.widget.ViewPager2
import com.es.multivs.R
import com.es.multivs.data.bledevices.BleDeviceTypes
import com.es.multivs.data.utils.*
import com.es.multivs.databinding.MeasurementFragmentBinding
import com.es.multivs.presentation.view.UserDevicesViewModel
import com.es.multivs.presentation.view.fragments.measurements.*
import com.es.multivs.presentation.view.viewmodels.ScheduleViewModel
import com.google.android.material.tabs.TabLayout
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
class MeasurementFragment : Fragment(), MeasurementListener, SummaryListener {

    companion object {
        fun newInstance() = MeasurementFragment()
        const val TAG = "MeasurementFragment"
    }

    private var binding: MeasurementFragmentBinding by autoCleared()
    private val scheduleViewModel: ScheduleViewModel by activityViewModels()
    private val userDevicesViewModel: UserDevicesViewModel by activityViewModels()

    @Inject
    lateinit var scheduleUpdates: ScheduleUpdatesManager

    private var devices: List<String> = mutableListOf()
    private var currentFragIndex = 0
    private var activeTimestamp: String? = null

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = MeasurementFragmentBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        Log.d("_SCANNING", "onViewCreated: $TAG")
        initListeners()
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

        binding.previousBtn.visibility = View.GONE
//        if (binding.skipBtn.visibility == View.GONE) {
//            binding.skipBtn.visibility = View.VISIBLE
//        }

        if (currentFragIndex >= devices.size) {
            currentFragIndex = 0;
        }

        val fragment: Fragment = measurementFragmentFactory(devices[currentFragIndex])
        val transaction = childFragmentManager.beginTransaction()
        transaction.setCustomAnimations(
            R.anim.fade_in,
            R.anim.fade_out,
            R.anim.enter_from_left,
            R.anim.exit_to_right
        )

        transaction.add(R.id.measurement_container, fragment, devices[currentFragIndex])
            .addToBackStack(devices[currentFragIndex])
        transaction.commit()

        handleDirection(ButtonDirection.NEXT, false)
    }

    private fun measurementFragmentFactory(s: String): Fragment {
        var fragment: Fragment? = null
        when (s) {
            BleDeviceTypes.ES_022 -> fragment = BpFragment.newInstance()
            BleDeviceTypes.ES_021 -> fragment = ThermometerFragment.newInstance()
            BleDeviceTypes.ES_020 -> fragment =
                OximeterFragment.newInstance(Constants.MeasurementsFragment)
            BleDeviceTypes.ES_023 -> fragment = WeightScaleFragment.newInstance()
            BleDeviceTypes.ES_024 -> fragment = GlucometerFragment.newInstance()
            BleDeviceTypes.ES_008 -> fragment =
                MultiVsFragment.newInstance(activeTimestamp, Constants.MeasurementsFragment)
        }

        return fragment!!
    }

    private fun clearFragmentBackStack() {
        childFragmentManager.popBackStackImmediate()
        currentFragIndex = 0
    }

    private fun getAllDeviceTypes() {
        userDevicesViewModel.getDevices().observe(viewLifecycleOwner) {
            it?.let {
                devices = it
                if (childFragmentManager.backStackEntryCount == 0) {
                    loadFirstMeasurement()
                }
            }
        }
    }

    private fun initListeners() {
        binding.nextBtn.setOnClickListener {
            onNextFragment()
        }

        binding.previousBtn.setOnClickListener {
            onBackFragment()
        }

        binding.skipBtn.setOnClickListener {
            onNextFragment()
        }
        Constants.isManualEntry = 1
    }

    private fun onBackFragment() {
        try {
            if (currentFragIndex == 1) {
                binding.previousBtn.visibility = View.GONE
            }

//            binding.skipBtn.visibility = View.VISIBLE

            val fragName =
                childFragmentManager.getBackStackEntryAt(childFragmentManager.backStackEntryCount - 1).name

            if (SummaryFragment.TAG == fragName) {
                childFragmentManager.popBackStack()
                val currentDeviceID = devices[currentFragIndex]
                fragmentTransactionSwitchCase(currentDeviceID)
            } else {
                val lastDeviceID = devices[currentFragIndex]
                currentFragIndex--
                val currentDeviceID = devices[currentFragIndex]
                fragmentTransactionSwitchCase(currentDeviceID, lastDeviceID)
            }
        } catch (e: Exception) {
            println(e.message)
        }
    }

    private fun onNextFragment() {
        binding.previousBtn.visibility = View.VISIBLE

//        binding.nextBtn.isEnabled = false
//        binding.nextBtn.visibility = View.INVISIBLE

        val lastDeviceID: String = devices[currentFragIndex]

        if (currentFragIndex == devices.size - 1) {
            if (BleDeviceTypes.ES_008 == lastDeviceID) {

                // this line needs to happen before clearing the backstack
                onCloseMeasurement(BleDeviceTypes.ES_008)

                clearFragmentBackStack()
                return
            }

            childFragmentManager.popBackStack()
//            binding.skipBtn.visibility = View.GONE


            val summaryFragment = SummaryFragment.newInstance()
            childFragmentManager.beginTransaction()
                .setTransition(FragmentTransaction.TRANSIT_FRAGMENT_OPEN)
                .add(R.id.measurement_container, summaryFragment, SummaryFragment.TAG)
                .addToBackStack(SummaryFragment.TAG).commit()

        } else {
            val lastDeviceID = devices[currentFragIndex]
            currentFragIndex++
            val currentDeviceID = devices[currentFragIndex]
            fragmentTransactionSwitchCase(currentDeviceID, lastDeviceID)
        }
    }

    private fun fragmentTransactionSwitchCase(currentDeviceID: String, lastDeviceID: String = "") {
        val transaction = childFragmentManager.beginTransaction()

        val frag = measurementFragmentFactory(currentDeviceID)

        if (lastDeviceID.isNotEmpty()) {
            /* childFragmentManager.commit {
                 detach(childFragmentManager.findFragmentByTag(lastDeviceID)!!)
                 remove(childFragmentManager.findFragmentByTag(lastDeviceID)!!)
             }*/
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

        transaction.add(R.id.measurement_container, frag, currentDeviceID)
            .addToBackStack(currentDeviceID).commit()
    }

    override fun handleDirection(direction: ButtonDirection, enable: Boolean) {
        if (direction == ButtonDirection.NEXT) {
//            if (enable) {
//                binding.nextBtn.isEnabled = true
//                binding.nextBtn.visibility = View.VISIBLE
//            } else {
//                binding.nextBtn.isEnabled = false
//                binding.nextBtn.visibility = View.INVISIBLE
//            }
        } else if (direction == ButtonDirection.PREVIOUS) {
            if (enable) {
                binding.previousBtn.isEnabled = true
                binding.previousBtn.visibility = View.VISIBLE
            } else {
                binding.previousBtn.isEnabled = false
                binding.previousBtn.visibility = View.INVISIBLE
            }
        }
    }

    override fun onBusy(busy: Boolean, msg: String) {
        if (busy) {
            showLoading(msg)
        } else {
            dismissLoading()
        }
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

    override fun onCloseMeasurement(tag: String, errorMessage: String?) {

        errorMessage?.let {
            AppUtils.makeErrorSnackbar(binding.info, it)
        }

        childFragmentManager.commit {
            detach(childFragmentManager.findFragmentByTag(tag)!!)
            remove(childFragmentManager.findFragmentByTag(tag)!!)
            childFragmentManager.popBackStack()
        }
        val viewPager2: ViewPager2 = requireActivity().findViewById(R.id.viewpager2)
        viewPager2.setCurrentItem(0, false)

        val tabLayout = requireActivity().findViewById<TabLayout>(R.id.tab_layout)
        tabLayout?.visibility = View.VISIBLE
    }

    override fun onResume() {
        super.onResume()

        Log.d("_SCANNING", "onResume: $TAG")
        if (!AppUtils.isLocationEnabled(requireContext())) {
            AppUtils.showLocationAlertFragment(childFragmentManager)
        }

        lifecycleScope.launch {
            activeTimestamp = scheduleViewModel.getActiveMeasurement()
            Log.d("MULTIVS", "onResume: $activeTimestamp")
            if (activeTimestamp.isNullOrEmpty()) {
                getAllDeviceTypes()
            } else {
                scheduleViewModel.recentMeasurements.forEach {
                    if (it.timeStamp == activeTimestamp) {
                        devices = configureDeviceIDs(it.deviceList)
                        loadFirstMeasurement()
                    }
                }

            }
        }
    }

    override fun onExitFromSummary(isSaved: Boolean, message: String) {


        childFragmentManager.commit {
            detach(childFragmentManager.findFragmentByTag(SummaryFragment.TAG)!!)
            remove(childFragmentManager.findFragmentByTag(SummaryFragment.TAG)!!)
            childFragmentManager.popBackStack()
        }

        clearFragmentBackStack()

        if (isSaved) {
            if (message.isNotEmpty()) {
                AppUtils.makeInfoSnackbar(binding.info, message)

                scheduleUpdates.measurementsComplete(activeTimestamp ?: "")
            }
        } else {
            if (message.isNotEmpty()) {
                AppUtils.showAlertFragment(
                    parentFragmentManager,
                    getString(R.string.measurements),
                    message
                )
            }
        }

        val viewPager2: ViewPager2 = requireActivity().findViewById(R.id.viewpager2)
        viewPager2.setCurrentItem(0, false)

        val tabLayout = requireActivity().findViewById<TabLayout>(R.id.tab_layout)
        tabLayout?.visibility = View.VISIBLE
    }
}