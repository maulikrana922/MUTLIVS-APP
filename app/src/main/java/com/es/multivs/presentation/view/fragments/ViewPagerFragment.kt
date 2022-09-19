package com.es.multivs.presentation.view.fragments

import android.graphics.ColorFilter
import android.graphics.PorterDuff
import android.graphics.PorterDuffColorFilter
import android.os.Build
import android.os.Bundle
import android.os.CountDownTimer
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.viewpager2.widget.ViewPager2
import com.es.multivs.R
import com.es.multivs.data.utils.Constants
import com.es.multivs.data.utils.autoCleared
import com.es.multivs.databinding.TabAndViewpagerLayoutBinding
import com.es.multivs.presentation.adapters.ViewPagerAdapter
import com.es.multivs.presentation.view.viewmodels.ScheduleViewModel
import com.google.android.material.tabs.TabLayout
import com.google.android.material.tabs.TabLayoutMediator
import dagger.hilt.android.AndroidEntryPoint
import java.util.concurrent.TimeUnit

/**
 * created by Marko
 * Etrog Systems LTD. 1/9/2021.
 */
@AndroidEntryPoint
class ViewPagerFragment : Fragment() {

    var binding: TabAndViewpagerLayoutBinding by autoCleared()
    private val viewModel: ScheduleViewModel by activityViewModels()
    private var isIntervalTab = false
    private var isTimerStop = false
    private lateinit var countDownTimer: CountDownTimer

    companion object {
        fun newInstance() = ViewPagerFragment()
    }

    private lateinit var mViewPagerAdapter: ViewPagerAdapter
    private var intervalTime = 1L

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {

        binding = TabAndViewpagerLayoutBinding.inflate(inflater, container, false)
        mViewPagerAdapter = ViewPagerAdapter(requireActivity())
        initData()
        setViewPager2()

        TabLayoutMediator(binding.tabLayout, binding.viewpager2, true, false)
        { tab, position ->
            when (position) {
                0 -> {
                    tab.setIcon(R.drawable.icon_concierge)
                    tab.text = "Health Hub"
                    tab.icon?.bounds
                }
                1 -> {
                    tab.setIcon(R.drawable.icon_measurements)
                    tab.text = "Measurement"
                    tab.tag = "Measurement"

                }
                /*      2 -> {
                          tab.setIcon(R.drawable.icon_medication)
                          tab.text = "Medications"
                      }
                      3 -> {
                          tab.setIcon(R.drawable.icon_survey)
                          tab.text = "Survey"
                      }
                      4 -> {
                          tab.setIcon(R.drawable.icon_communicator)
                          tab.text = "Video Visit"
                      }*/
                2 -> {
                    tab.setIcon(R.drawable.ic_celibration)
                    tab.text = "Calibration"
                    tab.tag = "Calibration"
                }
                3 -> {
                    tab.setIcon(R.drawable.icon_reports)
                    tab.text = "Reports"
                }
                4 -> {
                    tab.setIcon(R.drawable.ic_about)
                    tab.text = "About"
                }
                5 -> {
                    tab.setIcon(R.drawable.ic_intervals)
                    tab.text = "Intervals"
                }

            }
        }.attach()

        setTabLayout()

        return binding.root
    }

    private fun initData() {

//        if (Constants.interval.isNotEmpty() && (Constants.isCheckedEcg || Constants.isCheckedPpg)) {
//            showTimer(Constants.interval.toLong())
//        }

        viewModel.showTimerLiveData.observe(viewLifecycleOwner) {
            it.let {
                binding.tabLayout.getTabAt(5)!!.view.isEnabled = false
                binding.tabLayout.getTabAt(5)!!.view.alpha = 0f
                if (it)
                    showTimer(Constants.interval.toLong())
                else {
                    binding.tvTime.visibility = View.GONE
                    enableTab()
                    if (::countDownTimer.isInitialized)
                        countDownTimer.cancel()
                }
            }
        }
    }

