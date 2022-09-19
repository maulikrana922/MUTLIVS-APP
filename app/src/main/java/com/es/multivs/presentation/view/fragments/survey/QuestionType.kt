package com.es.multivs.presentation.view.fragments.survey

/**
 * Created by Marko on 1/26/2022.
 * Etrog Systems LTD.
 */
enum class QuestionType {

    TYPE_YES_NO {
        override fun toString(): String {
            return "Yes/ No"
        }
    },
    TYPE_RATING {
        override fun toString(): String {
            return "Rating"
        }
    },
    TYPE_MULTIPLE {
        override fun toString(): String {
            return "Multiple Answers"
        }
    },
    TYPE_FREE_TEXT {
        override fun toString(): String {
            return "Free Text"
        }
    },
    TYPE_FREE_NUMBER {
        override fun toString(): String {
            return "Free Number"
        }
    }
}
