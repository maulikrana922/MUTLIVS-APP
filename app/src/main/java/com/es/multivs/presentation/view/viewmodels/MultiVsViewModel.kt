package com.es.multivs.presentation.view.viewmodels

import android.app.Application
import android.bluetooth.BluetoothDevice
import android.os.Handler
import android.os.Looper
import android.util.Log
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import androidx.work.*
import com.es.multivs.R
import com.es.multivs.data.bledevices.multivs.BodyPositionHandler
import com.es.multivs.data.bledevices.multivs.MultiVsListener
import com.es.multivs.data.bledevices.multivs.MultiVsManager
import com.es.multivs.data.bledevices.multivs.PatchData
import com.es.multivs.data.database.entities.CalibrationResults
import com.es.multivs.data.database.entities.UserLocation
import com.es.multivs.data.database.gateway.GatewayDbHelper
import com.es.multivs.data.database.location.LocationDbHelper
import com.es.multivs.data.database.multivs.MultiVsDbHelper
import com.es.multivs.data.database.sets.PatchVariables
import com.es.multivs.data.database.sets.TestType
import com.es.multivs.data.models.ServerResult
import com.es.multivs.data.network.Resource
import com.es.multivs.data.network.TokenKeeper
import com.es.multivs.data.network.netmodels.CalibrationResponse
import com.es.multivs.data.network.netmodels.SerializedPatchResults
import com.es.multivs.data.network.netmodels.UserLatestRead
import com.es.multivs.data.repository.MultiVsRepository
import com.es.multivs.data.repository.ResultPostException
import com.es.multivs.data.utils.*
import com.es.multivs.data.utils.AppUtils.Companion.getBatteryPercentage
import com.es.multivs.data.work.CalibrationUploadWorker
import com.google.gson.Gson
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.collect
import java.text.SimpleDateFormat
import java.util.*
import javax.inject.Inject
import kotlin.coroutines.CoroutineContext

/**
 * Created by Marko on 10/21/2021.
 * Etrog Systems LTD.
 */