    private fun showTimer(interval: Long) {
        disableTab()
        intervalTime = Constants.interval.toLong()

        if (::countDownTimer.isInitialized) {
            countDownTimer.cancel()
        }
        binding.tvTime.visibility = View.VISIBLE
        val millis = TimeUnit.MINUTES.toMillis(interval)
        countDownTimer = object : CountDownTimer(millis, 1000) {
            override fun onTick(millis: Long) {
                try {
                    val sec = (millis / 1000).toInt() % 60
                    val min = (millis / (1000 * 60) % 60).toInt()
                    val hours = (millis / (1000 * 60 * 60) % 24).toInt()
                    binding.tvTime.text =
                        (if (Constants.isMULTIVS) "Interval Active (MULTIVS): " else "Interval Active (MULTIVS + SPO2): ").plus(
                            String.format(
                                "%02d:%02d:%02d",
                                hours,
                                min,
                                sec
                            )
                        )
                } catch (e: Exception) {
                    println(e.message)
                }
            }

            override fun onFinish() {
                isTimerStop = true
                if (binding.tabLayout.selectedTabPosition == 0)
                    binding.tabLayout.getTabAt(4)!!.select()
                binding.tvTime.visibility = View.GONE
                Constants.isIntervalsOn = true
                if (Constants.isIntervals) {
                    Constants.isIntervals = false
                }

            }
        }
        countDownTimer.start()
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        viewModel.serverLocation.observe(viewLifecycleOwner) {
            if (it != getString(R.string.usa)) {
                Log.d("TAGTAG", "onViewCreated: serverLocation")
                binding.clock.format24Hour = "dd-MMM-yyyy hh:mm"
            }
        }
    }

    private fun setViewPager2() {

        binding.viewpager2.apply {
            offscreenPageLimit = ViewPager2.OFFSCREEN_PAGE_LIMIT_DEFAULT
            isUserInputEnabled = false
            adapter = mViewPagerAdapter
        }

    }

    private fun disableTab() {
        for (i in 1 until 5) {
            binding.tabLayout.getTabAt(i)!!.view.isEnabled = false
            binding.tabLayout.getTabAt(i)!!.view.alpha = 0.5f
        }
    }

    private fun enableTab() {
        for (i in 1 until 5) {
            binding.tabLayout.getTabAt(i)!!.view.isEnabled = true
            binding.tabLayout.getTabAt(i)!!.view.alpha = 1f
        }
    }

    private fun setTabLayout() {

        binding.tabLayout.elevation = 1.3f
        binding.tabLayout.addOnTabSelectedListener(object : TabLayout.OnTabSelectedListener {
            override fun onTabSelected(tab: TabLayout.Tab?) {
                binding.txtLabelWhatNextToDay.visibility = View.GONE
                Constants.steps = "0"
                Constants.batteryLevel = "0"
                when (tab?.position) {
                    0 -> {
                        Log.e("tab", "Home Tab")
                        binding.txtLabelWhatNextToDay.visibility = View.VISIBLE
                        binding.tabLayout.visibility = View.VISIBLE
                        if (Constants.isDisConnectDevice && isTimerStop && Constants.isIntervalsOn) {
                            showTimer(intervalTime)
                            isTimerStop = false
                        }
                    }
                    1 -> binding.tabLayout.visibility = View.GONE
                    2 -> binding.tabLayout.visibility = View.GONE
                    5 -> {
                        isIntervalTab = true
                        binding.tabLayout.visibility = View.GONE
                    }
                    else -> {
                        if (isIntervalTab) {
                            isIntervalTab = false
                        }
                    }
                }
                setIconColor(R.color.colorPrimary, tab)
            }

            override fun onTabUnselected(tab: TabLayout.Tab?) {
                tab?.tag?.let {
                    if ((it as String) == "Measurements") {
                        binding.viewpager2.adapter?.notifyItemChanged(1)
                    }
                }
                tab?.icon?.clearColorFilter()

            }

            override fun onTabReselected(tab: TabLayout.Tab?) {
                //TODO("Not yet implemented")
            }
        })
    }

    private fun setIconColor(color: Int, tab: TabLayout.Tab?) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            val filter: ColorFilter =
                PorterDuffColorFilter(
                    resources.getColor(R.color.colorPrimary, null),
                    PorterDuff.Mode.SRC_IN
                )
            tab?.icon?.colorFilter = filter
        } else {
            tab?.icon?.setColorFilter(
                resources.getColor(color),
                PorterDuff.Mode.SRC_IN
            )
        }
    }
}