package com.es.multivs.presentation.view.fragments

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.DialogFragment
import com.es.multivs.R
import dagger.hilt.android.AndroidEntryPoint
import com.es.multivs.data.utils.autoCleared
import com.es.multivs.databinding.AlertSurveyFragmentBinding


/**
 * created by Marko
 * Etrog Systems LTD. 1/9/2021.
 */
@AndroidEntryPoint
class AlertSurveyFragment : DialogFragment() {

    fun interface AlertSurveyDialogListener {
        fun onClick(string: String)
    }

    private var binding: AlertSurveyFragmentBinding by autoCleared()

    private var listener: AlertSurveyDialogListener? = null

    companion object {

        fun newInstance(): AlertSurveyFragment {
            return AlertSurveyFragment()
        }
    }

    fun listen(callback: AlertSurveyDialogListener) {
        listener = callback
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = AlertSurveyFragmentBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.yesBtn.setOnClickListener {
            listener?.onClick(getString(R.string.yes))
            dismiss()
        }

        binding.noBtn.setOnClickListener {
            listener?.onClick(getString(R.string.no))
            dismiss()
        }
    }
}