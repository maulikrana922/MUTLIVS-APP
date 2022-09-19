package com.es.multivs.presentation.view.fragments

import android.app.Activity
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothManager
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.os.CountDownTimer
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.activity.result.contract.ActivityResultContracts.StartActivityForResult
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.bumptech.glide.Glide
import com.bumptech.glide.load.resource.drawable.DrawableTransitionOptions
import com.es.multivs.R
import com.es.multivs.data.models.ScheduleItem
import com.es.multivs.data.utils.*
import com.es.multivs.data.work.WeatherForecast
import com.es.multivs.data.work.WeatherWorker
import com.es.multivs.databinding.HealthhubFragmentLayoutBinding
import com.es.multivs.presentation.adapters.UserScheduleAdapter
import com.es.multivs.presentation.view.ScheduleItemSheet
import com.es.multivs.presentation.view.UserDevicesViewModel
import com.es.multivs.presentation.view.viewmodels.ScheduleViewModel
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.util.concurrent.TimeUnit
import javax.inject.Inject

/**
 * created by Marko
 * Etrog Systems LTD. 1/9/2021.
 */
@AndroidEntryPoint
class HealthHubFragment : Fragment(), UserScheduleAdapter.ScheduleAdapterListener {

    companion object {

        fun newInstance() = HealthHubFragment()

        const val SCHEDULE_UPDATE_INTERVAL = 300000L
    }

    @Inject
    lateinit var updates: ScheduleUpdatesManager

    private lateinit var binding: HealthhubFragmentLayoutBinding
    private var _adapter: UserScheduleAdapter? = null
    private val scheduleViewModel: ScheduleViewModel by activityViewModels()
    private val userDevicesViewModel: UserDevicesViewModel by activityViewModels()
    private var scheduleItems = mutableListOf<ScheduleItem>()

    private var loadingAlert: AlertDialog? = null
    private var refreshAlert: AlertDialog? = null
    private val _handler: Handler = Handler(Looper.getMainLooper())
    private var updateOnResume: Boolean = false
    private var isResume = false

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {

        val mBluetoothManager =
            requireActivity().getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager
        val mBluetoothAdapter = mBluetoothManager.adapter

        if (mBluetoothAdapter == null || !mBluetoothAdapter.isEnabled) {

            val enableBtIntent = Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE)

            val btPermissionResultLauncher = registerForActivityResult(
                StartActivityForResult()
            ) { result ->
                if (result.resultCode == Activity.RESULT_OK) {
                    Log.d("alih", "activated bluetooth")
                }
            }
            btPermissionResultLauncher.launch(enableBtIntent)
        }

        binding = HealthhubFragmentLayoutBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        scheduleViewModel.doingSomething.observe(viewLifecycleOwner) {
            it?.let {
                if (it) {
                    showLoadingAlert()
                } else {
                    hideLoadingAlert()
                }
            }
        }

        initRecycler()
        binding.materialCardView.setOnClickListener {
            lifecycleScope.launch {
                binding.healthHubLayout.transitionToStart()
                delay(500)
                updateWeather()
            }
        }

        lifecycleScope.launch {
            getSchedules(false)
            userDevicesViewModel.loadDevices()
        }

