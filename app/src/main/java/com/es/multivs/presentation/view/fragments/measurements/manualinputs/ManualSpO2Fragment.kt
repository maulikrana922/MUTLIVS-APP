package com.es.multivs.presentation.view.fragments.measurements.manualinputs

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.fragment.app.DialogFragment
import com.es.multivs.databinding.Spo2ManualInputBinding
import dagger.hilt.android.AndroidEntryPoint
import com.es.multivs.data.utils.autoCleared

@AndroidEntryPoint
class ManualSpO2Fragment(private val callback: ManualSpO2Callback) : DialogFragment() {

    var binding: Spo2ManualInputBinding by autoCleared()
    private var spo2 = 98

//    private var callback: ManualTemperatureCallback? = null

    fun interface ManualSpO2Callback {
        fun onInput(spo2: Int)
    }

    companion object {
        const val TAG = "ManualSpO2Dialog"
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = Spo2ManualInputBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.spo2VitalNp.apply {
            maxValue = 99
            minValue = 60
            value = 98
            setOnValueChangedListener { numberPicker, i, newVal ->
                spo2 = newVal
            }
        }

        binding.oximeterCancelTv.setOnClickListener {
            dismiss()
        }

        binding.oximeterOkTv.setOnClickListener {
            callback.onInput(spo2)
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