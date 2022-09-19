package com.es.multivs.presentation.view.fragments


import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.TextView
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.RecyclerView
import com.es.multivs.R
import com.es.multivs.data.network.netmodels.ReportsMeasurements
import com.es.multivs.data.network.netmodels.ReportsMedication
import com.es.multivs.data.utils.AppUtils
import com.es.multivs.data.utils.CustomProgressDialog
import com.es.multivs.data.utils.autoCleared
import com.es.multivs.databinding.ReportsFragmentLayoutBinding
import com.es.multivs.presentation.adapters.ReportsMeasurementsAdapter
import com.es.multivs.presentation.adapters.ReportsMedicationsAdapter
import com.es.multivs.presentation.view.viewmodels.ReportsViewModel
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch

/**
 * Created by Marko on 11/29/2021.
 * Etrog Systems LTD.
 */
@AndroidEntryPoint
class ReportsFragment : Fragment(), AdapterView.OnItemSelectedListener {

    companion object {
        fun newInstance() = ReportsFragment()
        const val TAG = "ReportsFragment"
    }

    private var measurementsList: MutableList<ReportsMeasurements.Message> = mutableListOf()
    private var medicationList: MutableList<ReportsMedication.Message.Medication> = mutableListOf()
    private var binding: ReportsFragmentLayoutBinding by autoCleared()
    private val reportsViewModel: ReportsViewModel by viewModels()
    private var isInternetAvailable = true
    private val hours = arrayOf("24 Hours", "7 Days", "14 Days")
    private var isSelectMedication = false
    private var isFirstOpen = false
    private val dialog = CustomProgressDialog()
    private var days = "24"
    private val reportsMeasurementsAdapter: ReportsMeasurementsAdapter by lazy {
        ReportsMeasurementsAdapter(reportsViewModel)
    }
    private val reportsMedicationsAdapter: ReportsMedicationsAdapter by lazy {
        ReportsMedicationsAdapter(reportsViewModel)
    }


    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {

        binding = ReportsFragmentLayoutBinding.inflate(inflater, container, false)

        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        isFirstOpen = true
        init()
    }

    fun init() {
        binding.spinnerHours.onItemSelectedListener = this
        val adapter =
            ArrayAdapter(requireActivity(), R.layout.support_simple_spinner_dropdown_item, hours)
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        binding.spinnerHours.adapter = adapter

        binding.btnMeasurements.setOnClickListener {
            getMeasurementData()
        }

        binding.btnMedications.setOnClickListener {
            getMedicationsData()
            isSelectMedication = true
            setVisibility(binding.rvMedication, binding.rvMeasurement, binding.clMeasurements)
            setMeasurementButtonPressedColors(binding.btnMeasurements, binding.btnMedications)
        }

        binding.ivRefresh.setOnClickListener {
            getData()
        }
    }

    override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
        Log.e("name", hours[position].substring(0, 2))
        days = hours[position].substring(0, 2).trim()
        if (!isFirstOpen) {
            getData()
        }
        isFirstOpen = false
    }

    override fun onNothingSelected(parent: AdapterView<*>?) {

    }

    private fun startProgress() {
        dialog.show(requireActivity().supportFragmentManager, "customProgressDialog")
        dialog.isCancelable = false
    }

    private fun checkMedication() {
        if (medicationList.isEmpty()) {
            binding.noReportsScene.noScheduleLayout.transitionToEnd()
            binding.noReportsScene.noSchedulesTv.text = getText(R.string.no_recent_medication_found)
            binding.clMedication.visibility = View.GONE
        } else {
            binding.noReportsScene.noScheduleLayout.transitionToStart()
            binding.clMedication.visibility = View.VISIBLE
            binding.rvMedication.visibility = View.VISIBLE
        }
    }

    private fun checkMeasurements() {
        if (measurementsList.isEmpty()) {
            binding.noReportsScene.noSchedulesTv.text =getText(R.string.no_recent_measurements_found)
            binding.noReportsScene.noScheduleLayout.transitionToEnd()
            binding.clMeasurements.visibility = View.GONE
        } else {
            binding.noReportsScene.noScheduleLayout.transitionToStart()
            binding.clMeasurements.visibility = View.VISIBLE
            binding.rvMeasurement.visibility = View.VISIBLE
        }
    }

    private fun getData() {
        Log.e("hour", days)
        if (isSelectMedication) {
            getMedicationsData()
        } else {
            getMeasurementData()
        }
    }

    private fun setMeasurementButtonPressedColors(whiteTv: TextView, blueTv: TextView) {
        whiteTv.setTextColor(ContextCompat.getColor(requireActivity(), R.color.black))
        whiteTv.background =
            ContextCompat.getDrawable(requireActivity(), R.drawable.bg_tab_gray_border)
        blueTv.setTextColor(ContextCompat.getColor(requireActivity(), R.color.white))
        blueTv.background =
            ContextCompat.getDrawable(requireActivity(), R.drawable.bg_submit_color_prime)
    }

    private fun setVisibility(
        rvMeasurement: RecyclerView,
        rvMedication: RecyclerView,
        clMedication: ConstraintLayout
    ) {
        rvMeasurement.visibility = View.VISIBLE
        rvMedication.visibility = View.GONE
        clMedication.visibility = View.GONE
    }

    private fun getMeasurementData() {
        isInternetAvailable = AppUtils.isInternetAvailable(requireActivity())
        if (isInternetAvailable) {
            startProgress()
            setVisibility(binding.rvMeasurement, binding.rvMedication, binding.clMedication)
            setMeasurementButtonPressedColors(binding.btnMedications, binding.btnMeasurements)
            isSelectMedication = false
            lifecycleScope.launch {
                measurementsList.clear()
                measurementsList = reportsViewModel.getMeasurements(days,requireActivity())
                binding.rvMeasurement.apply {
                    reportsMeasurementsAdapter.measurements = measurementsList
                    adapter = reportsMeasurementsAdapter
                }
                checkMeasurements()
                dialog.dismiss()
            }
        } else showInternetDialog()
    }

    private fun getMedicationsData() {
        isInternetAvailable = AppUtils.isInternetAvailable(requireActivity())
        if (isInternetAvailable) {
            startProgress()
            lifecycleScope.launch {
                medicationList.clear()
                medicationList = reportsViewModel.getMedications(days,requireActivity())
                binding.rvMedication.apply {
                    reportsMedicationsAdapter.medications = medicationList
                    adapter = reportsMedicationsAdapter
                }
                checkMedication()
                dialog.dismiss()
            }
        } else showInternetDialog()
    }

    override fun onResume() {
        super.onResume()
        getMeasurementData()
    }

    private fun showInternetDialog() {
        AppUtils.showAlertFragment(
            requireActivity().supportFragmentManager,
            requireActivity().getString(R.string.reports),
            requireActivity().getString(R.string.reports_internet_message)
        )
    }
}