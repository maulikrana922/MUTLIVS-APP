package com.es.multivs.data.utils

import android.widget.RadioButton
import android.widget.RadioGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager
import androidx.fragment.app.FragmentTransaction
import com.es.multivs.R
import com.es.multivs.data.database.entities.SurveyScheduleItem
import com.es.multivs.data.models.AnsweredQuestion
import com.es.multivs.data.models.Question
import com.es.multivs.presentation.view.fragments.survey.*

/**
 * Created by Marko on 1/10/2022.
 * Etrog Systems LTD.
 */
class SurveyUtils {

    companion object {

        fun surveyFragmentFactory(question: Question, surveyName: String): Fragment {
            return when (question.questionType) {
                QuestionType.TYPE_YES_NO.toString() -> {
                    YesNoFragment.newInstance(question, surveyName)
                }
                QuestionType.TYPE_RATING.toString() -> {
                    RatingFragment.newInstance(question, surveyName)
                }
                QuestionType.TYPE_MULTIPLE.toString() -> {
                    MultipleFragment.newInstance(question, surveyName)
                }
                QuestionType.TYPE_FREE_TEXT.toString() -> {
                    FreeTextFragment.newInstance(question, surveyName)
                }
                QuestionType.TYPE_FREE_NUMBER.toString() -> {
                    FreeNumberFragment.newInstance(question, surveyName)
                }
                else -> EmptyQuestionFragment()
            }
        }

        fun getSurveyTransaction(childFragmentManager: FragmentManager): FragmentTransaction {
            return childFragmentManager.beginTransaction().setCustomAnimations(
                R.anim.fade_in,
                R.anim.fade_out,
                R.anim.enter_from_left,
                R.anim.exit_to_right
            )
        }

        fun populatePostList(
            answersList: ArrayList<Question>,
            survey: SurveyScheduleItem
        ): ArrayList<AnsweredQuestion> {
            val listToUpload = ArrayList<AnsweredQuestion>()

            answersList.forEach {
                listToUpload.add(
                    AnsweredQuestion(
                        it.answer,
                        it.isAnswered,
                        it.qID.toInt(),
                        it.questionText,
                        it.questionType
                    )
                )
            }

            val difference = survey.questionList.minus(answersList.toSet())
            difference.forEach {
                listToUpload.add(
                    AnsweredQuestion(
                        null,
                        it.isAnswered,
                        it.qID.toInt(),
                        it.questionText,
                        it.questionType
                    )
                )
            }


            return listToUpload
        }
    }
}

/**
 * @return ArrayList of [RadioButton]
 */
fun RadioGroup.getButtons(): List<RadioButton> {

    val radioButtons = ArrayList<RadioButton>()

    for (i in 0 until childCount) {

        val btn = getChildAt(i)

        if (btn is RadioButton) {
            radioButtons.add(btn)
        }
    }

    return radioButtons
}