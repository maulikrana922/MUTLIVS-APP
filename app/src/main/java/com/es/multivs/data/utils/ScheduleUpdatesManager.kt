package com.es.multivs.data.utils

import javax.inject.Singleton

/**
 * Created by Marko on 12/25/2021.
 * Etrog Systems LTD.
 */

/**
 * Avoid creating an instance of [ScheduleUpdatesManager] by your own.
 * Instances of this class should best be injected by Dagger-Hilt in a constructor or in a field.
 *
 * This class handles the schedule update state during the lifecycle of the application.
 * You may set update states for types: calibrations, medications, measurements and surveys.
 *
 * Current configuration will also update the [fromServer] field on each update,
 * meaning an update from the server is needed.
 *
 * Once done with updates, make sure to use the [clear] method to reset all states of the [ScheduleUpdatesManager].
 */
@Singleton
class ScheduleUpdatesManager {

    var fromServer = UpdatesState.RequireServerUpdates(require = false, ignoreDatabase = false)
        @Synchronized get
        @Synchronized private set

    var calibration = UpdatesState.CalibrationComplete(false)
        @Synchronized get
        @Synchronized private set

    var medication = UpdatesState.MedicationComplete(false, "")
        @Synchronized get
        @Synchronized private set

    var measurements = UpdatesState.MeasurementComplete(false, "")
        @Synchronized get
        @Synchronized private set

    var survey = UpdatesState.SurveyComplete(false, "")
        @Synchronized get
        @Synchronized private set

    fun calibrationComplete() {
        calibration = UpdatesState.CalibrationComplete(true)
        requireServer()
    }

    fun medicationsComplete(timestamp: String) {
        medication = UpdatesState.MedicationComplete(true, timestamp)
        requireServer()
    }

    fun measurementsComplete(timestamp: String) {
        measurements = UpdatesState.MeasurementComplete(true, timestamp)
        requireServer()
    }

    fun surveyComplete(timestamp: String) {
        survey = UpdatesState.SurveyComplete(true, timestamp)
        requireServer()
    }

    /**
     * Use this method to indicate whether the app needs to fetch updates from server or not.
     * @param ignoreDatabase False to indicate that the app needs to combine the results from
     * the server with the existing entries in the database.
     * True to ignore existing database entries and overwrite them.
     */
    fun requireServer(ignoreDatabase: Boolean = false) {
        fromServer = if (ignoreDatabase) {
            UpdatesState.RequireServerUpdates(require = true, ignoreDatabase = true)
        } else {
            UpdatesState.RequireServerUpdates(require = true, ignoreDatabase = false)
        }

    }

    /**
     * Use this method to reset all update states back to their default.
     */
    fun clear() {
        fromServer = UpdatesState.RequireServerUpdates(require = false, ignoreDatabase = false)
        calibration = UpdatesState.CalibrationComplete(false)
        medication = UpdatesState.MedicationComplete(false, "")
        measurements = UpdatesState.MeasurementComplete(false, "")
        survey = UpdatesState.SurveyComplete(false, "")
    }
}

sealed class UpdatesState {

    /**
     * @param require boolean that represents whether an update should be fetched from the server or not.
     * @param ignoreDatabase if true, than all existing schedule data will be ignored while fetching schedules, and overridden.
     */
    data class RequireServerUpdates(val require: Boolean, val ignoreDatabase: Boolean = false) :
        UpdatesState()

    /**
     * @param complete whether the calibration task has been completed or not.
     */
    data class CalibrationComplete(val complete: Boolean) : UpdatesState()

    /**
     * @param complete whether the measurement task has been completed or not.
     */
    data class MeasurementComplete(val complete: Boolean, val timestamp: String) : UpdatesState()

    /**
     * @param complete whether the medication task has been completed or not.
     */
    data class MedicationComplete(val complete: Boolean, val timestamp: String) : UpdatesState()

    /**
     * @param complete whether the survey task has been completed or not.
     */
    data class SurveyComplete(val complete: Boolean, val timestamp: String) : UpdatesState()
}