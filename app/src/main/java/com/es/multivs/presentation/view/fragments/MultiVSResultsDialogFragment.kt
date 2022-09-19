package com.es.multivs.presentation.view.fragments

import android.app.Dialog
import android.graphics.Point
import android.os.Bundle
import android.view.Gravity
import android.view.View
import android.view.WindowManager
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.DialogFragment
import com.es.multivs.R
import dagger.hilt.android.AndroidEntryPoint
import java.util.*

/**
 * Created by Marko on 10/21/2021.
 * Etrog Systems LTD.
 */
@AndroidEntryPoint
class MultiVSResultsDialogFragment : DialogFragment() {

    companion object {
        fun newInstance(results: Bundle): MultiVSResultsDialogFragment {
            val fragment = MultiVSResultsDialogFragment()
            fragment.arguments = results
            return fragment
        }
    }

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val builder = AlertDialog.Builder(requireContext())
        val inflater = requireActivity().layoutInflater
        builder.setView(inflater.inflate(R.layout.multivs_results_layout, null))
        val dialog: AlertDialog = builder.setPositiveButton("OK") { _, _ ->
            dismiss()
        }.create()

        return dialog
    }

    override fun onResume() {
        val window = dialog!!.window
        val size = Point()
        val display = window!!.windowManager.defaultDisplay
        display.getSize(size)
        window.setLayout((size.x * 0.85).toInt(), WindowManager.LayoutParams.WRAP_CONTENT)
        window.setGravity(Gravity.CENTER)

        super.onResume()
    }

    override fun onStart() {
        super.onStart()
        val d = dialog as AlertDialog
        d.window!!.setBackgroundDrawableResource(R.drawable.bg_results_cornered)
        init(d)

        val negativeButton = d.getButton(AlertDialog.BUTTON_NEGATIVE)
        negativeButton.isEnabled = false
        negativeButton.visibility = View.GONE
    }

    private fun init(dialog: AlertDialog) {
        val heartRateTv = dialog.findViewById<TextView>(R.id.multi_vs_results_hr_tv)
        val temperatureTv = dialog.findViewById<TextView>(R.id.multi_vs_results_temp_tv)
        val respiratoryTv = dialog.findViewById<TextView>(R.id.multi_vs_results_resp_tv)
        val bloodPressureTv = dialog.findViewById<TextView>(R.id.multi_vs_results_bp_tv)
        val stepsTv = dialog.findViewById<TextView>(R.id.multi_vs_results_step_tv)
        val ecgTv = dialog.findViewById<TextView>(R.id.multi_vs_results_ecg_tv)

        val heartRate = requireArguments().getInt("heart_rate_result")
        val temperature = requireArguments().getString("temperature_result")
        val respiratory = requireArguments().getInt("respiratory_result")
        val steps = requireArguments().getInt("steps_result")
        val isECGOn = requireArguments().getBoolean("is_ecg_on")

        val diastolic = requireArguments().getString("diastolic_result")
        val systolic = requireArguments().getString("systolic_result")

        if (heartRate > 0) {
            heartRateTv!!.text = String.format(Locale.US, "%d", heartRate)
        }

        temperature?.let {
            if (it.toFloat() > 0){
                temperatureTv!!.text = String.format(Locale.US, "%s", it)
            }
        }

        if (respiratory > 0) {
            respiratoryTv!!.text = String.format(Locale.US, "%d", respiratory)
        }

        if (systolic!!.isNotEmpty() && diastolic!!.isNotEmpty() && "0.0" != systolic && "0.0" != diastolic) {
            bloodPressureTv!!.text =
                String.format(Locale.US, "%s / %s", systolic, diastolic)
        }

        if (steps > 0) {
            stepsTv!!.text = String.format(Locale.US, "%d", steps)
        }

        if (isECGOn) {
            ecgTv?.text = "On"
            ecgTv?.setTextColor(requireActivity().getColor(R.color.green_on))
        } else {
            ecgTv?.text = "Off"
            ecgTv?.setTextColor(requireActivity().getColor(R.color.red_off))
        }
    }
}