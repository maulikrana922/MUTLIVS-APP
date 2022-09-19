package com.es.multivs.presentation.view.viewmodels

import android.app.Application
import androidx.lifecycle.*
import androidx.work.*
import com.es.multivs.data.database.entities.SurveyScheduleItem
import com.es.multivs.data.database.gateway.GatewayDbHelper
import com.es.multivs.data.database.location.LocationDbHelper
import com.es.multivs.data.database.survey.SurveyDbHelper
import com.es.multivs.data.models.Question
import com.es.multivs.data.models.SurveyPostAnswer
import com.es.multivs.data.models.SurveyPostResponse
import com.es.multivs.data.repository.SurveyRepository
import com.es.multivs.data.utils.*
import com.es.multivs.data.work.SurveyWorker
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

/**
 * Created by Marko on 12/30/2021.
 * Etrog Systems LTD.
 */


@HiltViewModel
class SurveyViewModel @Inject constructor(
    application: Application,
    private val surveyDbHelper: SurveyDbHelper,
    private val gatewayDatabaseHelper: GatewayDbHelper,
    private val locationDbHelper: LocationDbHelper,
    private val repository: SurveyRepository,
    private val scheduleUpdates: ScheduleUpdatesManager
) : AndroidViewModel(application) {

    private val _surveyUpload = MutableLiveData<SurveyPostResponse>()

    val surveyUpload: LiveData<SurveyPostResponse> = _surveyUpload
    private val answers = hashMapOf<String, Question>()
    private var currentSurvey: SurveyScheduleItem? = null


    suspend fun getSurveyFromDatabaseByDay(): SurveyScheduleItem? {

        val list = surveyDbHelper.getLastSurveyItems()

        val timestamp = ScheduleUtils.findActiveSurvey(list)
        val today = AppUtils.getCurrentDayOfWeek()

        currentSurvey = list.asSequence()
            .filter { today in it.daysOfWeek }
            .firstOrNull { it.timestamp == timestamp }

        return currentSurvey
    }

    suspend fun postAnswers() {

        val answersList = ArrayList(answers.values)

        if (currentSurvey == null) return

        val survey = currentSurvey!!

        val listToUpload = SurveyUtils.populatePostList(answersList, survey)

        val location = locationDbHelper.getLocation()

        val surveyPost = SurveyPostAnswer(
            survey.surveyID,
            survey.surveyName,
            AppUtils.getBatteryPercentage(getApplication()).toString(),
            location.latitude.toString(),
            location.longitude.toString(),
            (System.currentTimeMillis() / 1000).toString(),
            ScheduleUtils.parseScheduleTimeToEpochMillis(currentSurvey),
            gatewayDatabaseHelper.getUsername(),
            listToUpload
        )

        if (AppUtils.isInternetAvailable(getApplication())) {

            val isUploaded = repository.postSurvey(surveyPost)

            if (isUploaded) {
                scheduleUpdates.surveyComplete(survey.timestamp)
            }

            _surveyUpload.postValue(SurveyPostResponse(isUploaded))
        } else {

            surveyDbHelper.saveSurveyPost(surveyPost)

            val constraints =
                Constraints.Builder().setRequiredNetworkType(NetworkType.CONNECTED).build()


            val data = workDataOf(
                SurveyWorker.SURVEY_TIME to survey.timestamp
            )

            val request = OneTimeWorkRequestBuilder<SurveyWorker>()
                .setInputData(data)
                .setConstraints(constraints).build()

            val manager = WorkManager.getInstance(getApplication())
            manager.enqueue(request)


            _surveyUpload.postValue(
                SurveyPostResponse(
                    false,
                    "No internet connection. Answers will be posted at a later time."
                )
            )
        }
    }

    fun handleAnswer(question: Question) {
        answers[question.qID] = question
    }

    fun clearAnswers() = answers.clear()

    fun getAnsweredQuestion(id: String): Question? = answers[id]

    fun removeAnswer(q: Question): Question? {
        val key = answers.asSequence().find { q.qID == it.value.qID }?.key

        key?.let {
            return answers.remove(it)
        }

        return null
    }
}