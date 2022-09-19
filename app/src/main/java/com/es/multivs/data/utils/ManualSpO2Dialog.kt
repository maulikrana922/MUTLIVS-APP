package com.es.multivs.data.utils

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.widget.NumberPicker
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import com.es.multivs.R

class ManualSpO2Dialog(context: Context, private val callback: ManualSpO2Callback) {

    private var alertD: AlertDialog? = null

    fun interface ManualSpO2Callback {
        fun onInput(spo2: Int)
    }

    init {
        initializeManualInputHrDialog(context)
    }

    private fun initializeManualInputHrDialog(context: Context) {

        var spo2 = 98

        val layoutInflater = LayoutInflater.from(context)
        val promptView: View = layoutInflater.inflate(R.layout.spo2_manual_input, null)
        alertD = AlertDialog.Builder(context).create()


        val mCancelTV = promptView.findViewById<TextView>(R.id.oximeter_cancel_tv)
        mCancelTV.setOnClickListener {
            alertD?.dismiss()
        }

        val mOkTV = promptView.findViewById<TextView>(R.id.oximeter_ok_tv)
        mOkTV.setOnClickListener {
            callback.onInput(spo2)
            alertD?.dismiss()
        }

        val spo2NumberPicker = promptView.findViewById<NumberPicker>(R.id.spo2_vital_np)
        spo2NumberPicker.maxValue = 99
        spo2NumberPicker.minValue = 60
        spo2NumberPicker.value = 98
        spo2NumberPicker.setOnValueChangedListener { picker: NumberPicker?, oldVal: Int, newVal: Int ->
            spo2 = newVal

        }

        alertD?.setView(promptView)
    }
}