@HiltViewModel
class MultiVsViewModel @Inject constructor(
    private val multiVsDbHelper: MultiVsDbHelper,
    private val gatewayDbHelper: GatewayDbHelper,
    private val locationDbHelper: LocationDbHelper,
    private val repo: MultiVsRepository,
    private var multiVsManager: MultiVsManager,
    application: Application
) : AndroidViewModel(application), CoroutineScope {

    companion object {
        private var VALUES_COUNT_30_SECONDS = 6000
        private var VALUES_COUNT_60_SECONDS = 12000
        private const val CONNECT_DURATION = 35000L
        private const val BAD_DATA_TIME_THRESHOLD = 7000
        private const val BAD_DATA_OCCURRENCE_THRESHOLD = 3
        private const val BAD_DATA_INTERVAL_THRESHOLD = 5

        const val TAG = "getSamplesByTestType"
    }

    private val _isConnected = MutableLiveData<Boolean>()
    var isConnected: LiveData<Boolean> = _isConnected
    private val _isWornCorrectly = MutableLiveData<Boolean>()
    var isWornCorrectly: LiveData<Boolean> = _isWornCorrectly
    private val ntcTemperature = MutableLiveData<Float>()
    fun getTemperature(): LiveData<Float> = ntcTemperature
    private val heartRate = MutableLiveData<Int>()
    private val steps = MutableLiveData<Int>()
    fun getSteps(): LiveData<Int> = steps
    private val userLatestRead = MutableLiveData<UserLatestRead>()
    fun getUserLatestRead(): LiveData<UserLatestRead> = userLatestRead
    private val uploadStatus = MutableLiveData<ServerResult>()
    fun isMeasurementFailed(): LiveData<ServerResult> = uploadStatus
    var isPosting: Boolean = false
    private val _calibrationResults = MutableLiveData<CalibrationPostResults?>()
    var calibrationResults: LiveData<CalibrationPostResults?> = _calibrationResults
    var calibrationResultLiveData = MutableLiveData<CalibrationResponse?>()
    //var _calibrationResultLiveData : LiveData<CalibrationResponse?> = calibrationResultLiveData

    var es008Mac: String? = null
    var bpMac: String? = null
    var mSys: Int = -1
    var mDia: Int = -1
    var bpSide: String = ""
    var userCountry: String? = null

    private var _handler = Handler(Looper.getMainLooper())
    private var bodyPositionInt = 0
    private var globalPatchData = PatchData()
    private val bodyPositionHandler = BodyPositionHandler(30000)
    private var results: CalibrationResults? = null
    private var patchVariables: PatchVariables = PatchVariables()

    private var startTime: Long = 0

    /**
     * Testing state of the multivs.
     * true when starting and false when stopping
     */
    var isTesting = false

    private var badDataCounter = 0
    private var firstBadDataTime = 0L

    var lastDevice: BluetoothDevice? = null

    //TODO: needs to be dynamic. get from server.
    private var checkForBadData = false

    var multivsActive = false

    var patchData = MutableLiveData<PatchData>()
    private val isBadData = MutableLiveData<Boolean>()

    fun clearHandler() {
        _handler.removeCallbacksAndMessages(null)
    }

    suspend fun getCalibrationDevicePositionAsync(): Deferred<String> = coroutineScope {
        val position = async {
            multiVsDbHelper.getCalibrationBodyPosition()
        }
        position
    }

    suspend fun getMeasurementDevicePositionAsync(): Deferred<String> = coroutineScope {
        val position = async {
            multiVsDbHelper.getMeasurementBodyPosition()
        }
        position
    }

    fun getTestTypeAsync() = async {
        multiVsDbHelper.getTestType()
    }

    suspend fun getUserId(): String {
        return gatewayDbHelper.getUsername()
    }

    suspend fun getAdvertisingId(): String {
        return gatewayDbHelper.getIdentifier()
    }

    fun setDeviceListener(): MultiVsListener {
        return object : MultiVsListener {
            override fun onPatchDataReceived(receivedPatchData: PatchData) {

                val currentTime = System.currentTimeMillis()
                /**
                 * Start collecting data 10 seconds after the MULTIVS device started the ECG/PPG operation.
                 */
                if (((currentTime / 1000L) - (startTime / 1000L)) > 10) {
                    checkForBadData = true
                    if (receivedPatchData.isEcg) {
                        globalPatchData.addAllToECGList(receivedPatchData.ecgSampleList)
                    }
                    if (receivedPatchData.isPpg) {
                        globalPatchData.addAllToPPGList(receivedPatchData.ppgSampleList)
                    }

                    patchData.postValue(receivedPatchData)
                }
            }

            /**
             * current algorithm: 7/12/2021
             *
             * badDataCounter = 0
             * newFirstBadDataTime = first bad data time in millis
             * currentBadDataTime = timestamp in millis of bad data ℕ ( ℕ ∈ {0,1,2..})
             * millisSinceNewFirstBadData = currentBadDataTime - newFirstBadDataTime
             *
             * if (badDataCounter > OccurrenceThreshold && millisSinceNewFirstBadData <= TimeThreshold)
             *     onStopTest()
             * else if (millisSinceNewFirstBadData > intervalThreshold)
             *     badDataCounter = 0
             *
             * if bad data counter reaches above the occurrence threshold
             * and remains under the time threshold, then stop the test.
             * if time of the current bad data since the latest first bad data occurrence is above the interval threshold,
             * then reset the counter and consequently the time of the first bad data.
             */
            override fun onBadData() {
                when (checkForBadData) {
                    true -> {
                        if (badDataCounter == 0) {
                            firstBadDataTime = System.currentTimeMillis()
                        }

                        val currentBadDataTime = System.currentTimeMillis()
                        val millisSinceNewFirstBadData = currentBadDataTime - firstBadDataTime

                        badDataCounter++

                        if (badDataCounter > BAD_DATA_OCCURRENCE_THRESHOLD && millisSinceNewFirstBadData <= BAD_DATA_TIME_THRESHOLD) {
                            stopTest(true)
                            isBadData.postValue(true)
                            badDataCounter = 0
                        } else if (millisSinceNewFirstBadData > BAD_DATA_INTERVAL_THRESHOLD) {
                            badDataCounter = 0
                        }
                    }
                    false -> {
                    }
                }

            }

            override fun onConnected(connected: Boolean) {
                clearHandler()
                if (!connected) {
                    if (multivsActive) {
                        multiVsManager.hardDisconnect()
                        _handler.postDelayed({
                            initBleDevice(lastDevice!!)
                        }, 1000)
                    }
                } else {
                    _isConnected.postValue(true)
                }
            }

            override fun onWearingStatus(wornCorrectly: Boolean) {
                _isWornCorrectly.postValue(wornCorrectly)
            }

            override fun onBodyPositionChanged(bodyPosition: Int) {
                bodyPositionInt = bodyPosition
                if (isTesting) {
                    bodyPositionHandler.add(bodyPosition)
                }
            }

            override fun onTemperatureReceived(value: Float) {
                globalPatchData.temperature = value
                ntcTemperature.postValue(value)
            }

            override fun onHeartRateReceived(hr: Int) {
                heartRate.postValue(hr)
            }

            override fun onStepsReceived(step: Int) {
                globalPatchData.steps = step
                steps.postValue(step)
            }

            override fun onBatteryLevelReceived(batteryLevel: Int) {
                globalPatchData.batteryLevel = batteryLevel
            }
        }
    }

    fun initBleDevice(device: BluetoothDevice) {
        this.lastDevice = device
        _handler.postDelayed({
            _isConnected.postValue(false)
        }, CONNECT_DURATION)
        val listener = setDeviceListener()

        multiVsManager.setAndConnect(device, listener)
    }

    fun startTest(isCalibration: Boolean) {
        if (isCalibration) {
            viewModelScope.launch(Dispatchers.Default) {
                startTime = System.currentTimeMillis()
                isTesting = true
                multiVsManager.startEcgPpg(true, true)
            }
        } else {
            viewModelScope.launch(Dispatchers.IO) {
                patchVariables = multiVsDbHelper.getVariables()
                val isECGChecked: Boolean = patchVariables.isECGChecked
                val isPPGChecked: Boolean = patchVariables.isPPGChecked
                startTime = System.currentTimeMillis()
                isTesting = true
                multiVsManager.startEcgPpg(isECGChecked, isPPGChecked)
            }
        }
    }

    fun stopTest(clearLists: Boolean) {

        isTesting = false
        checkForBadData = false
        multiVsManager.stopTest()

        if (clearLists) {
            globalPatchData.ecgSampleList.clear()
            globalPatchData.ppgSampleList.clear()
            globalPatchData = PatchData()
        }
    }

    private fun getSamplesByTestType(testType: String): String {
        Log.e("calibration:", Constants.CalibrationDuration)
        Log.e("measurement:", Constants.MeasurementDuration)
        if (Constants.MeasurementDuration == "60 sec") {
            VALUES_COUNT_30_SECONDS = 12000
            VALUES_COUNT_60_SECONDS = 24000
        } else {
            VALUES_COUNT_30_SECONDS = 6000
            VALUES_COUNT_60_SECONDS = 12000
        }

        var dataString = ""
        when (testType) {
            "ECG" -> try {
                dataString = globalPatchData.getECGDataString(VALUES_COUNT_30_SECONDS)
            } catch (e: IndexOutOfBoundsException) {
                Log.e(TAG, "updateResults: not enough samples" + e.message)
            }
            "PPG" -> try {
                dataString = globalPatchData.getPPGDataString(VALUES_COUNT_30_SECONDS)
            } catch (e: IndexOutOfBoundsException) {
                Log.e(TAG, "updateResults: not enough samples" + e.message)
            }
            "ECP" -> try {
                dataString =
                    globalPatchData.zipDataLists(VALUES_COUNT_60_SECONDS) // combine both lists e,p,e,p,e....e,p
            } catch (e: IndexOutOfBoundsException) {
                Log.e(TAG, "updateResults: not enough samples" + e.message)
            }
        }
        return dataString
    }

    private suspend fun determineTestType(isCalibration: Boolean): String {
        var testTypeStr = ""
        val testType: TestType = multiVsDbHelper.getTestType()
        if (testType.isECGChecked && testType.isPPGChecked) {
            testTypeStr = if (isCalibration) {
                "ECL"
            } else {
                "ECP"
            }
        } else if (testType.isECGChecked) {
            testTypeStr = "ECG"
        } else if (testType.isPPGChecked) {
            testTypeStr = "PPG"
        }
        return testTypeStr
    }

    fun closeDevice() {
        try {
            multiVsManager.disconnect()
            _handler.postDelayed({
                multiVsManager.hardDisconnect()
            }, 300)


            _isConnected.postValue(false)
        } catch (e: Exception) {
            println(e.message)
        }
    }

    fun postPatchData() {
        if (AppUtils.isInternetAvailable(getApplication())) {

            viewModelScope.launch {
                isPosting = true
                val results: SerializedPatchResults = configureMeasurementResults()
                if (results.data.equals("-1")) {
                    uploadStatus.postValue(ServerResult(false))
                } else {
                    bodyPositionHandler.resetMap()
                    try {
                        val baseURL = gatewayDbHelper.getBaseURL()
                        val userID = gatewayDbHelper.getUserID()
                        Log.e("battery", results.batteryLevel.toString())
                        Log.e("isoBattery", results.iosBatteryLevel.toString())
                        repo.postPatchResults(results, baseURL)
                        val resource = repo.getUserLatestRead(userID)

                        // clear lists after getting values and reset globalPatchData
                        globalPatchData.ecgSampleList.clear()
                        globalPatchData.ppgSampleList.clear()
                        globalPatchData = PatchData()

                        when (resource.status) {
                            Status.SUCCESS -> {
                                userLatestRead.postValue(resource.data!!)
                            }
                            Status.ERROR -> {
                                uploadStatus.postValue(ServerResult(false))
                            }
                            else -> uploadStatus.postValue(ServerResult(false))
                        }
                    } catch (e: ResultPostException) {
                        uploadStatus.postValue(ServerResult(false))
                    }
                }
            }
        } else {
            uploadStatus.postValue(
                ServerResult(
                    false,
                    "No Internet connection. Data won't be uploaded."
                )
            )
            //TODO: MultiVS worker
        }
        isPosting = false
    }

    private suspend fun configureMeasurementResults(): SerializedPatchResults {
        var dataString: String = ""

        // Initialize new results
        val resultsToUpload = SerializedPatchResults()

        val username: String = gatewayDbHelper.getUsername() //TODO: make dynamic
        resultsToUpload.username = username

        // Determine test type and append to file name
        val testType = determineTestType(false)

        val fileName =
            testType + username + "_" + SimpleDateFormat(
                "yyyy-MM-dd_HH-mm-ss",
                Locale.US
            ).format(Calendar.getInstance().time) + ".txt"

        resultsToUpload.filename = fileName

        // Get saved samples from globalPatchData
        dataString = getSamplesByTestType(testType)
        resultsToUpload.data = dataString

        // Get saved steps
        val steps: String = globalPatchData.steps.toString()
        if (steps == "0")
            resultsToUpload.stepValue = Constants.steps
        else {
            Constants.steps = steps
            resultsToUpload.stepValue = steps
        }

        Log.e("step", steps)

        // UUID is the same as username
        resultsToUpload.uuid = username

        // Get body position that was the most time during the test
        val position = bodyPositionHandler.getMaxTimeBodyPosition()
        bodyPositionHandler.resetMap()
        val bodyPosition: String = parseBodyPosition(bodyPositionInt)
        resultsToUpload.bodyPosition = bodyPosition

        // device postion from server
        val devicePosition = multiVsDbHelper.getMeasurementBodyPosition()
        resultsToUpload.devicePosition = devicePosition

        // temperature

        if (userCountry == getApplication<Application>().getString(R.string.usa)) {
            resultsToUpload.tempValue =
                MeasurementUtils.convertToFahrenheit(globalPatchData.temperature).toString()
        } else {
            resultsToUpload.tempValue = globalPatchData.temperature.toString()
        }

        // get sensor type
        val sensorType = "1" // ALWAYS 1 with ES008A
        resultsToUpload.sensorType = sensorType

        // get device mac address
        val macAddress: String = gatewayDbHelper.getIdentifier()
        resultsToUpload.macAddress = macAddress

        //TODO: get device battery status
        val iosBatteryLevel: String =
            AppUtils.getBatteryPercentage(getApplication()).toString()
        resultsToUpload.iosBatteryLevel = iosBatteryLevel

        // get sensor battery level
        val deviceBatteryLevel: String = globalPatchData.batteryLevel.toString()
        if (deviceBatteryLevel == "0")
            resultsToUpload.batteryLevel = Constants.batteryLevel
        else {
            Constants.batteryLevel = deviceBatteryLevel
            resultsToUpload.batteryLevel = deviceBatteryLevel
        }
        Log.e("constant", Constants.batteryLevel)

        // get the current time stamp
        val timestamp: String = (System.currentTimeMillis() / 1000).toString()
        resultsToUpload.timeStamp = timestamp

        val lastUserLocation: UserLocation = locationDbHelper.getLocation()
        val latitude: String = lastUserLocation.latitude.toString()
        resultsToUpload.latitude = latitude
        val longitude: String = lastUserLocation.longitude.toString()
        resultsToUpload.longitude = longitude

        return resultsToUpload
    }

    private fun parseBodyPosition(position: Int): String {
        val LYING_DOWN = "Lying down"
        val UPRIGHT = "Upright"
        val MOVING = "Moving"
        val LEFT_SIDE_LYING = "Left side lying"
        val RIGHT_SIDE_LYING = "Right side lying"
        val LYING_PROSTRATE = "Lying prostrate"
        val UPSIDE_DOWN = "Upside down"
        var positionToReturn = ""
        when (position) {
            1 -> positionToReturn = LYING_DOWN
            2 -> positionToReturn = UPRIGHT
            3 -> positionToReturn = MOVING
            4 -> positionToReturn = LEFT_SIDE_LYING
            5 -> positionToReturn = RIGHT_SIDE_LYING
            6 -> positionToReturn = LYING_PROSTRATE
            8 -> positionToReturn = UPSIDE_DOWN
            -1 -> positionToReturn = "Moving"
        }
        Log.e("body_position", positionToReturn)
        return positionToReturn
    }

    fun getPatchData(): LiveData<PatchData> = patchData

    fun getIsBadData(): LiveData<Boolean> = isBadData

    fun resetBadData() {
        isBadData.value = false
    }

    fun onResultsReceived() {
        _calibrationResults.value = null
    }

    fun onClearReasult() {
        calibrationResultLiveData.value = null
    }

    fun postCalibrationData() {
        if (AppUtils.isInternetAvailable(getApplication())) {
            val authHeader = "Bearer " + TokenKeeper.instance?.token
            //val url = "${"https://us.etrogsystems.com/etrogsystems/api/"}saveCalibrationRecord"
            ioJob {
                prepareResults()
                val gson = Gson()
                Log.e("token", authHeader)
                Log.e("request", gson.toJson(results))
                results?.bpCuffPlacement = Constants.bpSide
                Constants.bpSide = ""

                // Static data pass for testing
/*            results?.batterylevel="90"
            results?.bodyPosition="Upright"
            results?.devicePosition="Patch_Upper_Left"
            results?.bpCuffPlacement="80"
            results?.filename="ECL2821999525_2022-08-24_12-34-40.txt"
            results?.iosBatteryLevel="92"
            results?.lat="23.019034"
            results?.lng="72.5193013"
            results?.sensorType="1"
            results?.stepValue="25"
            results?.sys="120"
            results?.tempValue="95.74"
            results?.username="2821999525"
            results?.uuid="2821999525"*/


                repo.postCalibrationResult(results!!).collect {
                    val json = gson.toJson(it)
                    Log.e("response", json)
                    it.data?.let { it ->
                        SharedPrefs.setIntParam(
                            Constants.CalibrationSet,
                            AppUtils.calculateCalibrationSet(it.set)
                        )
                    }
                    it.data?.measurement?.let { it ->
                        SharedPrefs.setIntParam(
                            Constants.Measurement,
                            it
                        )
                    }

                    when (it.status) {
                        Resource.Status.SUCCESS -> {
                            when (it.data?.is_valid) {
                                0 -> {
                                    it.data.let { it ->
                                        SharedPrefs.setIntParam(
                                            Constants.CalibrationSet,
                                            AppUtils.calculateCalibrationSetForError(it.set)
                                        )
                                    }
                                    it.data.measurement.let { it ->
                                        SharedPrefs.setIntParam(
                                            Constants.Measurement,
                                            it
                                        )
                                    }
                                    _calibrationResults.postValue(
                                        CalibrationPostResults(
                                            Status.ERROR,
                                            getApplication<Application>().getString(R.string.the_last_calibration_was)
                                        )
                                    )
                                }
                                else -> {
//                                    _calibrationResults.postValue(
//                                    CalibrationPostResults(
//                                        Status.SUCCESS, getApplication<Application>().getString(R.string.successully_uploaded)
//                                    ))calibrationResultLiveData
                                    calibrationResultLiveData.postValue(it.data)
                                }
                            }

                        }
                        Resource.Status.ERROR -> {
                            _calibrationResults.postValue(
                                it.message?.let { it1 ->
                                    CalibrationPostResults(
                                        Status.ERROR,
                                        it1
                                    )
                                }
                            )
                        }
                    }
                }
                isPosting = false
                globalPatchData.ecgSampleList.clear()
                globalPatchData.ppgSampleList.clear()
                results = null // reset results
            }
        }else{
            _calibrationResults.postValue(
                CalibrationPostResults(
                    Status.ERROR,
                    "No Internet connection. Data won't be uploaded."
                )
            )
        }
    }

    fun postCalibration(requireActivity: FragmentActivity) {
        viewModelScope.launch(Dispatchers.IO) {
            isPosting = true
            _calibrationResults.postValue(
                CalibrationPostResults(
                    Status.LOADING,
                    getApplication<Application>().getString(R.string.processing_results)
                )
            )

            val isInvalidPPG = globalPatchData.ppgSampleList.all { it == 0 }

            if (isInvalidPPG) {
                _calibrationResults.postValue(
                    CalibrationPostResults(
                        Status.ERROR,
                        getApplication<Application>().getString(R.string.calibration_failed)
                    )
                )
                return@launch
            }

            prepareResults()

            if (AppUtils.isInternetAvailable(getApplication())) {

                _calibrationResults.postValue(
                    CalibrationPostResults(
                        Status.LOADING,
                        getApplication<Application>().getString(R.string.uploading_results)
                    )
                )
                try {
                    withTimeout(50000) {
                        results?.bpCuffPlacement = Constants.bpSide
                        Constants.bpSide = ""
                        Log.e("request", results?.bpCuffPlacement.toString())
                        val answer = repo.postCalibration(results!!)

                        if (answer == null) {
                            _calibrationResults.postValue(
                                CalibrationPostResults(
                                    Status.ERROR,
                                    getApplication<Application>().getString(R.string.calibration_failed)
                                )
                            )
                        } else if (answer.is_valid == 0) {
                            _calibrationResults.postValue(
                                CalibrationPostResults(
                                    Status.ERROR,
                                    getApplication<Application>().getString(R.string.the_last_calibration_was)
                                )
                            )
                        } else {
                            _calibrationResults.postValue(
                                CalibrationPostResults(
                                    Status.SUCCESS,
                                    getApplication<Application>().getString(R.string.successully_uploaded)
                                )
                            )
                        }
                    }
                } catch (e: Exception) {
//                    _calibrationResults.postValue(
//                        CalibrationPostResults(
//                            Status.ERROR,
//                            getApplication<Application>().getString(R.string.server_not_respond_recalibrate)
//                        )
//                    )

                    _calibrationResults.postValue(
                        CalibrationPostResults(
                            Status.SUCCESS,
                            getApplication<Application>().getString(R.string.successully_uploaded)
                        )
                    )
                } finally {
                    isPosting = false
                }

                globalPatchData.ecgSampleList.clear()
                globalPatchData.ppgSampleList.clear()
                results = null // reset results
            } else {
                _calibrationResults.postValue(
                    CalibrationPostResults(
                        Status.ERROR,
                        getApplication<Application>().getString(R.string.no_internet_connection_upload_failed)
                    )
                )

                globalPatchData.ecgSampleList.clear()
                globalPatchData.ppgSampleList.clear()
                results = null // reset results
                val constraints = Constraints.Builder()
                    .setRequiredNetworkType(NetworkType.CONNECTED)
                    .build()

                val dataToUpload = Gson().toJson(results)


                val uploadRequest = OneTimeWorkRequestBuilder<CalibrationUploadWorker>()
                    .setConstraints(constraints)
                    .setInputData(workDataOf("data" to dataToUpload))
                    .build()

                WorkManager.getInstance(getApplication()).enqueue(uploadRequest)
            }
            globalPatchData = PatchData()
        }

    }

    private suspend fun prepareResults() {
        results = CalibrationResults()
        globalPatchData.isEcg = true
        globalPatchData.isPpg = true

        results!!.sensorType = "1"

        // temperature
        if (userCountry == getApplication<Application>().getString(R.string.usa)) {
            results!!.tempValue =
                MeasurementUtils.convertToFahrenheit(globalPatchData.temperature).toString()
        } else {
            results!!.tempValue = globalPatchData.temperature.toString()
        }

        // steps
        val steps = globalPatchData.steps.toString()
        if (steps == "0")
            results!!.stepValue = Constants.steps
        else {
            Constants.steps = steps
            results!!.stepValue = steps
        }

        if (Constants.CalibrationDuration == "60 sec")
            VALUES_COUNT_60_SECONDS = 24000
        else VALUES_COUNT_60_SECONDS = 12000

        // MULTIVS battery
        //results!!.batterylevel = globalPatchData.batteryLevel.toString()
        if (globalPatchData.batteryLevel.toString() == "0")
            results!!.batterylevel = Constants.batteryLevel
        else {
            Constants.batteryLevel = globalPatchData.batteryLevel.toString()
            results!!.batterylevel = globalPatchData.batteryLevel.toString()
        }
        Log.e("constant", Constants.batteryLevel)
        // device battery
        results!!.iosBatteryLevel = getBatteryPercentage(getApplication()).toString()

        // blood pressure
        results!!.sys = mSys.toString()
        results!!.dia = mDia.toString()

        // MULTIVS data
        val dataString = globalPatchData.zipDataLists(VALUES_COUNT_60_SECONDS)
        results!!.data = dataString
        //results?.data="2063,3627,2061,3625,2059,3624,2056,3622,2053,3621,2053,3620,2054,3618,2054,3617,2056,3615,2058,3614,2058,3613,2057,3612,2055,3611,2055,3610,2056,3609,2055,3609,2055,3608,2055,3607,2056,3607,2056,3607,2056,3606,2057,3606,2056,3606,2054,3606,2053,3606,2053,3606,2053,3606,2055,3606,2057,3607,2056,3607,2055,3608,2054,3607,2052,3608,2050,3608,2049,3608,2047,3609,2044,3610,2043,3610,2042,3610,2039,3611,2039,3611,2038,3611,2036,3612,2036,3612,2036,3612,2034,3612,2034,3612,2033,3613,2029,3613,2029,3614,2028,3614,2027,3614,2029,3615,2031,3615,2031,3615,2030,3616,2030,3617,2027,3616,2026,3617,2024,3618,2022,3618,2020,3618,2021,3619,2017,3620,2017,3620,2016,3620,2015,3620,2013,3620,2013,3620,2009,3621,2006,3622,2000,3622,2001,3623,2010,3623,2034,3624,2077,3624,2134,3625,2184,3626,2214,3627,2213,3627,2181,3628,2128,3628,2073,3628,2031,3628,2009,3628,2001,3628,2002,3629,2005,3630,2007,3631,2010,3632,2011,3632,2011,3633,2009,3633,2008,3634,2007,3635,2006,3635,2005,3636,2005,3636,2005,3637,2006,3637,2007,3637,2009,3638,2010,3638,2010,3639,2009,3640,2010,3640,2010,3641,2014,3642,2018,3643,2024,3644,2028,3645,2031,3645,2033,3645,2037,3645,2042,3645,2046,3645,2051,3645,2056,3645,2058,3644,2058,3643,2058,3642,2056,3641,2053,3639,2049,3637,2044,3636,2036,3634,2026,3632,2014,3631,2003,3629,1992,3627,1980,3624,1970,3622,1960,3620,1952,3617,1945,3615,1941,3614,1939,3612,1937,3610,1936,3609,1936,3607,1935,3606,1933,3604,1935,3603,1934,3601,1933,3600,1932,3598,1931,3596,1929,3595,1930,3595,1929,3594,1928,3593,1930,3593,1930,3592,1930,3591,1930,3591,1932,3590,1931,3590,1932,3590,1931,3590,1930,3589,1930,3589,1931,3589,1931,3589,1932,3588,1933,3588,1933,3589,1930,3589,1930,3589,1929,3589,1927,3589,1926,3589,1925,3589,1924,3589,1922,3590,1918,3590,1917,3591,1916,3591,1914,3591,1915,3592,1917,3592,1917,3592,1916,3592,1915,3592,1914,3593,1915,3593,1914,3593,1916,3594,1914,3594,1913,3595,1909,3595,1908,3596,1905,3596,1906,3596,1903,3596,1902,3597,1902,3597,1901,3598,1899,3598,1897,3598,1894,3598,1891,3598,1896,3598,1912,3599,1943,3599,1993,3599,2050,3600,2095,3600,2115,3600,2103,3601,2062,3602,2007,3602,1958,3603,1924,3603,1908,3603,1906,3604,1909,3604,1912,3604,1916,3605,1917,3606,1918,3606,1920,3606,1922,3607,1923,3607,1924,3608,1925,3608,1926,3609,1927,3610,1928,3610,1929,3611,1933,3611,1933,3611,1934,3612,1937,3612,1940,3612,1941,3614,1946,3614,1951,3614,1956,3615,1962,3615,1969,3615,1976,3616,1982,3616,1988,3617,1995,3617,2001,3617,2007,3617,2011,3616,2014,3615,2015,3614,2017,3612,2016,3611,2013,3608,2009,3606,2003,3605,1994,3603,1985,3600,1976,3598,1965,3596,1956,3593,1947,3591,1938,3589,1932,3587,1928,3584,1924,3581,1922,3579,1919,3576,1916,3574,1913,3572,1912,3570,1911,3569,1911,3568,1911,3566,1911,3565,1912,3563,1912,3561,1915,3560,1916,3558,1916,3556,1916,3555,1915,3554,1916,3553,1918,3553,1920,3552,1923,3551,1923,3550,1923,3550,1923,3550,1923,3550,1923,3550,1923,3550,1925,3551,1924,3550,1923,3550,1924,3550,1922,3550,1919,3550,1920,3550,1919,3551,1919,3551,1919,3551,1919,3551,1918,3551,1917,3551,1915,3550,1915,3551,1915,3551,1916,3551,1917,3551,1920,3552,1921,3552,1921,3552,1921,3552,1921,3552,1921,3552,1918,3552,1917,3552,1916,3553,1913,3553,1910,3554,1911,3555,1911,3555,1909,3556,1908,3556,1908,3556,1908,3557,1907,3557,1904,3557,1902,3558,1900,3558,1903,3558,1917,3558,1951,3559,2006,3560,2069,3560,2120,3561,2146,3561,2135,3562,2093,3562,2036,3562,1982,3562,1945,3563,1928,3563,1922,3564,1922,3564,1923,3565,1925,3565,1925,3565,1926,3565,1927,3566,1930,3566,1932,3567,1935,3568,1938,3568,1940,3569,1942,3570,1942,3570,1941,3570,1941,3571,1942,3571,1943,3571,1945,3572,1950,3573,1953,3573,1954,3574,1956,3575,1959,3576,1963,3576,1970,3576,1975,3577,1985,3577,1994,3577,2000,3576,2006,3576,2011,3575,2015,3573,2013,3573,2015,3571,2014,3569,2013,3568,2009,3565,2007,3563,2001,3561,1995,3559,1987,3556,1979,3553,1968,3551,1958,3548,1948,3545,1938,3543,1930,3540,1923,3538,1918,3535,1916,3533,1915,3531,1914,3529,1914,3527,1913,3525,1912,3523,1914,3521,1914,3519,1915,3518,1917,3517,1918,3515,1917,3513,1917,3512,1917,3511,1918,3510,1916,3510,1917,3509,1920,3509,1922,3507,1922,3507,1925,3506,1924,3505,1924,3505,1924,3505,1925,3505,1925,3505,1923,3506,1923,3507,1920,3507,1920,3508,1918,3508,1919,3508,1919,3509,1918,3510,1916,3509,1916,3509,1916,3510,1916,3510,1917,3510,1917,3511,1916,3512,1914,3513,1912,3513,1912,3513,1914,3514,1916,3514,1917,3515,1920,3515,1923,3515,1922,3515,1922,3516,1921,3516,1919,3517,1917,3518,1916,3519,1917,3519,1918,3519,1918,3519,1917,3519,1916,3520,1914,3521,1913,3522,1912,3523,1910,3524,1906,3525,1905,3525,1911,3526,1929,3527,1967,3527,2027,3528,2094,3529,2143,3529,2165,3530,2150,3530,2104,3530,2042,3531,1990,3531,1955,3532,1939,3533,1933,3534,1935,3535,1938,3535,1938,3536,1939,3537,1940,3537,1940,3538,1940,3539,1942,3539,1943,3540,1944,3541,1945,3541,1948,3542,1951,3543,1953,3543,1955,3544,1957,3544,1958,3545,1959,3546,1960,3548,1965,3549,1969,3549,1972,3550,1977,3550,1983,3550,1989,3550,1995,3551,2002,3552,2010,3552,2015,3552,2021,3550,2024,3549,2028,3548,2029,3546,2031,3545,2031,3544,2031,3541,2029,3539,2028,3536,2023,3534,2016,3531,2008,3528,1999,3526,1988,3524,1979,3521,1971,3519,1963,3516,1956,3514,1951,3511,1945,3509,1940,3507,1937,3506,1936,3504,1935,3502,1935,3501,1935,3499,1935,3497,1935,3495,1935,3494,1937,3492,1939,3491,1942,3490,1943,3489,1945,3488,1946,3488,1946,3487,1946,3487,1946,3488,1946,3487,1946,3486,1947,3486,1946,3486,1944,3485,1944,3486,1944,3486,1944,3487,1944,3488,1947,3488,1949,3488,1949,3489,1949,3489,1950,3490,1949,3490,1949,3491,1951,3492,1951,3492,1950,3493,1950,3494,1949,3494,1948,3495,1946,3495,1945,3496,1945,3496,1946,3497,1947,3497,1947,3497,1947,3498,1948,3499,1947,3499,1944,3500,1943,3501,1940,3501,1936,3502,1934,3503,1934,3503,1935,3504,1936,3504,1936,3505,1935,3506,1934,3507,1935,3507,1933,3509,1929,3509,1925,3509,1923,3509,1924,3510,1935,3511,1963,3511,2011,3512,2072,3513,2126,3514,2157,3514,2156,3516,2119,3517,2061,3518,2004,3519,1963,3520,1941,3520,1934,3521,1935,3521,1938,3522,1940,3523,1942,3524,1944,3524,1944,3526,1944,3526,1946,3527,1947,3527,1947,3528,1947,3529,1949,3530,1950,3530,1954,3531,1956,3532,1960,3532,1963,3533,1965,3534,1966,3535,1969,3535,1970,3537,1974,3537,1979,3538,1982,3539,1987,3540,1992,3540,1998,3541,2003,3542,2010,3542,2015,3541,2024,3541,2029,3540,2033,3538,2036,3536,2038,3534,2037,3532,2036,3530,2034,3529,2029,3527,2022,3525,2013,3522,2002,3520,1990,3518,1979,3515,1971,3513,1964,3511,1958,3509,1954,3507,1950,3506,1948,3503,1946,3502,1944,3500,1943,3498,1943,3497,1942,3496,1943,3494,1944,3493,1945,3492,1946,3491,1948,3490,1948,3489,1948,3488,1951,3488,1951,3488,1952,3487,1952,3487,1952,3486,1952,3485,1951,3485,1950,3484,1950,3485,1950,3485,1950,3485,1950,3486,1950,3487,1950,3488,1951,3488,1952,3489,1952,3489,1952,3490,1952,3490,1951,3491,1949,3492,1948,3492,1947,3493,1948,3494,1949,3495,1951,3496,1952,3497,1953,3497,1950,3498,1947,3498,1946,3499,1945,3499,1946,3500,1947,3500,1951,3501,1954,3502,1956,3503,1957,3505,1959,3506,1959,3506,1959,3507,1957,3508,1955,3508,1953,3508,1954,3509,1951,3510,1951,3511,1950,3511,1950,3512,1950,3513,1951,3514,1951,3514,1948,3515,1943,3515,1940,3515,1941,3516,1953,3517,1981,3519,2029,3520,2088,3521,2139,3523,2164,3523,2157,3524,2117,3525,2057,3525,2002,3526,1964,3527,1945,3528,1940,3529,1942,3529,1945,3530,1947,3530,1949,3531,1951,3532,1953,3533,1956,3534,1957,3535,1959,3536,1960,3536,1962,3537,1962,3538,1963,3539,1964,3540,1966,3540,1966,3542,1968,3543,1970,3543,1972,3544,1976,3545,1979,3546,1983,3547,1987,3548,1992,3549,1997,3551,2002,3551,2008,3551,2013,3552,2021,3552,2026,3551,2029,3551,2030,3550,2033,3549,2034,3547,2034,3546,2036,3544,2036,3543,2035,3541,2029,3539,2023,3537,2014,3536,2004,3533,1991,3531,1981,3529,1969,3527,1958,3525,1950,3523,1946,3522,1940,3520,1936,3518,1933,3516,1931,3515,1928,3514,1926,3513,1925,3512,1923,3510,1923,3509,1923,3508,1924,3507,1923,3506,1924,3506,1923,3505,1922,3504,1921,3503,1924,3503,1923,3503,1922,3503,1921,3504,1922,3504,1920,3504,1921,3504,1921,3505,1923,3505,1921,3506,1921,3507,1919,3508,1918,3509,1915,3509,1914,3510,1912,3510,1911,3511,1909,3511,1909,3512,1909,3513,1910,3514,1911,3515,1912,3516,1910,3517,1908,3518,1906,3518,1905,3519,1904,3520,1906,3521,1907,3521,1907,3523,1905,3523,1904,3524,1903,3525,1903,3526,1904,3527,1906,3528,1907,3529,1909,3530,1909,3531,1909,3532,1908,3532,1908,3533,1906,3534,1904,3535,1900,3535,1898,3536,1895,3537,1893,3538,1891,3539,1890,3540,1890,3541,1891,3541,1891,3542,1889,3543,1885,3544,1883,3545,1882,3546,1891,3548,1915,3548,1959,3549,2016,3550,2070,3551,2104,3551,2106,3552,2074,3554,2018,3554,1962,3555,1919,3556,1894,3557,1886,3559,1888,3560,1891,3561,1894,3561,1898,3561,1899,3561,1899,3562,1898,3564,1897,3565,1894,3566,1893,3568,1895,3569,1896,3570,1896,3570,1897,3571,1898,3572,1896,3573,1897,3574,1899,3575,1901,3576,1903,3577,1906,3578,1909,3578,1911,3579,1917,3580,1920,3581,1926,3583,1932,3583,1938,3584,1941,3584,1945,3584,1949,3584,1952,3584,1956,3583,1958,3583,1960,3582,1959,3581,1958,3580,1955,3578,1949,3577,1943,3575,1935,3573,1926,3571,1918,3569,1909,3567,1898,3566,1890,3565,1880,3564,1872,3563,1866,3561,1861,3560,1857,3559,1855,3558,1851,3557,1847,3556,1844,3555,1842,3553,1841,3552,1842,3551,1845,3550,1847,3549,1848,3549,1848,3549,1850,3549,1849,3549,1848,3549,1849,3549,1848,3549,1846,3549,1846,3549,1846,3550,1846,3550,1847,3551,1848,3551,1848,3552,1849,3553,1849,3554,1848,3554,1847,3556,1846,3556,1845,3557,1844,3558,1843,3558,1844,3559,1843,3559,1844,3559,1844,3560,1844,3561,1843,3561,1844,3563,1844,3563,1843,3564,1843,3565,1842,3566,1841,3567,1841,3568,1842,3568,1842,3569,1843,3569,1844,3570,1845,3570,1847,3572,1849,3572,1848,3574,1847,3574,1844,3575,1841,3576,1839,3577,1838,3577,1837,3578,1836,3579,1834,3580,1828,3581,1823,3583,1823,3585,1831,3586,1855,3587,1897,3587,1956,3589,2008,3589,2044,3590,2049,3592,2023,3593,1970,3593,1917,3594,1872,3596,1846,3596,1837,3598,1838,3599,1841,3600,1845,3601,1850,3601,1851,3601,1853,3602,1854,3603,1855,3604,1856,3606,1857,3607,1857,3608,1858,3609,1858,3610,1859,3610,1860,3611,1864,3613,1867,3613,1871,3615,1875,3616,1877,3617,1879,3617,1882,3618,1885,3619,1888,3620,1893,3620,1898,3622,1902,3622,1908,3623,1917,3623,1923,3625,1931,3625,1938,3625,1942,3625,1942,3624,1943,3623,1943,3622,1940,3621,1937,3619,1933,3618,1928,3616,1920,3615,1913,3614,1902,3612,1892,3611,1883,3609,1875,3608,1868,3606,1864,3605,1860,3604,1856,3603,1854,3602,1851,3602,1849,3601,1847,3599,1847,3598,1846,3596,1846,3594,1847,3594,1848,3593,1848,3592,1850,3592,1849,3592,1849,3591,1851,3590,1854,3590,1857,3589,1861,3589,1865,3589,1866,3589,1865,3589,1863,3589,1864,3590,1864,3591,1863,3591,1864,3592,1867,3592,1868,3592,1870,3593,1873,3594,1873,3594,1872,3594,1874,3595,1876,3595,1879,3595,1882,3596,1885,3597,1887,3597,1888,3598,1888,3598,1889,3599,1891,3600,1892,3601,1894,3601,1895,3602,1896,3603,1896,3603,1895,3603,1893,3604,1892,3605,1892,3606,1892,3607,1894,3608,1895,3610,1897,3611,1896,3612,1897,3612,1896,3614,1895,3615,1894,3615,1895,3616,1898,3617,1899,3618,1900,3620,1898,3621,1895,3623,1893,3624,1902,3625,1919,3627,1955,3628,2009,3629,2068,3629,2113,3630,2132,3631,2119,3632,2076,3634,2022,3634,1974,3635,1944,3636,1931,3637,1929,3638,1931,3639,1933,3640,1936,3640,1937,3641,1939,3642,1941,3643,1946,3644,1948,3645,1952,3646,1954,3648,1954,3648,1952,3649,1952,3650,1952,3651,1954,3651,1958,3651,1961,3652,1965,3653,1970,3653,1973,3654,1978,3656,1983,3656,1989,3657,1994,3658,2000,3658,2006,3658,2012,3659,2017,3659,2022,3659,2030,3659,2035,3658,2039,3658,2042,3657,2045,3656,2044,3655,2042,3654,2040,3652,2036,3651,2029,3649,2022,3648,2013,3646,2001,3644,1991,3643,1983,3641,1974,3640,1968,3639,1961,3638,1953,3636,1948,3634,1945,3632,1942,3631,1940,3629,1941,3628,1940,3627,1937,3626,1936,3625,1937,3624,1938,3624,1940,3623,1941,3622,1943,3622,1943,3621,1940,3620,1939,3619,1939,3618,1938,3617,1939,3617,1941,3616,1942,3617,1944,3616,1946,3616,1946,3616,1946,3615,1944,3615,1943,3616,1941,3616,1939,3616,1937,3617,1937,3617,1935,3618,1935,3618,1936,3618,1937,3618,1939,3618,1940,3618,1941,3618,1941,3619,1941,3619,1940,3620,1940,3620,1940,3620,1938,3621,1936,3621,1935,3621,1934,3621,1933,3621,1933,3621,1934,3622,1936,3622,1937,3623,1938,3623,1940,3624,1940,3624,1939,3625,1935,3625,1932,3626,1928,3627,1926,3628,1927,3628,1930,3628,1932,3628,1935,3629,1936,3629,1936,3630,1932,3631,1925,3631,1920,3632,1919,3632,1927,3632,1950,3633,1996,3633,2056,3633,2118,3634,2161,3634,2170,3635,2143,3637,2088,3637,2027,3638,1974,3639,1945,3639,1933,3639,1932,3640,1933,3641,1934,3641,1934,3642,1934,3642,1933,3642,1934,3643,1935,3644,1937,3644,1938,3645,1940,3646,1939,3646,1940,3647,1939,3647,1940,3647,1942,3647,1943,3649,1943,3649,1946,3650,1945,3650,1945,3650,1949,3650,1956,3651,1960,3651,1967,3653,1973,3653,1977,3654,1980,3654,1985,3654,1990,3653,1995,3652,1999,3652,2002,3651,2003,3649,2002,3648,2000,3647,1999,3645,1994,3642,1989,3641,1985,3639,1978,3637,1968,3634,1956,3633,1942,3631,1932,3628,1921,3627,1915,3625,1910,3623,1906,3621,1900,3620,1897,3618,1894,3615,1891,3614,1892,3612,1891,3611,1890,3609,1888,3609,1887,3608,1883,3607,1879,3605,1877,3604,1877,3602,1878,3601,1879,3599,1884,3599,1887,3598,1891,3597,1893,3597,1894,3596,1893,3596,1894,3596,1891,3595,1891,3596,1894,3596,1895,3596,1893,3596,1892,3597,1891,3597,1888,3597,1889,3597,1893,3597,1896,3598,1896,3598,1897,3598,1895,3599,1893,3600,1892,3600,1893,3601,1895,3602,1896,3603,1896,3603,1895,3603,1898,3603,1900,3603,1904,3604,1906,3605,1911,3606,1911,3606,1913,3607,1913,3608,1912,3608,1908,3608,1908,3609,1907,3609,1908,3610,1914,3611,1916,3612,1914,3613,1912,3614,1906,3614,1897,3615,1892,3615,1891,3616,1893,3617,1898,3618,1902,3619,1902,3620,1898,3621,1890,3621,1881,3622,1878,3622,1881,3623,1894,3624,1925,3625,1975,3625,2039,3626,2099,3627,2139,3627,2144,3627,2115,3628,2061,3629,2005,3630,1957,3630,1925,3631,1908,3631,1900,3631,1899,3632,1901,3632,1904,3633,1908,3634,1909,3635,1911,3636,1913,3637,1920,3638,1926,3639,1926,3640,1925,3640,1926,3641,1925,3641,1926,3642,1930,3643,1933,3644,1934,3645,1935,3645,1938,3647,1944,3647,1939,3647,1933,3649,1935,3649,1940,3649,1952,3650,1977,3652,2002,3652,2022,3653,2035,3654,2044,3654,2048,3653,2044,3653,2040,3652,2038,3651,2041,3650,2045,3649,2049,3648,2051,3646,2049,3645,2039,3644,2028,3643,2010,3642,1996,3641,1991,3639,1995,3638,2003,3636,2023,3635,2036,3634,2034,3632,2021,3631,2009,3630,1999,3629,1997,3628,2004,3627,2016,3626,2024,3624,2027,3623,2021,3622,2011,3621,2000,3621,1991,3621,1987,3621,1990,3620,1994,3619,1998,3619,2001,3617,2003,3617,2000,3617,1997,3617,1994,3616,1992,3617,1992,3617,1992,3617,1990,3616,1987,3616,1986,3616,1987,3616,1992,3616,1997,3617,2001,3617,2001,3616,1996,3616,1987,3616,1981,3615,1980,3615,1983,3616,1987,3616,1991,3615,1992,3616,1993,3615,1991,3615,1990,3615,1993,3615,1996,3615,1999,3616,2001,3616,2000,3616,1998,3618,1998,3619,2002,3620,2004,3621,2007,3622,2007,3623,2006,3624,1997,3623,1991,3624,1985,3625,1985,3626,1985,3626,1989,3627,1991,3628,1993,3628,1993,3630,1991,3631,1990,3632,1991,3633,1991,3634,1989,3634,1987,3635,1978,3637,1970,3638,1964,3639,1966,3640,1979,3641,2012,3641,2064,3641,2124,3642,2167,3642,2182,3643,2166,3643,2119,3644,2061,3644,2013,3644,1984,3645,1972,3644,1971,3644,1973,3644,1975,3643,1979,3643,1980,3643,1981,3642,1984,3642,1987,3642,1990,3641,1993,3642,1997,3642,2000,3642,2003,3642,2006,3642,2009,3642,2009,3642,2010,3642,2013,3643,2015,3643,2018,3643,2023,3644,2028,3645,2032,3646,2038,3646,2046,3646,2056,3647,2065,3647,2077,3647,2088,3647,2099,3647,2107,3645,2115,3644,2120,3643,2123,3641,2125,3640,2125,3639,2123,3637,2121,3635,2117,3633,2110,3631,2105,3629,2098,3627,2090,3624,2080,3622,2072,3619,2062,3617,2054,3614,2047,3612,2039,3609,2035,3606,2033,3604,2030,3602,2030,3600,2031,3598,2027,3595,2026,3593,2023,3591,2022,3589,2022,3587,2022,3585,2021,3584,2021,3582,2018,3581,2015,3581,2015,3580,2016,3579,2013,3578,2014,3577,2015,3576,2016,3575,2015,3574,2014,3574,2013,3573,2009,3573,2005,3572,2001,3573,2001,3573,2000,3573,2001,3573,2001,3573,2002,3573,1999,3573,1996,3573,1995,3573,1993,3574,1990,3574,1989,3574,1988,3574,1985,3575,1983,3575,1981,3575,1979,3575,1976,3575,1974,3575,1973,3575,1971,3575,1969,3576,1968,3576,1968,3576,1966,3577,1966,3578,1965,3577,1964,3578,1966,3578,1966,3578,1965,3578,1964,3579,1964,3579,1963,3580,1964,3580,1964,3581,1964,3581,1964,3581,1962,3581,1959,3582,1956,3582,1954,3583,1951,3583,1950,3584,1949,3585,1948,3585,1946,3586,1945,3586,1943,3587,1943,3587,1942,3588,1940,3588,1938,3588,1932,3588,1929,3588,1932,3589,1947,3590,1974,3591,2019,3592,2075,3592,2125,3592,2151,3592,2147,3592,2111,3592,2055,3592,1996,3592,1952,3594,1931,3594,1926,3595,1929,3595,1933,3596,1935,3596,1934,3597,1930,3597,1924,3598,1913,3598,1898,3599,1884,3599,1881,3600,1887,3601,1907,3601,1933,3602,1956,3602,1970,3603,1973,3604,1967,3604,1960,3604,1955,3605,1951,3606,1951,3606,1954,3606,1959,3608,1961,3609,1964,3609,1970,3610,1974,3611,1980,3611,1987,3612,1994,3612,1998,3613,2004,3613,2008,3612,2013,3611,2015,3610,2017,3608,2018,3607,2018,3606,2015,3605,2013,3603,2009,3601,2001,3599,1993,3597,1987,3594,1979,3593,1972,3591,1965,3589,1960,3587,1954,3585,1951,3583,1949,3581,1946,3579,1946,3577,1945,3575,1945,3573,1946,3571,1948,3570,1948,3568,1950,3566,1949,3564,1948,3562,1946,3561,1944,3560,1942,3558,1941,3557,1939,3557,1937,3555,1937,3554,1936,3554,1934,3554,1933,3552,1930,3552,1927,3552,1925,3551,1922,3550,1919,3550,1918,3550,1913,3549,1911,3549,1907,3550,1905,3550,1902,3550,1899,3550,1895,3550,1893,3550,1890,3550,1888,3550,1887,3550,1886,3549,1886,3549,1885,3550,1885,3550,1887,3551,1886,3551,1886,3551,1886,3551,1886,3552,1887,3552,1889,3552,1893,3553,1894,3554,1896,3554,1896,3555,1895,3555,1893,3556,1892,3556,1891,3557,1891,3558,1889,3558,1888,3559,1886,3559,1883,3560,1881,3560,1880,3560,1878,3560,1875,3561,1874,3561,1872,3561,1866,3562,1861,3563,1858,3563,1862,3564,1877,3565,1909,3566,1960,3567,2020,3567,2067,3568,2087,3569,2077,3569,2034,3570,1972,3571,1916,3571,1879,3571,1857,3572,1851,3573,1855,3573,1861,3573,1864,3574,1868,3574,1871,3574,1872,3575,1872,3575,1872,3576,1874,3577,1874,3578,1874,3579,1876,3580,1877,3580,1876,3581,1878,3581,1880,3582,1881,3582,1885,3583,1889,3584,1892,3584,1895,3585,1899,3586,1902,3587,1906,3588,1914,3589,1919,3590,1922,3591,1927,3592,1932,3592,1935,3593,1938,3593,1944,3593,1947,3591,1952,3591,1956,3590,1957,3589,1955,3588,1952,3587,1945,3586,1936,3584,1927,3582,1916,3581,1905,3579,1892,3577,1883,3576,1874,3574,1868,3572,1864,3570,1862,3568,1860,3567,1858,3566,1853,3563,1849,3562,1846,3561,1843,3560,1841,3558,1841,3557,1839,3556,1838,3556,1836,3555,1836,3554,1837,3554,1838,3553,1838,3552,1838,3552,1838,3551,1837,3550,1838,3550,1838,3549,1839,3548,1841,3549,1842,3549,1840,3550,1839,3550,1837,3550,1836,3550,1835,3551,1834,3550,1833,3551,1834,3551,1833,3552,1833,3552,1832,3553,1832,3554,1831,3555,1830,3556,1829,3557,1829,3557,1830,3557,1830,3558,1830,3558,1830,3559,1831,3559,1832,3560,1833,3560,1836,3561,1838,3561,1836,3562,1837,3562,1838,3563,1838,3564,1837,3565,1837,3565,1836,3566,1834,3567,1832,3567,1833,3567,1833,3568,1834,3569,1836,3569,1839,3571,1840,3571,1841,3572,1839,3573,1835,3573,1833,3573,1837,3574,1856,3574,1890,3575,1943,3576,2003,3576,2050,3577,2069,3578,2055,3578,2010,3579,1952,3580,1900,3581,1867,3582,1853,3582,1854,3584,1862,3584,1868,3584,1873,3585,1878,3585,1880,3586,1880,3587,1883,3588,1886,3589,1889,3589,1892,3590,1895,3591,1897,3591,1900,3592,1903,3593,1907,3594,1912,3595,1916,3595,1921,3596,1924,3597,1928,3598,1933,3599,1940,3600,1948,3601,1956,3602,1967,3602,1978,3603,1988,3603,1998,3604,2007,3605,2014,3606,2020,3606,2024,3607,2028,3606,2033,3606,2035,3605,2037,3604,2037,3603,2035,3601,2032,3600,2028,3599,2020,3597,2012,3596,2003,3595,1993,3593,1983,3591,1975,3589,1967,3588,1961,3586,1956,3585,1955,3583,1952,3582,1952,3580,1950,3579,1949,3577,1948,3576,1949,3575,1948,3574,1949,3574,1952,3572,1954,3571,1955,3571,1958,3569,1960,3568,1961,3568,1961,3567,1964,3566,1965,3566,1966,3567,1966,3566,1968,3566,1969,3566,1971,3566,1974,3567,1976,3566,1979,3567,1981,3567,1981,3567,1982,3567,1984,3568,1984,3569,1984,3570,1984,3571,1985,3572,1985,3572,1986,3571,1987,3572,1988,3572,1988,3572,1989,3573,1992,3574,1996,3574,2000,3575,2002,3575,2005,3575,2010,3576,2011,3577,2013,3577,2015,3578,2014,3579,2014,3579,2013,3580,2012,3581,2013,3581,2013,3581,2010,3582,2010,3582,2012,3583,2012,3583,2011,3584,2010,3585,2010,3586,2012,3586,2023,3587,2050,3588,2095,3589,2158,3589,2221,3590,2264,3591,2276,3591,2254,3592,2199,3593,2140,3594,2090,3594,2059,3596,2049,3596,2052,3597,2058,3597,2066,3598,2073,3598,2077,3598,2080,3599,2083,3600,2086,3601,2089,3601,2094,3602,2098,3603,2104,3603,2109,3604,2117,3604,2119,3605,2125,3605,2126,3606,2130,3607,2130,3607,2134,3607,2139,3608,2145,3608,2154,3610,2163,3611,2174,3612,2184,3613,2190,3614,2197,3614,2202,3614,2206,3614,2210,3613,2217,3613,2221,3612,2226,3612,2232,3611,2235,3610,2238,3609,2239,3608,2237,3606,2230,3605,2224,3603,2215,3601,2205,3599,2194,3597,2184,3594,2175,3592,2166,3590,2159,3588,2154,3587,2151,3585,2146,3584,2143,3582,2143,3581,2142,3580,2141,3578,2143,3577,2144,3576,2144,3576,2144,3575,2144,3573,2142,3572,2142,3572,2141,3570,2140,3570,2140,3568,2143,3567,2141,3566,2141,3565,2140,3564,2140,3564,2139,3565,2140,3564,2141,3565,2142,3565,2141,3566,2141,3565,2139,3566,2138,3565,2136,3566,2134,3566,2133,3567,2132,3568,2130,3568,2129,3568,2127,3568,2125,3568,2124,3569,2123,3569,2121,3570,2122,3570,2123,3571,2124,3571,2125,3572,2128,3573,2130,3573,2131,3573,2129,3573,2127,3573,2122,3573,2117,3573,2113,3574,2111,3575,2110,3576,2110,3577,2108,3578,2107,3578,2104,3579,2100,3579,2095,3580,2090,3580,2086,3581,2088,3581,2101,3582,2129,3583,2178,3584,2242,3585,2295,3586,2325,3586,2320,3587,2280,3587,2218,3588,2160,3588,2118,3589,2096,3589,2090,3589,2093,3590,2095,3591,2096,3592,2095,3593,2095,3594,2092,3594,2091,3595,2091,3595,2092,3596,2091,3597,2091,3599,2092,3600,2090,3600,2089,3601,2088,3602,2088,3602,2087,3603,2088,3603,2088,3604,2090,3604,2092,3605,2092,3606,2095,3607,2098,3608,2103,3609,2106,3610,2113,3611,2121,3612,2127,3612,2131,3612,2137,3612,2139,3611,2140,3610,2141,3608,2140,3606,2137,3605,2134,3604,2129,3602,2121,3600,2114,3599,2104,3598,2092,3596,2080,3595,2068,3593,2055,3591,2044,3588,2034,3586,2024,3584,2017,3582,2010,3580,2005,3578,2000,3576,1997,3574,1993,3573,1991,3571,1989,3570,1988,3569,1988,3568,1988,3567,1986,3566,1984,3564,1981,3563,1979,3562,1978,3561,1975,3560,1974,3560,1972,3559,1971,3559,1969,3558,1970,3558,1966,3558,1966,3557,1964,3557,1962,3557,1960,3557,1959,3557,1958,3557,1956,3557,1953,3558,1950,3559,1948,3560,1947,3560,1946,3561,1944,3561,1942,3562,1940,3563,1939,3563,1939,3564,1939,3565,1938,3565,1940,3565,1939,3567,1938,3566,1936,3568,1933,3568,1928,3569,1923,3569,1917,3570,1913,3570,1907,3570,1905,3571,1902,3572,1898,3573,1894,3573,1894,3574,1889,3576,1884,3576,1880,3577,1880,3578,1887,3578,1911,3579,1955,3579,2014,3580,2071,3581,2107,3581,2109,3582,2074,3583,2013,3583,1948,3584,1897,3585,1865,3586,1853,3586,1853,3587,1856,3589,1859,3590,1859,3590,1859,3591,1856,3592,1852,3592,1850,3593,1848,3594,1847,3595,1847,3595,1848,3596,1848,3597,1847,3597,1845,3598,1844,3599,1843,3600,1842,3601,1844,3602,1846,3602,1849,3603,1851,3604,1854,3605,1855,3606,1860,3607,1864,3608,1868,3608,1875,3609,1881,3609,1887,3610,1892,3609,1897,3609,1900,3608,1902,3607,1903,3605,1900,3604,1898,3603,1891,3601,1885,3600,1874,3599,1864,3596,1851,3595,1839,3593,1826,3591,1815,3589,1806,3588,1798,3586,1790,3585,1782,3583,1777,3581,1771,3579,1766,3578,1762,3576,1758,3575,1755,3574,1753,3573,1752,3572,1751,3571,1754,3570,1754,3568,1754,3568,1756,3567,1758,3566,1757,3566,1758,3565,1759,3564,1758,3564,1758,3564,1759,3563,1760,3564,1761,3563,1763,3563,1765,3563,1765,3563,1766,3562,1767,3563,1767,3563,1768,3563,1769,3564,1771,3565,1773,3565,1774,3566,1776,3566,1778,3566,1780,3567,1782,3567,1783,3568,1783,3568,1786,3569,1786,3569,1786,3570,1787,3571,1786,3572,1784,3572,1782,3573,1780,3573,1780,3573,1781,3574,1781,3574,1780,3575,1782,3576,1782,3576,1780,3576,1778,3577,1775,3577,1769,3578,1767,3579,1774,3579,1793,3579,1832,3580,1894,3580,1963,3580,2016,3582,2040,3583,2024,3584,1975,3584,1909,3586,1853,3586,1814,3587,1798,3587,1797,3588,1799,3588,1804,3589,1806,3589,1808,3590,1807,3590,1808,3591,1807,3591,1808,3593,1807,3593,1809,3595,1810,3595,1811,3596,1811,3597,1813,3597,1815,3598,1816,3599,1819,3599,1821,3600,1823,3601,1826,3602,1830,3602,1835,3603,1841,3604,1849,3604,1859,3605,1865,3606,1871,3606,1877,3605,1884,3605,1889,3604,1895,3603,1900,3602,1905,3601,1907,3599,1908,3599,1908,3597,1906,3595,1903,3594,1897,3593,1890,3590,1882,3588,1873,3586,1861,3584,1849,3582,1839,3580,1829,3578,1819,3576,1813,3574,1807,3573,1803,3571,1799,3569,1798,3567,1796,3566,1797,3564,1795,3563,1794,3561,1793,3561,1793,3559,1792,3559,1792,3557,1792,3556,1792,3555,1792,3553,1794,3553,1796,3552,1796,3551,1796,3551,1796,3550,1796,3550,1795,3550,1797,3551,1798,3550,1798,3550,1795,3550,1794,3550,1794,3550,1791,3550,1788,3551,1788,3552,1787,3552,1784,3552,1783,3553,1783,3553,1784,3554,1785,3554,1786,3555,1788,3556,1790,3557,1790,3557,1788,3557,1787,3557,1784,3558,1780,3559,1777,3560,1773,3561,1771,3562,1770,3562,1771,3562,1771,3563,1772,3563,1772,3563,1771,3564,1766,3564,1761,3565,1755,3566,1754,3567,1758,3568,1779,3568,1818,3569,1877,3571,1934,3572,1931,3836,1911,3836,1875,3837,1819,3838,1763,3839,1763,3576,1764,3577,1764,3578,1766,3578,1767,3579,1768,3580,1766,3581,1765,3582,1765,3583,1766,3584,1766,3585,1769,3586,1772,3586,1774,3587,1775,3587,1778,3588,1777,3588,1780,3589,1781,3590,1785,3590,1786,3591,1792,3592,1797,3593,1803,3594,1810,3595,1816,3595,1823,3595,1830,3596,1836,3595,1842,3594,1849,3594,1852,3593,1854,3591,1856,3589,1855,3588,1853,3587,1849,3585,1844,3583,1836,3581,1828,3579,1819,3577,1809,3575,1799,3573,1790,3571,1779,3569,1771,3567,1764,3565,1756,3562,1749,3560,1745,3559,1740,3557,1737,3555,1735,3553,1734,3552,1734,3550,1731,3548,1732,3546,1732,3545,1734,3543,1735,3542,1738,3541,1738,3541,1738,3540,1739,3539,1740,3539,1741,3538,1742,3537,1743,3537,1743,3536,1742,3537,1743,3537,1744,3537,1743,3537,1743,3536,1742,3536,1740,3537,1737,3537,1738,3538,1738,3539,1738,3540,1738,3540,1738,3540,1736,3540,1736,3541,1738,3541,1740,3542,1740,3543,1742,3544,1744,3545,1743,3545,1742,3546,1739,3546,1738,3546,1735,3547,1734,3547,1732,3547,1733,3548,1731,3548,1732,3549,1731,3550,1731,3551,1731,3551,1730,3552,1727,3553,1722,3553,1717,3553,1716,3553,1723,3554,1746,3554,1792,3554,1858,3555,1922,3557,1968,3558,1980,3558,1953,3559,1895,3560,1832,3561,1779,3561,1746,3561,1733,3563,1732,3563,1733,3563,1734,3563,1735,3564,1736,3564,1735,3565,1736,3566,1737,3566,1736,3567,1736,3568,1736,3568,1737,3570,1740,3571,1744,3572,1746,3572,1750,3572,1754,3573,1757,3573,1760,3573,1764,3575,1767,3576,1771,3577,1775,3578,1782,3579,1789,3579,1797,3580,1805,3580,1813,3580,1820,3580,1826,3580,1831,3580,1834,3578,1836,3577,1839,3575,1840,3573,1841,3571,1841,3569,1839,3567,1834,3565,1826,3564,1818,3562,1807,3559,1797,3558,1785,3555,1776,3553,1768,3552,1761,3550,1755,3547,1750,3545,1745,3543,1741,3541,1738,3539,1736,3538,1734,3537,1735,3535,1733,3534,1731,3532,1731,3531,1730,3529,1732,3528,1735,3527,1739,3526,1740,3525,1744,3524,1743,3523,1741,3522,1739,3521,1738,3522,1734,3521,1733,3521,1734,3522,1734,3522,1733,3522,1733,3523,1731,3524,1728,3524,1726,3525,1724,3525,1723,3525,1722,3525,1723,3526,1723,3527,1722,3527,1720,3528,1719,3529,1719,3530,1719,3530,1718,3531,1721,3532,1721,3532,1720,3533,1721,3533,1719,3534,1717,3534,1715,3534,1713,3535,1711,3535,1711,3536,1711,3537,1711,3539,1711,3539,1710,3540,1710,3540,1707,3541,1706,3541,1703,3542,1701,3543,1705,3544,1719,3545,1749,3546,1801,3546,1869,3546,1927,3546,1961,3547,1958,3548,1921,3549,1857,3550,1799,3551,1755,3551,1733,3551,1727,3552,1729,3553,1733,3554,1734,3556,1735,3556,1733,3557,1733,3557,1734,3559,1735,3559,1735,3560,1739,3560,1738,3561,1739,3562,1741,3563,1743,3564,1745,3565,1749,3566,1751,3567,1753,3567,1755,3568,1757,3568,1760,3570,1766,3570,1772,3571,1780,3572,1787,3573,1793,3574,1800,3574,1808,3574,1814,3574,1821,3574,1828,3572,1832,3572,1834,3570,1837,3569,1834,3564,1829,3560,1823,3556,1815,3552,1802,3549,1792,3549,1782,3548,1774,3547,1766,3545,1763,3543,1757,3541,1754,3539,1751,3537,1749,3536,1747,3534,1747,3533,1749,3532,1747,3531,1749,3530,1749,3529,1749,3528,1748,3527,1751,3527,1753,3526,1755,3526,1755,3526,1757,3525,1758,3525,1758,3525,1757,3525,1758,3525,1760,3525,1759,3525,1760,3525,1760,3525,1761,3526,1760,3528,1761,3528,1760,3530,1760,3531,1761,3531,1760,3531,1761,3532,1760,3532,1759,3532,1759,3533,1759,3534,1758,3535,1761,3536,1765,3537,1768,3537,1771,3538,1772,3538,1770,3539,1769,3539,1767,3540,1766,3540,1766,3541,1768,3542,1768,3543,1768,3543,1768,3544,1768,3544,1768,3545,1768,3545,1767,3546,1764,3547,1760,3548,1759,3549,1766,3550,1785,3551,1825,3553,1883,3553,1944,3554,1988,3555,2003,3556,1983,3556,1932,3558,1871,3559,1822,3560,1790,3561,1778,3561,1777,3561,1780,3562,1782,3562,1784,3563,1784,3564,1784,3565,1784,3565,1784,3566,1785,3567,1785,3567,1787,3568,1788,3569,1789,3569,1791,3569,1794,3571,1796,3572,1798,3573,1801,3575,1802,3576,1804,3577,1806,3577,1810,3579,1815,3580,1819,3581,1824,3582,1832,3582,1838,3583,1845,3583,1851,3583,1857,3583,1859,3583,1862,3583,1864,3581,1867,3581,1867,3580,1867,3578,1866,3576,1863,3575,1860,3573,1855,3572,1849,3571,1840,3569,1830,3568,1819,3566,1810,3564,1800,3562,1792,3561,1785,3559,1779,3556,1774,3556,1768,3555,1766,3553,1762,3553,1758,3552,1755,3550,1754,3549,1751,3548,1751,3546,1751,3546,1751,3546,1751,3545,1751,3545,1749,3545,1748,3544,1745,3544,1742,3544,1740,3544,1741,3545,1741,3544,1740,3545,1738,3545,1737,3545,1734,3547,1733,3547,1731,3548,1730,3549,1729,3550,1727,3549,1725,3550,1723,3551,1723,3551,1720,3551,1720,3552,1719,3553,1719,3553,1718,3554,1717,3555,1718,3556,1716,3556,1715,3557,1715,3558,1714,3559,1712,3560,1711,3560,1709,3561,1707,3562,1705,3562,1705,3563,1705,3564,1705,3565,1706,3566,1704,3566,1700,3567,1696,3567,1694,3567,1698,3568,1716,3570,1752,3571,1809,3572,1871,3573,1916,3575,1932,3575,1912,3576,1860,3576,1797,3577,1745,3577,1709,3578,1694,3579,1691,3580,1693,3581,1694,3582,1695,3583,1696,3583,1695,3584,1694,3585,1692,3586,1692,3587,1693,3587,1695,3587,1695,3587,1698,3588,1699,3589,1701,3590,1703,3591,1705,3593,1705,3593,1706,3594,1709,3594,1713,3595,1717,3596,1723,3597,1730,3598,1735,3599,1739,3600,1744,3601,1750,3602,1755,3603,1760,3602,1765,3602,1768,3602,1770,3601,1771,3601,1772,3600,1771,3599,1769,3597,1768,3596,1764,3594,1758,3592,1752,3590,1745,3589,1736,3586,1726,3585,1719,3584,1709,3583,1699,3581,1693,3580,1688,3578,1682,3576,1680,3574,1680,3573,1678,3572,1679,3571,1681,3570,1682,3569,1684,3567,1684,3567,1683,3566,1683,3565,1686,3565,1687,3565,1690,3564,1690,3563,1692,3562,1693,3562,1694,3561,1694,3561,1697,3561,1699,3561,1699,3561,1701,3561,1702,3562,1704,3563,1706,3563,1706,3564,1707,3564,1708,3565,1707,3566,1708,3566,1707,3566,1708,3566,1708,3566,1709,3567,1708,3567,1710,3567,1712,3568,1714,3569,1717,3570,1718,3571,1717,3572,1715,3572,1713,3572,1711,3572,1712,3572,1713,3573,1712,3573,1714,3574,1713,3575,1711,3576,1710,3577,1707,3578,1704,3579,1706,3579,1717,3580,1738,3580,1779,3581,1838,3582,1900,3583,1945,3584,1961,3584,1944,3585,1895,3586,1836,3586,1787,3586,1753,3587,1739,3587,1738,3588,1740,3589,1742,3590,1745,3590,1747,3592,1750,3592,1753,3593,1754,3593,1755,3594,1756,3595,1757,3596,1758,3596,1759,3597,1762,3598,1766,3599,1767,3600,1770,3601,1773,3602,1775,3602,1777,3603,1780,3604,1783,3604,1788,3605,1794,3606,1799,3607,1805,3607,1811,3608,1818,3609,1824,3609,1829,3609,1834,3609,1839,3608,1841,3608,1842,3606,1842,3605,1843,3603,1841,3602,1840,3600,1835,3599,1830,3597,1822,3595,1814,3593,1804,3591,1796,3589,1787,3588,1779,3586,1771,3585,1765,3584,1759,3582,1756,3580,1753,3579,1752,3578,1751,3577,1752,3576,1753,3575,1752,3573,1752,3572,1753,3571,1754,3570,1754,3569,1757,3569,1758,3567,1760,3566,1760,3565,1760,3565,1760,3564,1761,3564,1762,3564,1765,3564,1767,3564,1769,3565,1769,3565,1771,3566,1771,3566,1772,3567,1772,3567,1771,3568,1770,3569,1772,3569,1771,3570,1774,3570,1775,3570,1778,3570,1779,3571,1780,3572,1779,3572,1779,3572,1779,3573,1777,3574,1773,3575,1773,3575,1772,3577,1770,3577,1771,3577,1775,3578,1776,3578,1778,3578,1779,3578,1781,3579,1777,3580,1770,3580,1767,3581,1768,3582,1778,3583,1807,3583,1857,3584,1922,3584,1981,3584,2019,3585,2022,3585,1989,3586,1932,3587,1873,3588,1828,3588,1802,3589,1792,3589,1792,3590,1795,3591,1796,3592,1797,3592,1801,3593,1801,3594,1803,3594,1804,3595,1806,3595,1805,3596,1806,3597,1807,3597,1810,3597,1813,3598,1815,3598,1817,3599,1820,3600,1823,3601,1827,3602,1834,3602,1839,3602,1845,3603,1850,3604,1857,3605,1864,3605,1872,3606,1880,3607,1887,3608,1894,3608,1899,3608,1903,3608,1906,3606,1907,3605,1908,3603,1908,3602,1909,3600,1906,3599,1903,3597,1898,3596,1892,3594,1881,3593,1873,3591,1863,3589,1852,3588,1843,3585,1837,3584,1828,3583,1825,3581,1822,3579,1818,3578,1816,3577,1816,3575,1815,3574,1814,3573,1813,3571,1813,3569,1813,3569,1815,3568,1816,3567,1817,3565,1819,3565,1819,3564,1821,3563,1822,3563,1823,3563,1823,3563,1823,3562,1822,3563,1822,3563,1822,3563,1825,3563,1826,3561,1826,3561,1826,3561,1827,3561,1826,3562,1829,3564,1831,3565,1833,3567,1834,3566,1835,3567,1836,3568,1836,3568,1836,3568,1834,3570,1833,3570,1830,3571,1828,3572,1827,3572,1826,3573,1825,3573,1826,3574,1827,3575,1828,3575,1824,3577,1821,3578,1818,3578,1821,3579,1835,3579,1870,3579,1923,3580,1990,3581,2046,3582,2077,3582,2071,3583,2030,3583,1968,3584,1910,3585,1869,3585,1847,3586,1841,3587,1843,3587,1845,3588,1845,3589,1848,3590,1850,3590,1851,3591,1853,3592,1854,3591,1855,3592,1856,3593,1857,3593,1859,3593,1861,3594,1863,3595,1865,3596,1868,3597,1871,3597,1873,3597,1876,3598,1880,3599,1884,3600,1890,3600,1895,3601,1901,3602,1910,3602,1915,3603,1921,3604,1928,3604,1934,3604,1939,3604,1946,3603,1949,3602,1951,3601,1954,3600,1953,3598,1950,3596,1949,3595,1944,3593,1938,3591,1930,3589,1921,3587,1913,3585,1902,3583,1891,3581,1884,3580,1879,3578,1872,3577,1870,3575,1868,3574,1866,3572,1865,3570,1865,3569,1864,3567,1864,3566,1865,3565,1865,3565,1865,3563,1865,3562,1867,3562,1868,3561,1868,3559,1871,3559,1872,3558,1873,3557,1872,3557,1873,3557,1874,3558,1874,3558,1875,3558,1877,3558,1879,3558,1879,3558,1880,3559,1880,3559,1880,3559,1879,3560,1877,3560,1878,3560,1879,3561,1881,3561,1882,3561,1886,3562,1887,3562,1887,3563,1887,3564,1884,3564,1880,3565,1877,3566,1871,3567,1867,3567,1865,3568,1865,3569,1864,3569,1865,3569,1865,3570,1863,3571,1859,3572,1853,3572,1848,3573,1848,3573,1858,3573,1882,3574,1923,3574,1983,3575,2044,3575,2085,3576,2093,3576,2068,3577,2012,3577,1947,3578,1895,3579,1863,3579,1850,3580,1848,3580,1851,3581,1852,3581,1853,3582,1854,3582,1855,3583,1856,3583,1858,3584,1859,3584,1860,3584,1862,3585,1863,3586,1864,3587,1866,3587,1868,3588,1870,3588,1874,3589,1876,3589,1880,3590,1884,3589,1888,3590,1891,3591,1896,3592,1900,3593,1906,3594,1914,3595,1921,3595,1928,3595,1934,3595,1939,3595,1944,3595,1949,3594,1953,3592,1957,3591,1957,3589,1955,3587,1948,3582,1939,3578,1929,3574,1918,3570,1906,3567,1898,3567,1892,3565,1888,3564,1884,3562,1882,3560,1882,3559,1880,3557,1879,3556,1878,3555,1878,3554,1879,3553,1880,3551,1882,3550,1883,3549,1883,3548,1884,3547,1885,3547,1884,3547,1884,3546,1885,3546,1885,3546,1885,3546,1887,3545,1887,3545,1888,3546,1887,3546,1886,3546,1885,3546,1885,3547,1885,3547,1885,3547,1885,3547,1885,3548,1884,3549,1884,3549,1887,3550,1889,3551,1890,3551,1892,3552,1892,3552,1892,3552,1891,3553,1890,3553,1888,3554,1884,3554,1880,3556,1876,3556,1873,3557,1872,3558,1872,3558,1871,3559,1872,3559,1872,3559,1869,3560,1863,3561,1856,3561,1854,3562,1863,3563,1885,3564,1928,3565,1990,3566,2048,3566,2085,3566,2093,3565,2066,3566,2010,3566,1948,3566,1901,3566,1870,3567,1858,3568,1857,3569,1860,3570,1860,3571,1860,3572,1858,3572,1858,3573,1859,3574,1860,3574,1860,3574,1861,3575,1860,3576,1859,3576,1859,3577,1859,3577,1858,3578,1859,3579,1860,3579,1861,3580,1862,3581,1865,3581,1868,3582,1870,3583,1874,3585,1879,3585,1883,3586,1888,3587,1893,3587,1897,3588,1901,3588,1906,3588,1910,3588,1914,3587,1914,3585,1915,3584,1916,3583,1913,3581,1909,3579,1906,3578,1901,3576,1892,3574,1884,3572,1876,3570,1865,3569,1854,3567,1845,3565,1837,3564,1828,3562,1823,3561,1818,3558,1814,3557,1810,3555,1808,3553,1806,3551,1804,3551,1803,3549,1804,3548,1805,3547,1804,3546,1805,3545,1806,3544,1807,3542,1808,3541,1807,3541,1808,3541,1807,3540,1807,3540,1807,3540,1806,3539,1807,3539,1806,3539,1808,3540,1808,3540,1808,3540,1807,3541,1807,3541,1805,3542,1804,3542,1806,3543,1807,3543,1808,3544,1810,3545,1812,3546,1812,3546,1814,3547,1819,3547,1820,3547,1822,3548,1824,3548,1824,3548,1821,3549,1819,3549,1817,3549,1816,3550,1817,3550,1819,3551,1821,3551,1822,3551,1825,3552,1825,3553,1824,3554,1819,3555,1817,3555,1818,3555,1827,3556,1850,3556,1895,3556,1955,3557,2020,3558,2064,3558,2078,3558,2056,3559,2003,3559,1941,3559,1892,3561,1860,3561,1848,3561,1849,3562,1852,3562,1855,3562,1858,3563,1861,3564,1862,3564,1863,3565,1865,3565,1869,3566,1871,3566,1874,3567,1877,3568,1877,3568,1878,3569,1880,3569,1883,3569,1886,3569,1891,3570,1896,3571,1901,3572,1903,3573,1907,3574,1913,3574,1918,3575,1923,3575,1933,3575,1942,3575,1948,3576,1957,3576,1963,3576,1966,3576,1968,3574,1972,3573,1972,3571,1975,3569,1975,3568,1977,3567,1976,3564,1974,3563,1967,3561,1959,3558,1950,3556,1940,3554,1930,3552,1924,3550,1916,3548,1911,3546,1906,3544,1902,3543,1899,3541,1896,3540,1895,3538,1891,3537,1892,3536,1892,3534,1893,3532,1892,3530,1894,3528,1895,3527,1896,3526,1896,3526,1899,3525,1899,3525,1899,3524,1900,3524,1900,3523,1900,3522,1901,3522,1901,3522,1900,3522,1901,3523,1902,3523,1903,3523,1904,3524,1904,3524,1903,3525,1901,3525,1901,3526,1900,3526,1901,3527,1902,3527,1903,3528,1905,3529,1908,3529,1909,3529,1910,3530,1913,3530,1914,3531,1916,3531,1918,3532,1918,3533,1916,3534,1915,3533,1911,3533,1908,3533,1908,3534,1908,3534,1908,3535,1912,3536,1915,3537,1915,3537,1915,3537,1912,3538,1907,3538,1906,3538,1914,3539,1935,3539,1975,3540,2033,3540,2095,3541,2139,3542,2151,3542,2128,3543,2077,3544,2016,3545,1967,3545,1936,3546,1925,3546,1925,3547,1928,3547,1930,3547,1932,3547,1934,3548,1936,3549,1937,3549,1940,3550,1942,3550,1947,3551,1950,3552,1953,3552,1956,3553,1959,3553,1960,3554,1963,3555,1965,3555,1967,3556,1968,3557,1969,3557,1972,3558,1977,3558,1982,3559,1989,3560,1997,3560,2003,3560,2009,3561,2015,3561,2021,3561,2027,3561,2031,3560,2034,3559,2037,3558,2039,3556,2040,3555,2040,3554,2040,3552,2037,3550,2033,3548,2027,3546,2020,3544,2011,3542,2002,3540,1993,3538,1984,3536,1976,3534,1969,3532,1962,3530,1957,3528,1954,3526,1951,3525,1948,3523,1948,3521,1947,3520,1946,3519,1946,3517,1947,3516,1946,3515,1945,3514,1947,3514,1946,3514,1947,3513,1949,3512,1951,3512,1951,3511,1953,3510,1952,3509,1950,3509,1950,3509,1949,3509,1948,3510,1948,3510,1949,3510,1949,3511,1947,3511,1948,3512,1949,3512,1948,3513,1948,3513,1948,3514,1948,3514,1948,3515,1949,3515,1951,3516,1953,3516,1956,3516,1956,3516,1955,3517,1954,3517,1954,3518,1952,3519,1951,3520,1951,3520,1950,3521,1948,3522,1947,3522,1945,3522,1944,3523,1945,3523,1945,3524,1944,3525,1946,3526,1945,3526,1943,3527,1940,3528,1940,3528,1945,3529,1962,3529,1995,3530,2050,3530,2113,3530,2162,3531,2180,3532,2165,3532,2117,3533,2053,3535,1998,3535,1964,3536,1949,3537,1948,3537,1952,3537,1954,3537,1954,3537,1954,3538,1954,3538,1955,3539,1956,3540,1957,3540,1960,3540,1960,3541,1960,3542,1962,3543,1965,3544,1967,3544,1969,3545,1971,3546,1972,3547,1972,3548,1974,3549,1977,3548,1981,3550,1987,3551,1993,3551,1999,3552,2006,3553,2012,3553,2018,3551,2025,3550,2032,3548,2036,3547,2037,3544,2037,3543,2035,3542,2030,3541,2025,3539,2019,3538,2008,3536,1999,3535,1990,3532,1979,3530,1970,3528,1962,3526,1954,3524,1949,3522,1946,3521,1943,3519,1942,3518,1941,3517,1940,3516,1939,3514,1940,3513,1940,3512,1940,3511,1941,3509,1940,3508,1938,3507,1938,3506,1938,3505,1937,3505,1938,3505,1939,3505,1939,3505,1938,3505,1939,3505,1938,3505,1938,3505,1937,3505,1938,3505,1938,3506,1938,3507,1938,3508,1938,3508,1938,3509,1938,3510,1938,3511,1938,3511,1938,3512,1937,3512,1935,3512,1935,3513,1933,3514,1933,3514,1933,3515,1935,3516,1935,3517,1939,3518,1941,3519,1940,3519,1942,3520,1941,3521,1938,3521,1934,3521,1931,3522,1928,3523,1926,3523,1926,3524,1927,3525,1930,3525,1930,3526,1930,3526,1925,3527,1921,3527,1917,3528,1919,3528,1933,3529,1963,3530,2015,3531,2079,3532,2132,3532,2157,3533,2150,3533,2106,3534,2043,3534,1986,3535,1945,3535,1926,3536,1923,3536,1927,3537,1930,3537,1935,3538,1935,3538,1936,3539,1937,3539,1939,3540,1937,3542,1938,3542,1939,3542,1941,3543,1942,3544,1947,3544,1949,3545,1950,3546,1952,3546,1953,3547,1955,3547,1959,3548,1965,3548,1968,3549,1976,3550,1983,3551,1990,3551,1997,3552,2004,3553,2011,3553,2016,3554,2021,3554,2025,3554,2030,3553,2031,3552,2032,3551,2033,3549,2034,3547,2033,3546,2032,3544,2029,3543,2024,3542,2017,3540,2006,3538,1994,3536,1984,3534,1976,3532,1967,3530,1962,3528,1956,3526,1949,3524,1942,3523,1939,3522,1935,3521,1936,3519,1937,3518,1938,3517,1938,3515,1940,3514,1941,3512,1941,3511,1942,3511,1943,3510,1943,3510,1944,3510,1943,3510,1944,3509,1944,3510,1943,3510,1943,3510,1944,3510,1944,3510,1943,3510,1943,3510,1942,3509,1942,3510,1940,3511,1940,3511,1940,3512,1939,3512,1937,3512,1937,3513,1937,3514,1936,3514,1936,3515,1935,3516,1935,3516,1934,3517,1935,3518,1936,3518,1938,3518,1941,3519,1943,3520,1944,3520,1947,3520,1947,3521,1945,3522,1943,3523,1940,3523,1937,3524,1936,3525,1938,3526,1938,3526,1940,3527,1939,3527,1940,3528,1937,3529,1935,3529,1931,3530,1928,3531,1927,3532,1934,3532,1953,3533,1991,3534,2047,3534,2102,3535,2141,3535,2150,3536,2127,3536,2075,3537,2018,3537,1971,3538,1944,3539,1934,3539,1936,3540,1942,3541,1945,3541,1949,3541,1952,3543,1951,3543,1951,3544,1951,3545,1950,3546,1950,3546,1951,3546,1951,3547,1952,3548,1955,3549,1956,3549,1959,3550,1963,3551,1968,3551,1968,3551,1971,3552,1973,3553,1976,3554,1978,3555,1984,3556,1990,3557,1997,3558,2002,3559,2009,3559,2017,3559,2022,3560,2026,3559,2030,3559,2033,3558,2032,3557,2032,3556,2032,3555,2032,3555,2029,3553,2025,3551,2021,3550,2014,3548,2003,3545,1992,3544,1981,3542,1971,3541,1961,3539,1954,3538,1948,3537,1944,3536,1939,3534,1935,3533,1931,3531,1928,3530,1925,3529,1923,3528,1924,3527,1924,3526,1925,3525,1928,3524,1930,3524,1931,3524,1930,3523,1928,3522,1927,3522,1923,3522,1920,3522,1922,3522,1920,3522,1919,3522,1919,3522,1917,3523,1915,3523,1912,3523,1908,3524,1904,3524,1901,3525,1895,3526,1892,3526,1892,3527,1890,3528,1889,3528,1889,3529,1890,3530,1887,3530,1886,3530,1882,3532,1879,3533,1877,3533,1876,3533,1876,3534,1875,3535,1874,3535,1869,3536,1863,3538,1859,3539,1856,3540,1853,3541,1852,3543,1851,3543,1850,3544,1849,3545,1848,3545,1847,3546,1844,3546,1841,3547,1842,3548,1851,3548,1871,3549,1907,3550,1961,3551,2014,3551,2046,3552,2050,3553,2023,3554,1968,3554,1909,3555,1864,3555,1837,3556,1830,3557,1831,3558,1835,3559,1839,3559,1840,3560,1841,3560,1840,3561,1841,3561,1841,3562,1843,3563,1844,3564,1845,3566,1848,3567,1849,3567,1850,3568,1853,3569,1856,3570,1858,3571,1861,3571,1864,3572,1867,3572,1869,3573,1873,3574,1878,3574,1884,3575,1887,3575,1893,3576,1898,3577,1903,3577,1907,3578,1911,3578,1914,3578,1916,3578,1917,3577,1920,3576,1922,3576,1924,3575,1924,3574,1924,3573,1920,3573,1915,3571,1907,3570,1898,3568,1888,3566,1878,3565,1867,3563,1859,3562,1852,3561,1843,3559,1836,3558,1833,3557,1829,3556,1825,3555,1825,3554,1825,3553,1825,3552,1826,3551,1827,3550,1829,3550,1830,3549,1830,3549,1831,3548,1831,3548,1830,3547,1829,3547,1829,3547,1828,3547,1828,3547,1831,3547,1832,3547,1832,3548,1833,3548,1834,3549,1833,3550,1833,3551,1834,3552,1834,3553,1833,3554,1835,3555,1834,3555,1834,3555,1835,3556,1836,3557,1835,3557,1837,3558,1837,3559,1838,3559,1839,3559,1842,3560,1843,3561,1847,3562,1850,3563,1853,3563,1857,3564,1859,3564,1861,3565,1863,3565,1865,3566,1866,3567,1866,3568,1870,3569,1871,3570,1872,3571,1874,3572,1878,3572,1880,3573,1882,3574,1881,3575,1883,3576,1885,3576,1895,3576,1921,3577,1963,3577,2021,3578,2079,3579,2118,3580,2125,3581,2102,3581,2052,3582,1996,3583,1954,3584,1933,3584,1926,3585,1931,3586,1940,3586,1948,3586,1953,3587,1959,3588,1965,3588,1969,3588,1976,3589,1981,3590,1988,3590,1994,3591,2001,3592,2007,3593,2013,3593,2020,3595,2027,3595,2033,3596,2039,3597,2045,3598,2052,3598,2060,3598,2069,3599,2076,3599,2087,3600,2097,3601,2107,3601,2116,3602,2127,3603,2136,3604,2145,3604,2154,3604,2161,3604,2167,3603,2173,3603,2177,3602,2180,3601,2183,3600,2183,3599,2182,3598,2179,3596,2174,3595,2168,3594,2161,3592,2154,3590,2148,3589,2144,3587,2142,3586,2142,3585,2142,3584,2143,3582,2143,3581,2143,3580,2143,3579,2145,3577,2146,3576,2149,3574,2152,3574,2157,3573,2162,3573,2169,3572,2175,3572,2179,3571,2183,3570,2186,3570,2187,3569,2188,3569,2191,3569,2193,3570,2196,3570,2201,3571,2205,3571,2209,3572,2211,3572,2214,3572,2214,3573,2213,3573,2214,3574,2216,3574,2217,3575,2218,3575,2222,3576,2222,3576,2223,3577,2224,3577,2226,3578,2228,3579,2232,3579,2236,3580,2240,3581,2243,3581,2247,3582,2247,3582,2248,3582,2247,3582,2246,3583,2245,3584,2246,3584,2248,3585,2249,3586,2253,3586,2254,3586,2255,3587,2254,3587,2251,3587,2246,3589,2245,3590,2249,3591,2265,3591,2299,3592,2354,3592,2419,3593,2472,3593,2497,3594,2490,3595,2446,3595,2385,3595,2329,3596,2291,3596,2273,3597,2273,3597,2277,3598,2283,3599,2286,3600,2288,3600,2288,3601,2287,3601,2287,3602,2289,3602,2292,3603,2297,3604,2302,3604,2306,3605,2309,3605,2312,3606,2312,3607,2314,3607,2318,3608,2320,3609,2321,3610,2324,3610,2326,3610,2329,3610,2333,3610,2340,3611,2347,3612,2354,3613,2360,3614,2368,3614,2374,3614,2379,3614,2382,3614,2385,3614,2385,3613,2384,3612,2383,3611,2381,3609,2377,3608,2372,3607,2366,3605,2357,3604,2349,3603,2339,3601,2330,3599,2322,3597,2316,3596,2309,3593,2304,3592,2300,3590,2298,3589,2294,3587,2292,3586,2291,3584,2290,3582,2288,3581,2287,3579,2288,3578,2288,3578,2288,3576,2290,3575,2291,3573,2290,3572,2290,3571,2291,3570,2289,3570,2289,3569,2291,3569,2290,3568,2289,3568,2289,3568,2289,3568,2288,3569,2289,3569,2289,3569,2290,3569,2290,3569,2290,3569,2291,3569,2292,3570,2291,3570,2290,3571,2290,3571,2290,3571,2289,3571,2289,3571,2290,3571,2288,3572,2287,3573,2287,3573,2287,3573,2286,3574,2288,3574,2288,3574,2287,3574,2286,3574,2284,3575,2281,3575,2277,3575,2272,3576,2269,3576,2269,3576,2267,3576,2268,3577,2269,3577,2269,3578,2267,3578,2261,3578,2257,3578,2254,3579,2258,3579,2279,3580,2319,3580,2376,3581,2437,3580,2480,3581,2492,3582,2468,3583,2414,3583,2351,3584,2300,3584,2265,3584,2250,3584,2246,3584,2247,3585,2246,3585,2248,3585,2248,3586,2247,3587,2246,3586,2245,3587,2243,3587,2242,3587,2241,3587,2240,3586,2240,3586,2240,3586,2241,3586,2243,3587,2245,3588,2246,3590,2248,3591,2251,3592,2254,3593,2258,3594,2262,3595,2267,3595,2273,3595,2277,3594,2278,3595,2280,3593,2281,3593,2277,3592,2274,3590,2272,3589,2270,3588,2266,3586,2261,3585,2254,3583,2246,3580,2234,3578,2222,3576,2211,3574,2201,3573,2189,3571,2181,3569,2171,3567,2162,3564,2154,3562,2148,3560,2141,3558,2139,3556,2138,3555,2136,3553,2134,3551,2134,3550,2131,3549,2130,3548,2126,3546,2123,3545,2122,3545,2120,3544,2118,3543,2118,3542,2116,3541,2113,3539,2111,3539,2108,3539,2106,3538,2105,3538,2103,3538,2101,3537,2099,3537,2096,3538,2093,3539,2091,3539,2089,3539,2088,3539,2087,3539,2085,3540,2083,3539,2083,3539,2080,3540,2080,3540,2077,3540,2075,3541,2072,3542,2070,3542,2068,3542,2067,3542,2065,3542,2061,3542,2056,3542,2054,3543,2054,3543,2053,3543,2055,3543,2055,3543,2056,3544,2054,3544,2052,3544,2050,3545,2046,3545,2043,3546,2038,3547,2036,3548,2033,3548,2033,3548,2032,3550,2030,3550,2028,3550,2027,3550,2023,3551,2016,3551,2009,3551,2007,3551,2011,3551,2034,3551,2076,3552,2138,3553,2200,3553,2242,3554,2250,3555,2222,3556,2165,3556,2100,3556,2049,3556,2017,3556,2006,3557,2005,3557,2006,3558,2004,3559,2004,3560,2001,3560,2000,3561,1998,3561,1998,3562,1998,3562,1997,3563,1995,3563,1995,3564,1995,3564,1996,3565,1996,3566,1997,3566,1997,3567,1996,3567,1996,3568,1998,3568,1999,3569,2001,3569,2006,3570,2009,3570,2013,3571,2018,3571,2024,3571,2027,3572,2030,3571,2033,3571,2034,3570,2036,3570,2037,3568,2036,3567,2035,3566,2033,3565,2026,3563,2020,3561,2014,3559,2006,3557,1997,3554,1986,3551,1975,3549,1963,3547,1952,3545,1942,3542,1933,3541,1928,3538,1922,3536,1918,3534,1915,3532,1913,3529,1910,3527,1908,3525,1907,3523,1906,3522,1905,3521,1903,3519,1901,3518,1898,3516,1896,3515,1896,3514,1895,3513,1892,3512,1891,3511,1891,3511,1889,3510,1887,3510,1886,3510,1884,3509,1882,3509,1880,3509,1880,3508,1879,3508,1879,3508,1876,3508,1875,3508,1874,3509,1874,3510,1872,3510,1872,3511,1870,3511,1866,3511,1864,3512,1859,3512,1855,3512,1851,3513,1850,3513,1848,3514,1847,3515,1849,3516,1848,3516,1847,3517,1845,3518,1844,3518,1841,3518,1839,3518,1837,3518,1836,3518,1836,3519,1835,3519,1835,3520,1835,3520,1836,3521,1836,3521,1837,3522,1836,3522,1836,3522,1833,3523,1831,3523,1827,3524,1825,3525,1823,3526,1821,3526,1820,3527,1819,3527,1816,3528,1814,3529,1811,3530,1804,3530,1799,3530,1800,3530,1809,3531,1833,3531,1879,3532,1941,3533,1999,3534,2036,3535,2040,3535,2008,3535,1950,3536,1888,3536,1841,3537,1813,3538,1803,3539,1802,3540,1802,3540,1802,3541,1801,3542,1801,3543,1799,3543,1802,3544,1802,3545,1802,3545,1803,3545,1805,3546,1804,3547,1805,3547,1805,3548,1806,3549,1806,3549,1807,3550,1808,3551,1809,3552,1809,3552,1811,3553,1814,3553,1817,3555,1822,3555,1827,3556,1832,3557,1838,3558,1846,3558,1852,3558,1857,3559,1861,3558,1865,3557,1865,3555,1865,3554,1866,3552,1863,3550,1860,3548,1855,3546,1848,3544,1841,3542,1833,3540,1823,3538,1812,3536,1802,3532,1791,3530,1782,3528,1775,3525,1770,3522,1764,3521,1759,3518,1755,3516,1753,3514,1751,3512,1750,3510,1751,3508,1750,3506,1748,3504,1746,3503,1745,3500,1743,3499,1742,3498,1741,3497,1740,3495,1739,3495,1740,3494,1742,3493,1742,3493,1743,3493,1744,3492,1743,3492,1741,3492,1740,3492,1739,3493,1740,3493,1740,3493,1739,3493,1739,3494,1739,3494,1736,3495,1734,3495,1735,3496,1733,3496,1733,3496,1733,3497,1735,3497,1736,3498,1736,3498,1736,3499,1736,3500,1734,3500,1732,3501,1732,3501,1733,3502,1730,3503,1730,3503,1730,3504,1729,3504,1729,3505,1730,3506,1731,3506,1731,3506,1733,3507,1734,3507,1735,3507,1735,3508,1738,3508,1737,3509,1734,3510,1733,3511,1731,3511,1727,3513,1725,3514,1725,3514,1725,3515,1726,3516,1727,3516,1726,3516,1722,3517,1718,3518,1712,3518,1707,3520,1708,3520,1720,3521,1748,3521,1799,3522,1862,3522,1917,3523,1946,3523,1942,3523,1902,3523,1842,3524,1786,3524,1744,3525,1722,3526,1716,3527,1716,3527,1717,3528,1720,3529,1722,3529,1721,3531,1722,3531,1721,3532,1720,3532,1721,3533,1724,3533,1724,3534,1726,3535,1729,3536,1729,3536,1731,3537,1733,3538,1735,3538,1737,3539,1738,3540,1740,3540,1745,3541,1748,3542,1752,3543,1759,3543,1765,3544,1770,3544,1777,3545,1784,3546,1789,3547,1794,3547,1799,3546,1802,3545,1803,3544,1801,3542,1801,3540,1797,3539,1795,3537,1789,3535,1784,3532,1776,3530,1767,3528,1756,3526,1744,3523,1733,3521,1724,3519,1712,3516,1703,3514,1697,3512,1690,3510,1684,3508,1682,3506,1678,3505,1674,3502,1673,3500,1670,3499,1669,3496,1668,3495,1667,3494,1666,3493,1666,3491,1665,3490,1667,3489,1667,3488,1667,3488,1668,3488,1667,3488,1666,3488,1664,3487,1663,3487,1659,3487,1657,3487,1654,3487,1652,3488,1650,3488,1648,3489,1648,3489,1647,3490,1646,3491,1646,3491,1644,3492,1643,3493,1640,3493,1640,3493,1636,3494,1636,3494,1636,3495,1634,3496,1631,3497,1630,3497,1628,3498,1625,3499,1624,3500,1623,3501,1621,3501,1620,3502,1619,3503,1619,3504,1617,3505,1617,3506,1614,3507,1615,3508,1614,3508,1615,3509,1615,3510,1616,3510,1616,3511,1615,3511,1615,3513,1614,3513,1614,3514,1611,3515,1607,3516,1602,3517,1598,3518,1595,3518,1592,3519,1589,3519,1588,3520,1586,3521,1583,3521,1578,3522,1573,3523,1570,3524,1572,3524,1587,3525,1619,3525,1672,3526,1734,3526,1783,3528,1805,3528,1795,3529,1750,3530,1685,3531,1628,3531,1588,3533,1567,3533,1561,3534,1563,3535,1566,3536,1567,3537,1568,3538,1569,3539,1569,3540,1569,3540,1570,3541,1570,3541,1571,3542,1571,3542,1571,3543,1571,3544,1572,3545,1572,3547,1574,3548,1575,3549,1577,3550,1580,3550,1583,3551,1585,3551,1589,3552,1591,3553,1595,3553,1599,3554,1604,3555,1609,3556,1615,3556,1619,3557,1625,3557,1631,3557,1633,3557,1637,3557,1639,3557,1637,3556,1636,3554,1633,3552,1627,3550,1621,3548,1613,3546,1599,3544,1588,3542,1577,3541,1565,3539,1552,3537,1544,3535,1535,3533,1528,3531,1521,3530,1519,3528,1515,3526,1512,3525,1509,3524,1507,3523,1505,3521,1502,3520,1502,3518,1500,3517,1500,3515,1499,3514,1500,3514,1499,3513,1501,3512,1500,3512,1500,3511,1499,3511,1498,3510,1494,3510,1492,3510,1492,3511,1491,3511,1493,3511,1494,3512,1494,3513,1493,3513,1493,3513,1491,3514,1490,3515,1490,3515,1489,3516,1488,3517,1486,3517,1485,3518,1482,3520,1481,3520,1479,3520,1477,3521,1476,3522,1476,3522,1476,3523,1477,3524,1478,3525,1479,3526,1479,3527,1478,3528,1478,3528,1479,3529,1480,3529,1481,3530,1483,3531,1485,3531,1486,3532,1487,3533,1487,3534,1485,3535,1482,3536,1481,3537,1480,3537,1478,3538,1479,3538,1480,3539,1480,3539,1480,3540,1479,3540,1475,3541,1470,3542,1469,3543,1472,3543,1489,3544,1526,3545,1582,3545,1644,3546,1695,3546,1715,3547,1701,3547,1653,3548,1591,3549,1535,3550,1499,3551,1482,3552,1480,3553,1483,3554,1489,3554,1493,3554,1496,3555,1496,3555,1497,3556,1498,3557,1500,3557,1502,3558,1505,3559,1507,3560,1508,3561,1511,3562,1515,3563,1519,3563,1523,3564,1527,3564,1529,3565,1531,3565,1536,3567,1540,3567,1545,3568,1550,3570,1558,3571,1566,3572,1574,3573,1583,3573,1592,3574,1601,3574,1609,3574,1615,3574,1620,3574,1625,3573,1628,3572,1629,3571,1630,3569,1628,3568,1625,3566,1620,3565,1613,3563,1606,3561,1596,3559,1586,3557,1578,3555,1571,3553,1563,3552,1560,3550,1557,3548,1553,3546,1551,3544,1548,3542,1548,3539,1547,3537,1547,3536,1548,3535,1550,3534,1551,3533,1555,3532,1556,3531,1556,3530,1557,3529,1559,3528,1559,3527,1560,3526,1563,3526,1564,3525,1564,3525,1568,3525,1569,3525,1570,3525,1572,3525,1574,3525,1574,3525,1576,3526,1576,3527,1577,3527,1578,3527,1579,3527,1580,3528,1581,3528,1581,3529,1581,3529,1582,3530,1583,3531,1584,3532,1585,3533,1588,3533,1589,3534,1589,3534,1590,3535,1588,3536,1588,3537,1590,3537,1593,3537,1597,3538,1601,3539,1604,3539,1605,3540,1605,3540,1603,3541,1602,3541,1600,3542,1599,3542,1598,3543,1599,3543,1599,3544,1602,3545,1603,3546,1604,3547,1605,3548,1605,3548,1602,3549,1597,3549,1594,3550,1596,3550,1607,3551,1636,3552,1685,3553,1750,3554,1809,3554,1846,3555,1847,3556,1814,3556,1758,3557,1700,3558,1657,3558,1633,3559,1626,3559,1627,3560,1629,3560,1631,3561,1633,3562,1634,3562,1634,3564,1637,3564,1639,3565,1640,3566,1642,3567,1644,3567,1644,3568,1645,3568,1648,3569,1650,3570,1652,3570,1655,3571,1659,3571,1663,3572,1667,3573,1671,3574,1676,3575,1682,3576,1689,3577,1697,3578,1706,3579,1715,3580,1722,3580,1731,3581,1737,3581,1742,3581,1744,3580,1749,3580,1751,3578,1754,3577,1755,3575,1755,3574,1752,3572,1745,3570,1736,3568,1726,3566,1717,3564,1707,3563,1698,3561,1690,3559,1682,3557,1674,3556,1670,3554,1667,3552,1664,3550,1662,3548,1662,3547,1659,3545,1658,3544,1657,3543,1658,3541,1658,3540,1660,3539,1661,3538,1663,3538,1663,3537,1666,3536,1669,3536,1671,3535,1673,3534,1674,3533,1675,3533,1675,3532,1674,3532,1673,3533,1674,3534,1674,3533,1674,3534,1675,3534,1675,3535,1674,3535,1673,3535,1674,3536,1674,3536,1676,3536,1677,3537,1678,3538,1681,3539,1682,3539,1683,3540,1684,3540,1686,3540,1685,3541,1686,3541,1685,3542,1684,3543,1684,3544,1685,3544,1686,3545,1689,3545,1690,3546,1691,3547,1693,3548,1693,3549,1693,3549,1694,3549,1693,3550,1691,3550,1691,3550,1694,3550,1693,3551,1696,3552,1700,3553,1702,3554,1701,3555,1701,3556,1696,3557,1693,3557,1696,3557,1711,3558,1741,3558,1795,3559,1861,3560,1915,3561,1946,3561,1942,3562,1903,3562,1842,3563,1787,3564,1746,3565,1726,3566,1720,3566,1723,3567,1726,3567,1727,3569,1729,3570,1730,3571,1733,3572,1735,3573,1737,3573,1740,3573,1742,3573,1743,3573,1747,3575,1749,3575,1752,3576,1754,3577,1758,3578,1762,3578,1765,3579,1769,3580,1773,3580,1777,3582,1780,3582,1786,3583,1793,3584,1800,3585,1816,3584,1829,3584,1841,3583,1850,3582,1859,3581,1859,3582,1859,3581,1859,3580,1858,3579,1853,3577,1847,3575,1839,3572,1829,3571,1822,3570,1813,3568,1804,3567,1796,3566,1790,3563,1785,3562,1781,3560,1778,3559,1778,3556,1776,3555,1774,3554,1774,3553,1775,3552,1775,3551,1776,3550,1777,3548,1778,3547,1778,3546,1777,3545,1778,3545,1778,3544,1780,3544,1783,3544,1788,3543,1789,3543,1791,3543,1792,3543,1791,3543,1789,3543,1790,3543,1791,3542,1793,3543,1794,3544,1794,3545,1792,3545,1792,3546,1790,3547,1790,3547,1792,3547,1794,3548,1794,3549,1793,3549,1791,3550,1790,3550,1789,3551,1791,3552,1794,3553,1797,3554,1801,3554,1804,3554,1805,3555,1807,3555,1808,3556,1810,3556,1810,3556,1811,3557,1810,3557,1810,3559,1807,3560,1807,3560,1807,3561,1806,3561,1807,3562,1806,3562,1805,3563,1804,3564,1802,3565,1797,3565,1798,3566,1809,3567,1831,3567,1876,3568,1938,3568,1997,3569,2035,3570,2043,3570,2014,3570,1960,3571,1901,3572,1855,3573,1828,3573,1820,3574,1820,3575,1824,3575,1826,3576,1825,3577,1826,3578,1825,3578,1825,3578,1828,3579,1830,3580,1832,3582,1835,3583,1837,3583,1839,3584,1841,3584,1842,3585,1844,3586,1846,3587,1847,3588,1849,3589,1851,3589,1854,3590,1857,3591,1863,3592,1870,3594,1876,3594,1883,3595,1891,3596,1898,3596,1905,3597,1913,3597,1919,3598,1922,3597,1926,3597,1930,3596,1929,3595,1930,3593,1928,3592,1923,3590,1916,3589,1909,3588,1900,3587,1889,3586,1878,3584,1866,3582,1855,3581,1845,3579,1838,3577,1831,3575,1827,3574,1824,3571,1821,3571,1816,3569,1816,3568,1814,3567,1812,3566,1810,3565,1811,3564,1810,3563,1809,3561,1809,3561,1810,3559,1811,3559,1810,3558,1811,3557,1810,3557,1810,3557,1809,3557,1809,3557,1810,3557,1811,3557,1811,3556,1810,3557,1808,3557,1806,3558,1806,3558,1805,3558,1806,3559,1805,3559,1805,3560,1803,3561,1803,3561,1802,3562,1801,3563,1801,3563,1801,3563,1800,3563,1801,3563,1801,3564,1801,3565,1801,3565,1802,3565,1802,3566,1800,3567,1800,3567,1800,3568,1799,3569,1797,3570,1796,3570,1797,3571,1796,3572,1797,3572,1796,3573,1798,3573,1798,3573,1794,3574,1788,3574,1786,3574,1784,3575,1791,3576,1816,3577,1860,3577,1919,3578,1980,3578,2020,3579,2027,3579,2000,3580,1945,3581,1882,3581,1832,3582,1803,3582,1791,3583,1791,3584,1795,3585,1798,3585,1798,3585,1801,3585,1802,3585,1805,3586,1804,3586,1806,3587,1806,3587,1809,3587,1810,3588,1813,3590,1815,3590,1820,3591,1822,3592,1825,3592,1828,3592,1832,3593,1833,3593,1838,3594,1842,3594,1848,3595,1853,3595,1861,3596,1868,3597,1877,3598,1886,3599,1897,3600,1903,3600,1909,3599,1915,3599,1919,3598,1920,3597,1921,3596,1924,3595,1922,3593,1919,3592,1916,3590,1912,3589,1904,3587,1896,3586,1888,3584,1881,3583,1872,3581,1864,3579,1856,3577,1851,3575,1846,3574,1842,3572,1841,3571,1841,3569,1841,3567,1842,3565,1842,3564,1845,3562,1845,3561,1845,3559,1845,3558,1848,3558,1849,3556,1851,3556,1855,3556,1856,3555,1856,3555,1857,3555,1857,3555,1857,3554,1859,3554,1862,3554,1863,3554,1865,3554,1867,3554,1869,3554,1870,3554,1871,3554,1872,3554,1871,3555,1871,3555,1871,3556,1870,3556,1870,3556,1871,3557,1873,3557,1873,3558,1873,3558,1875,3558,1878,3559,1878,3559,1881,3559,1885,3560,1886,3561,1886,3561,1885,3561,1883,3562,1880,3563,1877,3563,1875,3564,1876,3563,1876,3563,1877,3564,1878,3564,1879,3564,1880,3565,1881,3566,1877,3567,1874,3567,1870,3568,1871,3568,1881,3568,1910,3568,1958,3569,2024,3569,2084,3569,2122,3570,2127,3570,2096,3571,2038,3572,1977,3573,1930,3573,1901,3573,1892,3574,1892,3574,1896,3574,1898,3575,1900,3576,1901,3577,1902,3577,1902,3578,1903,3578,1905,3579,1906,3579,1907,3579,1909,3579,1909,3580,1912,3580,1914,3581,1916,3582,1918,3582,1923,3582,1924,3583,1928,3583,1932,3584,1935,3585,1941,3586,1948,3586,1955,3587,1961,3587,1970,3588,1976,3588,1984,3589,1989,3588,1995,3588,1998,3587,2001,3586,2002,3584,2003,3582,2004,3581,2002,3579,1998,3577,1994,3575,1988,3574,1977,3572,1966,3570,1956,3568,1943,3566,1932,3564,1925,3562,1918,3560,1913,3558,1909,3556,1906,3554,1903,3552,1901,3551,1900,3549,1899,3548,1897,3546,1898,3544,1898,3543,1898,3542"

        // device position on body
        val position = bodyPositionHandler.getMaxTimeBodyPosition()
        val bodyPosition = parseBodyPosition(bodyPositionInt)
        bodyPositionHandler.resetMap()
        results!!.bodyPosition = bodyPosition

        // username & uuid
        val username = gatewayDbHelper.getUsername()
        results!!.username = username
        results!!.uuid = username

        // location
        val userLocation = locationDbHelper.getLocation()
        results!!.lng = userLocation.longitude.toString()
        results!!.lat = userLocation.latitude.toString()

        // device position from server
        val devicePosition = multiVsDbHelper.getCalibrationBodyPosition()
        results!!.devicePosition = devicePosition

        // test type + filename + timestamp
        val testType = "ECL"
        val fileName = "$testType${results!!.username}_${
            SimpleDateFormat(
                "yyyy-MM-dd_HH-mm-ss",
                Locale.US
            ).format(Calendar.getInstance().time)
        }.txt"
        val timeStamp = (System.currentTimeMillis() / 1000).toString()
        results!!.filename = fileName
        results!!.timeStamp = timeStamp
    }

    fun stopAndClose() {
        viewModelScope.launch {
            stopTest(true)
            delay(250)
            closeDevice()
        }
    }

    init {
        viewModelScope.launch {
            userCountry = gatewayDbHelper.getUserCountrySuspended()
        }
    }

    private val job = Job()
    override val coroutineContext: CoroutineContext
        get() = job + Dispatchers.IO

    fun onDestroy() {
        calibrationResultLiveData = MutableLiveData()
    }
}


class CalibrationResult(val isCalibrating: Boolean) {
    private var status: Boolean? = null
    fun status(): Boolean? {
        return status
    }

    fun setStatus(status: Boolean) {
        this.status = status
    }
}

class CalibrationPostResults(val status: Status, val msg: String)