        updateWeather()
    }

    private suspend fun getSchedules(ignoreDatabase: Boolean) {

        val isInternetAvailable = AppUtils.isInternetAvailable(requireContext())
        var list: MutableList<ScheduleItem> = mutableListOf()

        if (isInternetAvailable) {
            list = scheduleViewModel.fetchAllSchedules(ignoreDatabase)
        } else {
            AppUtils.makeToast(requireContext(), "No internet connection!")
        }

        if (list.isNotEmpty()) {
            binding.noSchedulesTv.visibility = View.INVISIBLE

            val itemList = resolveActiveSchedules()
            _adapter?.addSchedules(itemList)

            _adapter?.let {
                val activeMeasurementIndex = it.notifyActiveSchedules()
                delay(1000) // give some delay to smooth out operations
                binding.scheduleRecycler.smoothScrollToPosition(activeMeasurementIndex)
            }
        } else {
            binding.noSchedulesTv.visibility = View.VISIBLE
        }
    }


    /**
     * periodically update the recyclerview with the current active schedules
     */
    private fun updateSchedules() {

        AppUtils.makeToast(requireContext(), getString(R.string.updating_schedule))
        lifecycleScope.launch {

            updateTasksCompletion()

            if (AppUtils.isInternetAvailable(requireContext()) && updates.fromServer.require) {
                getSchedules(updates.fromServer.ignoreDatabase)
            } else {
                val itemList = resolveActiveSchedules()
                if (itemList.isNotEmpty()) {
                    _adapter?.addSchedules(itemList)
                }
                _adapter?.let {
                    val activeMeasurementIndex = it.notifyActiveSchedules()
                    binding.scheduleRecycler.smoothScrollToPosition(activeMeasurementIndex)
                }
            }

            updates.clear()
        }
    }

    private suspend fun resolveActiveSchedules(): List<ScheduleItem> {
        val list = mutableListOf<ScheduleItem>()

        val latestMeasurementList = scheduleViewModel.getDBMeasurements()

        if (latestMeasurementList.isNotEmpty()) {
            val activeMeasurement: String =
                ScheduleUtils.findActiveMeasurement(latestMeasurementList)
            scheduleViewModel.insertActiveMeasurement(activeMeasurement)
            list.addAll(latestMeasurementList)
        }

        val latestMedicationList = scheduleViewModel.getDBMedications()
        if (latestMedicationList.isNotEmpty()) {
            ScheduleUtils.findActiveMedication(latestMedicationList)
            list.addAll(latestMedicationList)
        }

        val latestCalibrationList = scheduleViewModel.getDBCalibrations()
        if (latestCalibrationList.isNotEmpty()) {
            ScheduleUtils.findActiveCalibration(latestCalibrationList)
            list.addAll(latestCalibrationList)
        }

        val latestSurveyList = scheduleViewModel.getDBSurveysByDay()
        if (latestSurveyList.isNotEmpty()) {
            ScheduleUtils.findActiveSurvey(latestSurveyList)
            list.addAll(latestSurveyList)
        }

        return list
    }

    private suspend fun updateTasksCompletion() {
        if (updates.calibration.complete) {
            scheduleViewModel.updateCalibrationTasksCompletion()
        }
        if (updates.measurements.complete) {
            scheduleViewModel.updateMeasurementTasksCompletion(updates.measurements.timestamp)
        }
        if (updates.medication.complete) {
            scheduleViewModel.updateMedicationTasksCompletion(updates.medication.timestamp)
        }
        if (updates.survey.complete) {
            scheduleViewModel.updateSurveyTaskCompletion(updates.survey.timestamp)
        }
    }

    private fun hideLoadingAlert() {
        loadingAlert?.apply {
            if (isShowing) {
                dismiss()
            }
        }
        refreshAlert
    }

    private fun showLoadingAlert() {

        loadingAlert = AlertDialog.Builder(requireContext()).create()
        loadingAlert?.apply {
            val layoutInflater = LayoutInflater.from(requireActivity())
            val promptView: View =
                layoutInflater.inflate(R.layout.loading_layout, binding.root, false)
            setView(promptView)
            setCancelable(false)
        }
    }

    private fun showBluetoothRefreshAlert() {
        isResume = true
        refreshAlert = AlertDialog.Builder(requireActivity()).create()
        if (!refreshAlert!!.isShowing) {
            refreshAlert?.apply {
                val layoutInflater = LayoutInflater.from(requireActivity())
                val promptView: View =
                    layoutInflater.inflate(R.layout.refresh_layout, binding.root, false)
                setView(promptView)
                setCancelable(true)
                show()
            }
        }
    }

    private fun hideBluetoothRefreshAlert() {
        refreshAlert?.apply {
            if (isShowing) {
                dismiss()
                isResume = false
            }
        }
    }

    private fun initRecycler() {
        _adapter = UserScheduleAdapter(requireActivity(), scheduleItems)
//        _adapter?.clearSchedules()
        _adapter?.setListener(this)

        binding.scheduleRecycler.apply {
            adapter = _adapter
            layoutManager =
                LinearLayoutManager(requireActivity(), LinearLayoutManager.HORIZONTAL, false)
        }

        binding.careCoordinatorLinearLayout.setOnClickListener {
            AppUtils.showVideoAlertDialog(parentFragmentManager)
        }
    }

    private fun initializeBlueTooth() {
        val mBluetoothManager =
            requireActivity().getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager
        val mBluetoothAdapter = mBluetoothManager.adapter
        if (!isResume)
            showBluetoothRefreshAlert()

        lifecycleScope.launch {
            mBluetoothAdapter.disable()
            _handler.postDelayed(object : Runnable {
                override fun run() {
                    Log.d("_BLUETOOTH", "is enabled: ${mBluetoothAdapter.isEnabled}")
                    if (!mBluetoothAdapter.isEnabled) {
                        mBluetoothAdapter.enable()
                        hideBluetoothRefreshAlert()
                    } else {
                        _handler.postDelayed(this, 2500)
                    }
                }
            }, 2500)
        }
    }

    //TODO: create multiple schedule sheet types
    override fun onCardClicked(item: ScheduleItem) {
        val scheduleSheet: ScheduleItemSheet = ScheduleItemSheet.newInstance(item)
        scheduleSheet.isCancelable = true
        scheduleSheet.show(childFragmentManager, "ScheduleItemSheet")
    }

    override fun onResume() {
        super.onResume()
        initializeBlueTooth()
        if (!AppUtils.isLocationEnabled(requireContext())) {
            AppUtils.showLocationAlertFragment(childFragmentManager)
        }
        updateOnResume = true
        if (updateOnResume) {
            updatesRunnable.run()
        }
        binding.layoutCalibration.visibility=View.GONE
        scheduleViewModel.isCalibrationRequired.observe(requireActivity()) {
             Log.e("isCalibration",it.toString())
            if (it) startCalibrationTime() else binding.layoutCalibration.visibility=View.GONE
        }

    }


    private val updatesRunnable = object : Runnable {
        override fun run() {
            Log.d("_SCANNING", "running schedules updates")

            if (ScheduleUtils.isMidnightUpdateWindow()) {
                updates.requireServer(ignoreDatabase = true)
            }

            updateSchedules()
            updateWeather()


            _handler.postDelayed(this, SCHEDULE_UPDATE_INTERVAL)
        }
    }

    fun updateWeather() {
        WeatherForecast().fetchWeather(requireContext(), viewLifecycleOwner, {
            if (it != null) {
                val location = "${it.city}, ${it.country}"
                binding.weatherLocation.text = location

                val temperature = "${it.temperature}°"
                lifecycleScope.launch {

                }
                binding.weatherTemperature.text = temperature
                val iconURL = it.icon

                binding.weatherDescription.text = it.description

                Glide.with(requireContext())
                    .load("https://openweathermap.org/img/wn/$iconURL@2x.png")
                    .transition(DrawableTransitionOptions.withCrossFade())
                    .error(R.drawable.ic_error).into(binding.weatherIcon)

                binding.healthHubLayout.transitionToEnd()
            } else {
                binding.weatherDescription.text = WeatherWorker.WEATHER_ERROR
            }
        })
    }

    override fun onPause() {
        super.onPause()
        _handler.removeCallbacks(updatesRunnable)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _adapter = null
    }

    private fun startCalibrationTime() {
        binding.layoutCalibration.visibility = View.VISIBLE
        binding.tvCalibration.text =
            "Done ${SharedPrefs.getIntParam(Constants.CalibrationSet, 0)}/3 set "
        binding.tvMeasurement.text =
            "Done ${SharedPrefs.getIntParam(Constants.Measurement, 0)}/3 measurement"

        val millis = TimeUnit.SECONDS.toMillis(15)
        val countDownTimer = object : CountDownTimer(millis, 1000) {
            override fun onTick(millis: Long) {
            }

            override fun onFinish() {
                binding.layoutCalibration.visibility = View.INVISIBLE
            }
        }
        countDownTimer.start()
    }

}