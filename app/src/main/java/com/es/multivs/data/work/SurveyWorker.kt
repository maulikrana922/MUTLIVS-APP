package com.es.multivs.data.work

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import androidx.work.workDataOf
import com.es.multivs.data.database.survey.SurveyDbHelper
import com.es.multivs.data.repository.SurveyRepository
import com.es.multivs.data.utils.ScheduleUpdatesManager
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject

/**
 * Created by Marko on 1/12/2022.
 * Etrog Systems LTD.
 */
@HiltWorker
class SurveyWorker @AssistedInject constructor(
    @Assisted context: Context,
    @Assisted workerParams: WorkerParameters,
    private val repository: SurveyRepository,
    private val surveyDbHelper: SurveyDbHelper,
    private val updates: ScheduleUpdatesManager
) : CoroutineWorker(context, workerParams) {

    companion object {

        const val RESULT = "WeatherWorker"
        const val SURVEY_TIME = "survey_timestamp"
    }


    override suspend fun doWork(): Result {

        val timestamp = inputData.getString(SURVEY_TIME)

        val survey = surveyDbHelper.getLatestSurveyPost()

        val isUploaded = repository.postSurvey(survey)

        if (isUploaded) {
            updates.surveyComplete(timestamp ?: "")
            return Result.success(workDataOf(RESULT to true))
        }

        return Result.failure(workDataOf(RESULT to false))
    }


}

