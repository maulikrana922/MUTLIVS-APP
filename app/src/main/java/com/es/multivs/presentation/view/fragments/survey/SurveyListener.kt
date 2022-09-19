package com.es.multivs.presentation.view.fragments.survey

import com.es.multivs.data.models.Question

/**
 * Created by Marko on 1/10/2022.
 * Etrog Systems LTD.
 */
interface SurveyListener {
    fun handleAnswer(answeredQuestion: Question)
}