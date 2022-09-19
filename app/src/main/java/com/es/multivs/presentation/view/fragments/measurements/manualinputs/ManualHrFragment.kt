package com.es.multivs.presentation.view.fragments.measurements.manualinputs

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.fragment.app.DialogFragment
import com.es.multivs.databinding.HeartRateManualInputBinding
import dagger.hilt.android.AndroidEntryPoint
import com.es.multivs.data.utils.autoCleared

@AndroidEntryPoint
class ManualHrFragment(private val callback: ManualHrCallback) : DialogFragment() {

    var binding: HeartRateManualInputBinding by autoCleared()
    private var hr = 75

//    private var callback: ManualTemperatureCallback? = null

    fun interface ManualHrCallback {
        fun onInput(heartRate:Int)
    }

    companion object {
        const val TAG = "ManualHrDialog"
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = HeartRateManualInputBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.heartRateVitalNp.apply {
            maxValue = 200
            minValue = 30
            value = 75
            setOnValueChangedListener { numberPicker, i, newVal ->
                hr = newVal
            }
        }

        binding.heartRateCancelNumberPicker.setOnClickListener {
            dismiss()
        }

        binding.heartRateNumberPickerOkTvBtn.setOnClickListener {
            callback.onInput(hr)
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