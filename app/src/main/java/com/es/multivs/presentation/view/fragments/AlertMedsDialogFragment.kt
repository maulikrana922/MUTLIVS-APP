package com.es.multivs.presentation.view.fragments

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.DialogFragment
import com.es.multivs.data.utils.BluetoothAndLocationHandler
import dagger.hilt.android.AndroidEntryPoint
import com.es.multivs.data.utils.autoCleared
import com.es.multivs.databinding.AlertMedsDialogFragmentBinding


/**
 * created by Marko
 * Etrog Systems LTD. 1/9/2021.
 */
@AndroidEntryPoint
class AlertMedsDialogFragment : DialogFragment() {

    fun interface AlertMedsDialogFragmentListener {
        fun onAction(action: AlertMedsAction)
    }

    fun listen(callback: AlertMedsDialogFragmentListener){
        listener = callback
    }

    private lateinit var permissionRequests: BluetoothAndLocationHandler
    private var binding: AlertMedsDialogFragmentBinding by autoCleared()
    private var listener: AlertMedsDialogFragmentListener? = null

    companion object {

        const val TITLE = "alertTitle"
        const val BODY = "alertBody"

        fun newInstance(): AlertMedsDialogFragment {
            return AlertMedsDialogFragment()
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = AlertMedsDialogFragmentBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        permissionRequests = BluetoothAndLocationHandler(requireActivity().activityResultRegistry)

        val title: String = arguments?.get(TITLE) as String
        binding.alertTitle.text = title

        val body: String = arguments?.get(BODY) as String
        binding.alertBody.text = body

        binding.yesBtn.setOnClickListener {
            dismiss()
            listener?.onAction(AlertMedsAction.YES)
        }

        binding.noBtn.setOnClickListener {
            dismiss()
            listener?.onAction(AlertMedsAction.NO)
        }
    }
}