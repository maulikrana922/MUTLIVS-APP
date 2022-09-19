package com.es.multivs.data.models

/**
 * Created by Dinesh on 7/Sep/2022.
 * Etrog Systems LTD.
 */
data class LoginDto(
    val username: String, val macAddress: String = "", val uuid: String,
    val password: String, val appVersion: String
)