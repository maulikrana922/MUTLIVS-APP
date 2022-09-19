package com.es.multivs.presentation.view.fragments.measurements.manualinputs

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.fragment.app.DialogFragment
import com.es.multivs.databinding.BloodPressureManualInputBinding
import dagger.hilt.android.AndroidEntryPoint
import com.es.multivs.data.utils.autoCleared

@AndroidEntryPoint
class ManualBpFragment(private val callback: ManualBpCallback) : DialogFragment() {

    var binding: BloodPressureManualInputBinding by autoCleared()
    private var dia = 80
    private var sys = 120
//    private var callback: ManualTemperatureCallback? = null

    fun interface ManualBpCallback {
        fun onInput(sys:Int, dia:Int)
    }

    companion object {
        const val TAG = "ManualBpDialog"
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = BloodPressureManualInputBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.sysNp.apply {
            maxValue = 200
            minValue = 50
            value = 120
            setOnValueChangedListener { numberPicker, i, newVal ->
                sys = newVal
            }
        }

        binding.diaNp.apply {
            maxValue = 160
            minValue = 20
            value = 80
            setOnValueChangedListener { numberPicker, i, newVal ->
                dia = newVal
            }
        }

        binding.bloodPressureCancelNumberPicker.setOnClickListener {
            dismiss()
        }

        binding.bloodPressureNumberPickerOkTvBtn.setOnClickListener {
            callback.onInput(sys, dia)
            dismiss()
        }
    }

    override fun onResume() {
        super.onResume()
        val params: ViewGroup.LayoutParams = dialog!!.window!!.attributes
        params.width = ConstraintLayout.LayoutParams.MATCH_PARENT
        params.height = ConstraintLayout.LayoutParams.MATCH_PARENT
        dialog!!.window!!.attributes = params as WindowManager.LayoutParams
    }
}