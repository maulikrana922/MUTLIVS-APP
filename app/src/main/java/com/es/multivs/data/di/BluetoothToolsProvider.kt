package com.es.multivs.data.di

import android.content.Context
import com.es.multivs.data.utils.BluetoothTools
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/**
 * Created by Marko on 11/15/2021.
 * Etrog Systems LTD.
 */
@Module
@InstallIn(SingletonComponent::class)
class BluetoothToolsProvider {

    @Provides
    @Singleton
    fun provideBluetoothTools(@ApplicationContext context: Context): BluetoothTools {
        return BluetoothTools(context)
    }
}