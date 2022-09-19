package com.es.multivs.presentation.view.fragments

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.os.bundleOf
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.viewpager2.widget.ViewPager2
import com.es.multivs.R
import com.es.multivs.data.database.entities.MedicationData
import com.es.multivs.data.models.MedicationScheduleItem
import com.es.multivs.data.repository.MedicationUploadData
import com.es.multivs.data.utils.AppUtils
import com.es.multivs.data.utils.ScheduleUpdatesManager
import com.es.multivs.data.utils.autoCleared
import com.es.multivs.databinding.MedicationFragmentBinding
import com.es.multivs.presentation.adapters.MedicationsAdapter
import com.es.multivs.presentation.view.viewmodels.MedicationsViewModel
import com.google.android.material.tabs.TabLayout
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.*
import java.lang.StringBuilder
import javax.inject.Inject

/**
 * Created by Marko on 11/29/2021.
 * Etrog Systems LTD.
 */
@AndroidEntryPoint
class MedicationsFragment : Fragment() {

    private var binding: MedicationFragmentBinding by autoCleared()
    private val viewModel: MedicationsViewModel by viewModels()

    private lateinit var medsToUpload: MutableList<MedicationData>

    private var medicationItem: MedicationScheduleItem? = null

    private var tabLayout: TabLayout? = null

    private var medsAdapter: MedicationsAdapter? = null

    @Inject
    lateinit var scheduleUpdates: ScheduleUpdatesManager


    companion object {
        fun newInstance() = MedicationsFragment()
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = MedicationFragmentBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        //TODO livedata for medication uploads
        viewModel.medicationUpload.observe(viewLifecycleOwner) { uploadStatus ->
            if (uploadStatus.success) {
                AppUtils.makeSnackbar(
                    binding.saveBtn,
                    "The ${medicationItem?.timeStamp} medications were successfully uploaded"
                )
            } else {
                AppUtils.showAlertFragment(
                    parentFragmentManager,
                    getString(R.string.medications),
                    uploadStatus.message
                )
            }
            val viewPager2: ViewPager2 = requireActivity().findViewById(R.id.viewpager2)
            viewPager2.setCurrentItem(0, false)
            tabLayout = requireActivity().findViewById(R.id.tab_layout)
            tabLayout?.visibility = View.VISIBLE
        }

        medsToUpload = mutableListOf()

        initViews()
    }


    private fun initViews() {
        binding.saveBtn.setOnClickListener {
            medicationItem?.let { medItem ->


                // The difference list will contain all the medications that weren't taken.
                val difference = medItem.medicationList.filterNot { medsToUpload.contains(it) }

                if (difference.isNotEmpty()) {

                    // Make sure the user is aware that not all medications were taken.
                    createAlertMedicationDialog(difference)
                } else {
                    val medicationUploadDataList = arrayListOf<MedicationUploadData>()
                    medItem.medicationList.forEach { data ->
                        medicationUploadDataList.add(
                            MedicationUploadData(
                                data.medicationID.toString(),
                                data.medicationName,
                                data.strength,
                                data.dosageQuantity,
                                data.dosageForm,
                                "true"
                            )
                        )
                    }

                    viewModel.uploadTakenMedications(medicationUploadDataList, medicationItem)

//                    if (AppUtils.isInternetAvailable(requireContext())) {
//                        viewModel.uploadTakenMedications(medicationUploadDataList, medicationItem)
//                    } else {
//                        AppUtils.showAlertFragment(
//                            childFragmentManager,
//                            "No internet connection",
//                            "Cannot upload medications data"
//                        )
//                    }
                }
            }
        }
    }

    private fun fetchMedications() {
        lifecycleScope.launch {
            medicationItem = viewModel.getMedicationsFromDatabase()
            if (medicationItem != null) {
                medicationItem?.let {

                    binding.saveBtn.visibility = View.VISIBLE
                    binding.saveBtn.isClickable = true
                    binding.medicationMotionLayout.transitionToStart()
                    binding.medicationRecycler.visibility = View.VISIBLE
                    binding.tableLayout.visibility = View.VISIBLE

                    medsAdapter = MedicationsAdapter(it.medicationList) { medication, taken ->
                        if (taken) {
                            medsToUpload.add(medication)
                        } else {
                            medsToUpload.remove(medication)
                        }
                    }

                    binding.medicationRecycler.apply {
                        adapter = medsAdapter
                        layoutManager =
                            LinearLayoutManager(
                                requireContext(),
                                LinearLayoutManager.VERTICAL,
                                false
                            )
                    }

                }
            } else { // no active medications in database

                medsAdapter?.clearList()

                binding.saveBtn.visibility = View.GONE
                binding.saveBtn.isClickable = false
                binding.medicationMotionLayout.transitionToEnd()
                binding.medicationRecycler.visibility = View.GONE
                binding.tableLayout.visibility = View.GONE
            }
        }
    }

    override fun onResume() {
        super.onResume()
        fetchMedications()
    }

    private fun createAlertMedicationDialog(difference: List<MedicationData>) {
        val medsString = createMedicationDifferenceString(difference)
        val dialog = AlertMedsDialogFragment.newInstance().apply {
            arguments = bundleOf(
                Pair(AlertMedsDialogFragment.TITLE, "Medications"),
                Pair(AlertMedsDialogFragment.BODY, medsString)
            )
        }

        dialog.isCancelable = false
        dialog.show(childFragmentManager, "AlertMedsDialogFragment")
        dialog.listen { action ->
            when (action) {
                AlertMedsAction.YES -> {
                    // upload
                    val medicationUploadDataList = arrayListOf<MedicationUploadData>()

                    medsToUpload.forEach {
                        medicationUploadDataList.add(
                            MedicationUploadData(
                                it.medicationID.toString(),
                                it.medicationName,
                                it.strength,
                                it.dosageQuantity,
                                it.dosageForm,
                                "true"
                            )
                        )
                    }

                    medicationItem?.let {
                        it.medicationList.forEach { data ->
                            if (!medsToUpload.contains(data)) {
                                medicationUploadDataList.add(
                                    MedicationUploadData(
                                        data.medicationID.toString(),
                                        data.medicationName,
                                        data.strength,
                                        data.dosageQuantity,
                                        data.dosageForm,
                                        "false"
                                    )
                                )
                            }
                        }
                    }
                    viewModel.uploadTakenMedications(medicationUploadDataList, medicationItem)
                }
                AlertMedsAction.NO -> {
                    // dismiss, do nothing
                }
            }
        }
    }

    private fun createMedicationDifferenceString(difference: List<MedicationData>): String {
        val builder = StringBuilder()
        builder.append("Are you sure you want to skip these medications:")
        difference.forEach {
            builder.append("\n").append(it.medicationName)
        }
        return builder.toString()
    }

    override fun onPause() {
        super.onPause()
        AppUtils.dismissSnackbar()
    }
}