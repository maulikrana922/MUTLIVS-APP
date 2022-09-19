package com.es.multivs.data.network.retrofit

import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import javax.inject.Singleton


@Module
@InstallIn(SingletonComponent::class)
class ApiUtil {

    @Singleton
    @Provides
    fun getRetrofitAPI(): Api {

        val baseURL = "http://localhost/"
        val retrofit =
            Retrofit.Builder().baseUrl(baseURL).addConverterFactory(GsonConverterFactory.create())
                .build()

        return retrofit.create(Api::class.java)
    }
}
