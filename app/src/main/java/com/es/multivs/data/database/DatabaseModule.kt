package com.es.multivs.data.database

import android.content.Context
import androidx.room.Room
import com.es.multivs.data.database.calibrations.CalibrationDao
import com.es.multivs.data.database.gateway.GatewayDao
import com.es.multivs.data.database.location.LocationDao
import com.es.multivs.data.database.measurements.MeasurementDao
import com.es.multivs.data.database.medications.MedicationDao
import com.es.multivs.data.database.multivs.MultiVSDao
import com.es.multivs.data.database.survey.SurveyDao
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
class DatabaseModule {

    @Singleton
    @Provides
    fun getInstance(@ApplicationContext context: Context): AppDataBase {
        return Room.databaseBuilder(context, AppDataBase::class.java, "database.db")
            .fallbackToDestructiveMigration()
            .build()
    }

    @Singleton
    @Provides
    fun provideGatewayDao(appDataBase: AppDataBase): GatewayDao {
        return appDataBase.getGatewayDao()
    }

    @Singleton
    @Provides
    fun provideLocationDao(appDataBase: AppDataBase): LocationDao {
        return appDataBase.getLocationDao()
    }

    @Singleton
    @Provides
    fun provideMultiVSDao(appDataBase: AppDataBase): MultiVSDao {
        return appDataBase.getMultiVsDao()
    }

    @Singleton
    @Provides
    fun provideMeasurementDao(appDataBase: AppDataBase): MeasurementDao {
        return appDataBase.getMeasurementDao()
    }

    @Singleton
    @Provides
    fun provideMedicationDao(appDataBase: AppDataBase): MedicationDao {
        return appDataBase.getMedicationDao()
    }

    @Singleton
    @Provides
    fun provideCalibrationDao(appDataBase: AppDataBase): CalibrationDao {
        return appDataBase.getCalibrationDao()
    }

    @Singleton
    @Provides
    fun provideSurveyDao(appDataBase: AppDataBase): SurveyDao {
        return appDataBase.getSurveyDao()
    }

}