package com.es.multivs.data.repository

import android.widget.Toast
import com.es.multivs.data.models.Survey
import com.es.multivs.data.database.gateway.GatewayDbHelper
import com.es.multivs.data.models.SurveyPostAnswer
import com.es.multivs.data.network.TokenKeeper
import com.es.multivs.data.network.retrofit.Api
import com.es.multivs.data.utils.AppUtils
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeout
import java.lang.Exception
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Created by Marko on 12/28/2021.
 * Etrog Systems LTD.
 */
@Singleton
class SurveyRepository @Inject constructor(
    private val gatewayDbHelper: GatewayDbHelper,
    private val api: Api,
) {
    companion object {
        private const val GET_SURVEYS_PATH = "getUserSurveys/"

    }

    suspend fun fetchSurveys(): List<Survey>? {

        val username = gatewayDbHelper.getUsername()
        val authHeader = "Bearer " + TokenKeeper.instance?.token

        val baseURL = gatewayDbHelper.getBaseURL()
        val url = "${baseURL}${GET_SURVEYS_PATH}${username}"

        try {

            val response = api.getUserSurveys(url, authHeader)
            if (response.isSuccessful) {

                return response.body()
            }
        } catch (e: Exception) {
        }

        return null
    }

    suspend fun postSurvey(surveyPostAnswer: SurveyPostAnswer): Boolean {

        val authHeader = "Bearer " + TokenKeeper.instance?.token
        val baseURL = gatewayDbHelper.getBaseURL()
        val url = "${baseURL}save-survey"

        val result: Boolean = withContext(Dispatchers.IO) {
            try {
                withTimeout(10000) {
                    val response = api.uploadSurvey(url, surveyPostAnswer, authHeader)

                    if (response.isSuccessful) {
                        val result = response.body()
                        result?.status ?: false
                    } else {
                        Toast.makeText(AppUtils.application, response.message(), Toast.LENGTH_SHORT).show()
                        false
                    }
                }
            } catch (e: Exception) {
                false
            }
        }

        return result
    }
}