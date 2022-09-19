package com.es.multivs.data.network.retrofit

import com.es.multivs.data.database.entities.CalibrationResults
import com.es.multivs.data.models.LoginDto
import javax.inject.Inject


class CoreApiImp @Inject constructor( private val api: Apis) {
    suspend fun uploadCalibrationData(results: CalibrationResults) = api.uploadCalibrationData(results)
    suspend fun login(results: LoginDto) = api.login(results)

}