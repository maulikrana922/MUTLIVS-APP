package com.es.multivs.data.network.retrofit

import com.es.multivs.data.database.entities.CalibrationResults
import com.es.multivs.data.models.*
import com.es.multivs.data.network.GenericResponse
import com.es.multivs.data.network.netmodels.*
import com.es.multivs.data.repository.MedicationUploadModel
import com.es.multivs.data.repository.MedicationUploadResponse
import com.es.multivs.data.repository.WeatherResponse
import retrofit2.Response
import retrofit2.http.*
import java.lang.reflect.Type

interface Apis {

    @POST("saveCalibrationRecord")
    suspend fun uploadCalibrationData(
        @Body calibrationResults: CalibrationResults
    ): Response<GenericResponse<CalibrationResponse>>

    @POST("member-login")
    suspend fun login(
        @Body loginDto: LoginDto
    ): Response<GenericResponse<Any>>

}