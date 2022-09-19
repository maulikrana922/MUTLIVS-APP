package com.es.multivs.data.models

/**
 * Created by Marko on 12/28/2021.
 * Etrog Systems LTD.
 */
data class ServerResult(val status: Boolean, val message: String = "")

sealed class SurveyResponse {
    data class ArrayResponse(val response: List<Survey>) : SurveyResponse()
    data class ObjectResponse(val response: ServerResult) : SurveyResponse()
}