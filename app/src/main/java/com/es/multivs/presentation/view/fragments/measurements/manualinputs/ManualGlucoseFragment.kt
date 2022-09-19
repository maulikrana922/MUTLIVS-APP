package com.es.multivs.presentation.view.fragments.measurements.manualinputs

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.fragment.app.DialogFragment
import com.es.multivs.databinding.GlucoseManualInputBinding
import dagger.hilt.android.AndroidEntryPoint
import com.es.multivs.data.utils.autoCleared

@AndroidEntryPoint
class ManualGlucoseFragment(private val callback: ManualGlucoseCallback) : DialogFragment() {

    var binding: GlucoseManualInputBinding by autoCleared()
    private var glucose = 100


    fun interface ManualGlucoseCallback {
        fun onInput(glucose: Int)
    }

    companion object {
        const val TAG = "ManualGlucoseDialog"
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = GlucoseManualInputBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.glucoseNp.apply {
            maxValue = 800
            minValue = 10
            value = 100
            setOnValueChangedListener { numberPicker, i, newVal ->
                glucose = newVal
            }
        }

        binding.glucoseCancelBtnTv.setOnClickListener {
            dismiss()
        }

        binding.glucoseOkTvBtn.setOnClickListener {
            callback.onInput(glucose)
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