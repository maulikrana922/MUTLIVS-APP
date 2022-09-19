package com.es.multivs.data.models

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.google.gson.annotations.SerializedName
import java.io.Serializable

/**
 * Created by Marko on 1/9/2022.
 * Etrog Systems LTD.
 */
@Entity
class SurveyPostAnswer(
    @SerializedName("survey_id") val surveyID: String,
    @SerializedName("survey_Name") val surveyName: String,
    @SerializedName("iosBatteryLevel") val iosBatteryLevel: String,
    @SerializedName("lat") val lat: String,
    @SerializedName("lng") val lng: String,
    @SerializedName("timestamp") val timestamp: String,
    @SerializedName("schedule_time") val scheduleTimestamp: String,
    @SerializedName("username") val username: String,
    @SerializedName("question_list") val questionList: ArrayList<AnsweredQuestion>
) : Serializable {

    @PrimaryKey(autoGenerate = true)
    var id: Int = 0

}



data class AnsweredQuestion(
    @SerializedName("answer") val answer: String?,
    @SerializedName("is_answered") val isAnswered: Boolean,
    @SerializedName("question_id") val questionID: Int,
    @SerializedName("question_text") val questionText: String,
    @SerializedName("question_type") val questionType: String
) : Serializable {

    override fun equals(other: Any?): Boolean {
        if (other is AnsweredQuestion) {
            if (this.questionID == other.questionID && this.questionText == other.questionText) {
                return true
            }
        }
        return false
    }
}

//data class NotAnsweredQuestion(
//    @SerializedName("is_answered") val isAnswered: Boolean,
//    @SerializedName("question_id") val questionID: Int,
//    @SerializedName("question_text") val questionText: String,
//    @SerializedName("question_type") val questionType: String
//) : PostQuestion(), Serializable



