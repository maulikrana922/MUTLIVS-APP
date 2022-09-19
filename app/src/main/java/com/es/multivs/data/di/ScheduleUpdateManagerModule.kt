package com.es.multivs.data.di

import com.es.multivs.data.utils.ScheduleUpdatesManager
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/**
 * Created by Marko on 12/25/2021.
 * Etrog Systems LTD.
 */
@Module
@InstallIn(SingletonComponent::class)
class ScheduleUpdateManagerModule {

    @Singleton
    @Provides
    fun provideScheduleUpdateState(): ScheduleUpdatesManager {
        return ScheduleUpdatesManager()
    }
}