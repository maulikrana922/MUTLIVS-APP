package com.es.multivs.data.models

import androidx.room.ColumnInfo
import androidx.room.Ignore
import androidx.room.PrimaryKey
import com.google.gson.annotations.SerializedName
import java.io.Serializable

/**
 * Created by Marko on 12/28/2021.
 * Etrog Systems LTD.
 */

data class Survey(
    @PrimaryKey(autoGenerate = true)
    var id: Int,

    @SerializedName("survey_id")
    val surveyID: String,

    @SerializedName("survey_Name")
    val surveyName: String,

    @SerializedName("survey_URL")
    val surveyURL: String = "",

    val timeOfSurvey1: String,

    val timeOfSurvey2: String,

    val timeOfSurvey3: String,

    val timeOfSurvey4: String,

    @SerializedName("days_of_week")
//    @ColumnInfo(name = "days_of_week")
    val daysOfWeek: ArrayList<String>,

    @SerializedName("question_list")
//    @ColumnInfo(name = "question_list")
    val questionList: ArrayList<Question>
) : Serializable {
    override fun toString(): String {
        return """
            surveyURL: $surveyURL
            timeOfSurvey1: $timeOfSurvey1
            timeOfSurvey2: $timeOfSurvey2
            timeOfSurvey3: $timeOfSurvey3
            timeOfSurvey4: $timeOfSurvey4
            daysOfWeek: $daysOfWeek
            questionList: $questionList
        """.trimIndent()
    }
}

data class Question(
    @SerializedName("required")
    @ColumnInfo(name = "required")
    val required: String,

    @SerializedName("question_id")
    @ColumnInfo(name = "question_id")
    val qID: String,

    @SerializedName("question_text")
    @ColumnInfo(name = "question_text")
    val questionText: String,

    @SerializedName("question_type")
    @ColumnInfo(name = "question_type")
    val questionType: String,

    @SerializedName("answers_text")
    @ColumnInfo(name = "answers_text")
    val answerText: ArrayList<String>?,

    @Ignore
    var isAnswered: Boolean = false,

    @Ignore
    var answer: String?

) : Serializable {


    @Ignore
    override fun toString(): String {
        return """
            required: $required
            questionID: $qID
            questionText: $questionText
            questionType: $questionType
            isAnswered: $isAnswered
            user answer: $answer
        """.trimIndent()
    }
}