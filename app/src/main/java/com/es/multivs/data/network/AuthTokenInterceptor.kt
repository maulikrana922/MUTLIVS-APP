package com.es.multivs.data.network

import android.util.Log
import okhttp3.Interceptor
import okhttp3.Request
import okhttp3.Response
import java.io.IOException

class AuthTokenInterceptor : Interceptor {

    private val TAG_THIS = AuthTokenInterceptor::class.java.simpleName

    companion object {
        fun getInstance() = AuthTokenInterceptor()
    }

    @Throws(IOException::class)
    override fun intercept(chain: Interceptor.Chain): Response {
        var request = chain.request()
        val builder = request.newBuilder()
        val token: String = "Bearer " + TokenKeeper.instance?.token

        token.let {
            request = setAuthHeader(builder, it).build()
        }
        val response = chain.proceed(request)
        Log.e(TAG_THIS, "Request responses code: " + response.code() + " " + request.url())
        return response
    }

    private fun setAuthHeader(builder: Request.Builder, token: String): Request.Builder {
        return builder.header("Authorization", token)
    }

}
