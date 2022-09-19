package com.es.multivs.presentation.view.fragments.calibration

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.os.bundleOf
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.fragment.app.commit
import com.es.multivs.R
import com.es.multivs.data.utils.Constants
import com.es.multivs.databinding.CalibrationsFragment3Binding
import com.es.multivs.presentation.view.fragments.measurements.manualinputs.ManualBpFragment
import com.es.multivs.presentation.view.viewmodels.MultiVsViewModel
import com.es.multivs.presentation.view.viewmodels.UserDetailsViewModel


/**
 * Created by Marko on 10/27/2021.
 * Etrog Systems LTD.
 */
class CalibrationFragment3 : Fragment() {

    lateinit var mBinding: CalibrationsFragment3Binding
    private val userDetailsViewModel: UserDetailsViewModel by activityViewModels()
    private val multiVsViewModel: MultiVsViewModel by activityViewModels()

    companion object {
        fun newInstance(): CalibrationFragment3 = CalibrationFragment3()
        const val USE_BP_DEVICE = "use_device"
        const val SYS = "manual_sys"
        const val DIA = "manual_dia"
        const val TAG = "CalibrationFragment3"

    }

    private lateinit var mBusyCallback: BusyCallback

    private var mSYS: Int = 0
    private var mDIA: Int = 0


    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        mBinding = CalibrationsFragment3Binding.inflate(inflater, container, false)
        mBusyCallback = parentFragment as BusyCallback
        return mBinding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        mBusyCallback.onInstruction("")
        mBinding.closeBtn.setOnClickListener {
            mBusyCallback.onClose("CalibrationFragment3")
        }

        userDetailsViewModel.manualInput.observe(viewLifecycleOwner) { manualInput ->
            manualInput?.let {
                if (it == 1 && !(multiVsViewModel.bpMac.isNullOrEmpty())) { // have manual and bp device
                    mBinding.manualBtn.visibility = View.VISIBLE
                    mBinding.measurementBtn.visibility = View.VISIBLE
                } else { // user does not have the blood pressure device
                    mBinding.manualBtn.visibility = View.VISIBLE
                }
            }
        }

        mBinding.manualBtn.setOnClickListener {
            Constants.bpSide = "Manual input"
            ManualBpFragment { sys, dia ->
                mSYS = sys
                mDIA = dia
                multiVsViewModel.mSys = mSYS
                multiVsViewModel.mDia = mDIA
                goToMeasurementFragment(false, sys = mSYS, dia = mDIA)
            }.show(childFragmentManager, ManualBpFragment.TAG)
        }

        mBinding.measurementBtn.setOnClickListener {
            mBinding.manualBtn.visibility = View.GONE
            mBinding.measurementBtn.visibility = View.GONE
            mBinding.bloodPressureIv.visibility = View.VISIBLE
            mBinding.leftArmBtn.visibility = View.VISIBLE
            mBinding.rightArmBtn.visibility = View.VISIBLE
        }

        mBinding.leftArmBtn.setOnClickListener {
            multiVsViewModel.bpSide = "Left arm"
            Constants.bpSide = "Left arm"
            goToMeasurementFragment(true, "left")
        }

        mBinding.rightArmBtn.setOnClickListener {
            multiVsViewModel.bpSide = "Right arm"
            Constants.bpSide = "Right arm"
            goToMeasurementFragment(true, "right")
        }

//        mBinding.continueBtn.setOnClickListener {
////            goToMeasurementFragment(false)
//            //TODO transact to measurement fragment
//        }
    }

    private fun goToMeasurementFragment(
        useBpDevice: Boolean = false,
        side: String = "",
        sys: Int = -1,
        dia: Int = -1
    ) {
        parentFragmentManager.commit {
            setCustomAnimations(R.anim.fade_in, R.anim.fade_out, R.anim.fade_in, R.anim.fade_out)
            replace(
                R.id.calibration_container,
                CalibrationFragment4::class.java,
                bundleOf(USE_BP_DEVICE to useBpDevice, SYS to sys, DIA to dia),
                CalibrationFragment4.TAG
            )
        }
    }

    override fun onPause() {
        super.onPause()
        mBinding.instructions.text = ""
        mBusyCallback.onInstruction("")
    }
}