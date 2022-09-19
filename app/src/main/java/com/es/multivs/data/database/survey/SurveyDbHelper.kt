package com.es.multivs.data.database.survey

import com.es.multivs.data.database.entities.SurveyScheduleItem
import com.es.multivs.data.models.SurveyPostAnswer
import javax.inject.Inject

/**
 * Created by Marko on 12/28/2021.
 * Etrog Systems LTD.
 */

class SurveyDbHelper @Inject constructor(private val surveyDao: SurveyDao) {

    suspend fun deleteAndSaveSurveys(surveyList: List<SurveyScheduleItem>){
        surveyDao.deleteAndInsertSurveys(surveyList)
    }

    suspend fun getLastSurveyItems(): List<SurveyScheduleItem> {
        return surveyDao.getSurveys() ?: listOf()
    }

    suspend fun updateTaskDone(timestamp: String) {
        surveyDao.updateTaskDone(timestamp)
    }

    suspend fun saveSurveyPost(surveyPost: SurveyPostAnswer) {
        surveyDao.saveSurveyPost(surveyPost)
    }

    suspend fun getLatestSurveyPost(): SurveyPostAnswer {
        val survey =  surveyDao.getLatestSurveyPost()
        surveyDao.deleteLatestSurveyPost()
        return survey
    }


}