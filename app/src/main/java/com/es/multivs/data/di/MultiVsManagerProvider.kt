package com.es.multivs.data.di

import android.content.Context
import com.es.multivs.data.bledevices.multivs.MultiVsManager
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/**
 * Created by Marko on 11/18/2021.
 * Etrog Systems LTD.
 */
@Module
@InstallIn(SingletonComponent::class)
class MultiVsManagerProvider {

    @Provides
    @Singleton
    fun provideMultiVsManager(@ApplicationContext context: Context): MultiVsManager {
        return MultiVsManager(context)
    }
}