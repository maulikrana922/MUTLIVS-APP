package com.es.multivs.data.network.retrofit

import com.es.multivs.data.database.entities.CalibrationResults
import com.es.multivs.data.models.ServerResult
import com.es.multivs.data.models.Survey
import com.es.multivs.data.models.SurveyPostAnswer
import com.es.multivs.data.models.VideoModel
import com.es.multivs.data.network.netmodels.*
import com.es.multivs.data.repository.MedicationUploadModel
import com.es.multivs.data.repository.MedicationUploadResponse
import com.es.multivs.data.repository.WeatherResponse
import retrofit2.Response
import retrofit2.http.*

interface Api {

    @GET
    suspend fun getWeather(@Url url: String): Response<WeatherResponse>

    @GET
    suspend fun getUserSchedule(
        @Url url: String,
        @Header("Authorization") AuthHeader: String?
    ): Response<List<UserSchedule>>

    @FormUrlEncoded
    @POST
    suspend fun getUserDevices(
        @Field(value = "mac_id") macAddress: String?,
        @Url url: String?,
        @Header("Authorization") authHeader: String?
    ): Response<UserDevices>

    @GET
    suspend fun getUserDetails(
        @Url url: String?,
        @Header("Authorization") AuthHeader: String
    ): Response<UserDetails>

    @POST
    suspend fun setSensorValue(
        @Url url: String?,
        @Body serializedResults: SerializedResults?,
        @Header("Authorization") authHeader: String?
    ): Response<MeasurementsPostAnswer>

    @POST
    suspend fun uploadUserLocation(
        @Url url: String?,
        @Body serializedUserLocation: SerializedUserLocation?,
        @Header("Authorization") authHeader: String?
    ): Response<PeriodicInfoAnswer>

    @POST
    suspend fun saveFileRecord(
        @Url url: String?,
        @Body serializedResults: SerializedPatchResults?,
        @Header("Authorization") authHeader: String?
    ): Response<SerializedPatchResults>

    @GET
    suspend fun getUserLatestRead(
        @Url url: String?,
        @Header("Authorization") authHeader: String?
    ): Response<UserLatestRead>

    @GET
    suspend fun getUserMedications(
        @Url url: String?,
        @Header("Authorization") authHeader: String?
    ): Response<List<MedicationSchedule>>

    @POST
    suspend fun uploadCalibration(
        @Url url: String,
        @Body calibrationResults: CalibrationResults,
        @Header("Authorization") authHeader: String?
    ): Response<CalibrationPostAnswer>

    @POST
    suspend fun uploadMedications(
        @Url url: String,
        @Body medications: MedicationUploadModel,
        @Header("Authorization") authHeader: String?
    ): Response<MedicationUploadResponse>

    @GET
    suspend fun getUserSurveys(
        @Url url: String,
        @Header("Authorization") authHeader: String?
    ): Response<List<Survey>>

    @POST
    suspend fun uploadSurvey(
        @Url url: String,
        @Body surveyPostAnswer: SurveyPostAnswer,
        @Header("Authorization") authHeader: String?
    ): Response<ServerResult>

    @GET
    suspend fun getReportsMeasurements(
        @Url url: String,
        @Header("Authorization") authHeader: String?
    ): Response<ReportsMeasurements>

    @GET
    suspend fun getReportsMedication(
        @Url url: String,
        @Header("Authorization") authHeader: String?
    ): Response<ReportsMedication>

    @GET
    suspend fun getUserURL(
        @Url url: String?,
        @Header("Authorization") AuthHeader: String
    ): Response<VideoModel>
}