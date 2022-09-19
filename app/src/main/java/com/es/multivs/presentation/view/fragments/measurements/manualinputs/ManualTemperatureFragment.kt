package com.es.multivs.presentation.view.fragments.measurements.manualinputs

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.fragment.app.DialogFragment
import com.es.multivs.databinding.TemperatureManualInputLayoutBinding
import dagger.hilt.android.AndroidEntryPoint
import com.es.multivs.data.utils.autoCleared

@AndroidEntryPoint
class ManualTemperatureFragment(private val callback: ManualTemperatureCallback) : DialogFragment() {

    var binding: TemperatureManualInputLayoutBinding by autoCleared()
    private var integer = 97
    private var decimal = 0
//    private var callback: ManualTemperatureCallback? = null

    fun interface ManualTemperatureCallback {
        fun onInput(input: String)
    }

    companion object {
        const val TAG = "ManualTemperatureDialog"
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = TemperatureManualInputLayoutBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.temperatureIntegerNumbersNumberPicker.apply {
            maxValue = 110
            minValue = 0
            value = 97
            setOnValueChangedListener { numberPicker, i, new ->
                integer = new
            }
        }

        binding.temperatureDecimalNumberPicker.apply {
            maxValue = 9
            minValue = 0
            value = 0
            setOnValueChangedListener { numberPicker, i, new ->
                decimal = new
            }
        }

        binding.temperatureCancelTv.setOnClickListener {
            dismiss()
        }

        binding.temperatureOkTv.setOnClickListener {
            callback.onInput("$integer.$decimal")
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