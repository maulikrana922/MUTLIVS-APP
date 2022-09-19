package com.es.multivs.data.database.survey

import androidx.room.*
import com.es.multivs.data.database.entities.SurveyScheduleItem
import com.es.multivs.data.models.SurveyPostAnswer

/**
 * Created by Marko on 12/28/2021.
 * Etrog Systems LTD.
 */

@Dao
interface SurveyDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun saveSurveys(surveyList: List<SurveyScheduleItem>)

    @Query("DELETE FROM surveyscheduleitem")
    suspend fun deleteSurveys()

    @Transaction
    suspend fun deleteAndInsertSurveys(surveyList: List<SurveyScheduleItem>) {
        deleteSurveys()
        saveSurveys(surveyList)
    }

    @Query("SELECT * FROM surveyscheduleitem")
    suspend fun getSurveys(): List<SurveyScheduleItem>?

    @Query("UPDATE surveyscheduleitem SET is_task_done=1 WHERE time_stamp=:timestamp")
    suspend fun updateTaskDone(timestamp: String)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun saveSurveyPost(surveyPost: SurveyPostAnswer)

    @Query("SELECT * FROM surveypostanswer")
    suspend fun getLatestSurveyPost(): SurveyPostAnswer

    @Query("DELETE FROM surveypostanswer")
    suspend fun deleteLatestSurveyPost()
}