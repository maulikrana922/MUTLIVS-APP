package com.es.multivs.presentation.view.fragments

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.lifecycleScope
import com.es.multivs.R
import com.es.multivs.data.database.entities.NonMultiVSResults
import com.es.multivs.databinding.SummaryLayoutBinding
import com.es.multivs.presentation.view.viewmodels.MeasurementViewModel
import dagger.hilt.android.AndroidEntryPoint
import com.es.multivs.data.utils.autoCleared
import kotlinx.coroutines.launch

/**
 * Created by Marko on 11/29/2021.
 * Etrog Systems LTD.
 */
@AndroidEntryPoint
class SummaryFragment : Fragment() {

    private val measurementViewModel: MeasurementViewModel by activityViewModels()

    private var binding: SummaryLayoutBinding by autoCleared()
    private var listener: SummaryListener? = null
    private var measurementListener: MeasurementListener? = null
    private var mNonMultiVSResults: NonMultiVSResults? = null

    companion object {
        fun newInstance(): SummaryFragment = SummaryFragment()
        const val TAG = "SummaryFragment"
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = SummaryLayoutBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.summarySaveResultBtn.isEnabled = false
        initListeners()

        lifecycleScope.launch {
            val results = measurementViewModel.getNonMultiVSResults()
            mNonMultiVSResults = results
            showMeasurementResult(results)
        }

        measurementViewModel.uploadStatus.observe(viewLifecycleOwner, { uploadStatus ->
            binding.progressBar.visibility = View.INVISIBLE
            if (uploadStatus.success) {
                listener?.onExitFromSummary(true, getString(R.string.successully_uploaded))
            } else {
                listener?.onExitFromSummary(false, uploadStatus.message)
            }
        })
    }

    private fun initListeners() {
        binding.summaryCancelBtn.setOnClickListener {
            listener?.onExitFromSummary(false)
        }

        binding.summarySaveResultBtn.setOnClickListener {
            mNonMultiVSResults?.let {results->

                measurementViewModel.postResults(results)
                binding.progressBar.visibility = View.VISIBLE
//                if (AppUtils.isInternetAvailable(requireContext())) {
//                    measurementViewModel.postResults(it)
//                    binding.progressBar.visibility = View.VISIBLE
//                } else {
//                    //TODO create upload worker
//                    AppUtils.showAlertFragment(
//                        parentFragmentManager,
//                        getString(R.string.measurements),
//                        getString(R.string.no_internet_connection_upload_failed)
//                    )
//                }
            }

            if (mNonMultiVSResults == null) {
                Log.d("_SCANNING", "initListeners: RESULTS ARE NULL")
            }
        }
    }

    /**
     * If at least one measurement was taken, then show the save button
     */
    private fun showMeasurementResult(results: NonMultiVSResults) {

        /**
         * heart rate result
         */
        if (results.heartRate != 0) {
            binding.summaryHeartRateResult.text = results.heartRate.toString()
            binding.summarySaveResultBtn.isEnabled = true
            binding.summarySaveResultBtn.visibility = View.VISIBLE
        } else {
            binding.summaryHeartRateResult.text = "-"
        }

        /**
         * blood pressure result
         */
        if (results.bpCuffSys != 0 && results.bpCuffDia != 0) {
            binding.summaryBloodPressureRateResult.text =
                getString(R.string.blood_pressure_results, results.bpCuffSys, results.bpCuffDia)
            binding.summarySaveResultBtn.isEnabled = true
            binding.summarySaveResultBtn.visibility = View.VISIBLE
        } else {
            binding.summaryBloodPressureRateResult.text = "-"
        }

        /**
         * spO2 result
         */
        if (results.oximeterSpo2 != 0) {
            binding.summarySpo2RateResult.text =
                getString(R.string.spo2_results, results.oximeterSpo2)
            binding.summarySaveResultBtn.isEnabled = true
            binding.summarySaveResultBtn.visibility = View.VISIBLE
        } else {
            binding.summarySpo2RateResult.text = "-"
        }

        /**
         * temperature result
         */
        if (results.thermometerTemperature == null) {
            binding.summaryTemperatureRateResult.text = "-"
        } else {
            if (results.thermometerTemperature!!.isNotEmpty()) {
                binding.summaryTemperatureRateResult.text = results.thermometerTemperature
                binding.summarySaveResultBtn.isEnabled = true
                binding.summarySaveResultBtn.visibility = View.VISIBLE
            }
        }

        /**
         * weight result
         */
        if (results.weight == null) {
            binding.summaryWeightRateResult.text = "-"
        } else {
            if (results.weight!!.isNotEmpty()) {
                binding.summaryWeightRateResult.text = results.weight
                binding.summarySaveResultBtn.isEnabled = true
                binding.summarySaveResultBtn.visibility = View.VISIBLE
            }
        }

        /**
         * glucose result
         */
        if (results.glucoseLevel != 0) {
            binding.summaryGlucoseRateResult.text = results.glucoseLevel.toString()
            binding.summarySaveResultBtn.isEnabled = true
            binding.summarySaveResultBtn.visibility = View.VISIBLE
        } else {
            binding.summaryGlucoseRateResult.text = "-"
        }
    }

    override fun onResume() {
        super.onResume()

        if (listener == null) {
            listener = parentFragment as SummaryListener?
        }
        if (measurementListener == null) {
            measurementListener = parentFragment as MeasurementListener?
        }
    }

    override fun onPause() {
        super.onPause()
        measurementListener = null
        listener = null
    }
}