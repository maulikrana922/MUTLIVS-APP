package com.es.multivs.data.database.entities

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Ignore
import androidx.room.PrimaryKey
import com.es.multivs.data.models.Question
import com.es.multivs.data.models.ScheduleItem
import java.io.Serializable

/**
 * Created by Marko on 12/29/2021.
 * Etrog Systems LTD.
 */

/**
 * @param timestamp String representation of the survey timestamp in 24h format
 * @param isTaskDone Indicator for performing the task
 * @param isActive Indicator for active schedule
 * @param surveyName Name given by the server
 * @param questionList ArrayList containing [Question] objects
 * @param surveyID ID given by the server
 * @param daysOfWeek Array of days on which the survey should be presented
 */
@Entity
data class SurveyScheduleItem(
    @ColumnInfo(name = "time_stamp") var timestamp: String,
    @ColumnInfo(name = "is_task_done") var isTaskDone: Boolean,
    @ColumnInfo(name = "is_active") var isActive: Boolean,
    @ColumnInfo(name = "survey_name") val surveyName: String,
    @ColumnInfo(name = "question_list") val questionList: ArrayList<Question>,
    @ColumnInfo(name = "survey_id") val surveyID: String,
    @ColumnInfo(name = "days_of_week") val daysOfWeek: ArrayList<String>,
) : ScheduleItem, Serializable {

    @PrimaryKey(autoGenerate = true)
    var id: Int = 0

    @Ignore
    override fun showContent(): String {
        return surveyName
    }

    @Ignore
    override fun getItemType(): String {
        return "survey_item"
    }

    @Ignore
    override fun getTitle(): String {
        return "Survey"
    }

    @Ignore
    override fun getTime(): String {
        return timestamp
    }

    @Ignore
    override fun isItemActive(): Boolean {
        return isActive
    }

    @Ignore
    override fun taskDone(): Boolean {
        return isTaskDone
    }

    override fun equals(other: Any?): Boolean {
        var isSameTimeStamp = false
        var isSameName = false
        var isSameSurveyID = false

        if (other is SurveyScheduleItem) {
            isSameTimeStamp = (this.timestamp == other.timestamp)
            isSameName = (this.surveyName == other.surveyName)
            isSameSurveyID = (this.surveyID == other.surveyID)
        }

        return isSameTimeStamp && isSameName && isSameSurveyID
    }

    override fun hashCode(): Int {
        var result = timestamp.hashCode()
        result = 31 * result + isTaskDone.hashCode()
        result = 31 * result + isActive.hashCode()
        result = 31 * result + surveyName.hashCode()
        result = 31 * result + questionList.hashCode()
        result = 31 * result + surveyID.hashCode()
        result = 31 * result + daysOfWeek.hashCode()
        result = 31 * result + id
        return result
    }
}