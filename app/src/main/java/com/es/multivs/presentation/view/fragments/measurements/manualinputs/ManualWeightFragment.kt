package com.es.multivs.presentation.view.fragments.measurements.manualinputs

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.fragment.app.DialogFragment
import com.es.multivs.databinding.WeightManualInputBinding
import dagger.hilt.android.AndroidEntryPoint
import com.es.multivs.data.utils.autoCleared

@AndroidEntryPoint
class ManualWeightFragment(private val callback: ManualWeightCallback) : DialogFragment() {

    var binding: WeightManualInputBinding by autoCleared()


    fun interface ManualWeightCallback {
        fun onInput(weight: Double)
    }

    companion object {
        const val TAG = "ManualWeightDialog"
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = WeightManualInputBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        var integer = 150
        var decimal = 0

        binding.weightCancelTv.setOnClickListener {
            dismiss()
        }

        binding.weightOkTv.setOnClickListener {
            callback.onInput((integer + (decimal.toDouble() / 10)))
            dismiss()
        }

        binding.weightIntegerNumbersNumberPicker.apply {
            maxValue = 400
            minValue = 0
            value = 150
            setOnValueChangedListener { numberPicker, i, newVal ->
                integer = newVal
            }
        }

        binding.weightDecimalNumberPicker.apply {
            maxValue = 9
            minValue = 0
            value = 0
            setOnValueChangedListener { numberPicker, i, newVal ->
                decimal = newVal
            }
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