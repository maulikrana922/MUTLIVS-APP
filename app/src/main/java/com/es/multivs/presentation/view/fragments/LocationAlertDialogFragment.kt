package com.es.multivs.presentation.view.fragments

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.DialogFragment
import androidx.lifecycle.lifecycleScope
import com.es.multivs.data.utils.BluetoothAndLocationHandler
import com.es.multivs.databinding.AlertDialogFragmentBinding
import dagger.hilt.android.AndroidEntryPoint
import com.es.multivs.data.utils.autoCleared
import kotlinx.coroutines.launch


/**
 * created by Marko
 * Etrog Systems LTD. 1/9/2021.
 */
@AndroidEntryPoint
class LocationAlertDialogFragment : DialogFragment() {

    private lateinit var permissionRequests: BluetoothAndLocationHandler
    private var binding: AlertDialogFragmentBinding by autoCleared()

    companion object {

        const val TITLE = "alertTitle"
        const val BODY = "alertBody"

        fun newInstance(): LocationAlertDialogFragment {
            return LocationAlertDialogFragment()
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

        permissionRequests = BluetoothAndLocationHandler(requireActivity().activityResultRegistry)

        val title: String = arguments?.get(TITLE) as String
        binding.alertTitle.text = title

        val body: String = arguments?.get(BODY) as String
        binding.alertBody.text = body

        binding.okBtn.setOnClickListener {
            if (permissionRequests.isLocationEnabled(requireContext())){
                dismiss()
            }else{
                lifecycleScope.launch {
                    permissionRequests.requestLocationPermission()
                    permissionRequests.enableLocation(requireContext())
                    dismiss()
                }
            }
        }
    }
}