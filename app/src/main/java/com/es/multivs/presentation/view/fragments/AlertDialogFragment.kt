package com.es.multivs.presentation.view.fragments

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.DialogFragment
import com.es.multivs.databinding.AlertDialogFragmentBinding
import dagger.hilt.android.AndroidEntryPoint
import com.es.multivs.data.utils.autoCleared


/**
 * created by Marko
 * Etrog Systems LTD. 1/9/2021.
 */
@AndroidEntryPoint
class AlertDialogFragment : DialogFragment() {

    private var binding: AlertDialogFragmentBinding by autoCleared()

    private var _withAction: Boolean = false
    private lateinit var _title: String

    companion object {

        const val TITLE = "alertTitle"
        const val BODY = "alertBody"

        fun newInstance(): AlertDialogFragment {
            return AlertDialogFragment()
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = AlertDialogFragmentBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val title: String = arguments?.get(TITLE) as String
        binding.alertTitle.text = title

        val body: String = arguments?.get(BODY) as String
        binding.alertBody.text = body

        binding.okBtn.setOnClickListener {
            dismiss()
        }
    }
}