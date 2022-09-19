package com.es.multivs.presentation.adapters

import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import androidx.viewpager2.adapter.FragmentStateAdapter
import com.es.multivs.presentation.view.fragments.*
import com.es.multivs.presentation.view.fragments.calibration.CalibrationFragment
import java.lang.IllegalStateException

class ViewPagerAdapter(fa: FragmentActivity): FragmentStateAdapter(fa) {



    /**
     * Update this number every time a new fragment is added to the viewPager.
     */
    private val PAGE_COUNT = 6

    override fun getItemCount(): Int {
        return PAGE_COUNT
    }




    override fun createFragment(position: Int): Fragment {

        val frag: Fragment = when (position) {
            0 -> HealthHubFragment.newInstance()
            1 -> MeasurementFragment.newInstance()
           /* 2 -> MedicationsFragment.newInstance()
            3 -> SurveyFragment.newInstance()
            4 -> VideoVisitFragment.newInstance()*/
            2 -> CalibrationFragment.newInstance()
            3 -> ReportsFragment.newInstance()
            4 -> AboutFragment.newInstance()
            5 -> IntervalsFragment.newInstance()
            else -> throw IllegalStateException("Unexpected value: $position")
        }
        return frag
    }


}