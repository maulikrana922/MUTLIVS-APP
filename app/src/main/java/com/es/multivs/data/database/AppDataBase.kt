package com.es.multivs.data.database

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.es.multivs.data.database.calibrations.CalibrationDao
import com.es.multivs.data.database.entities.*
import com.es.multivs.data.database.gateway.GatewayDao
import com.es.multivs.data.database.location.LocationDao
import com.es.multivs.data.database.measurements.MeasurementDao
import com.es.multivs.data.database.medications.MedicationDao
import com.es.multivs.data.database.multivs.MultiVSDao
import com.es.multivs.data.database.survey.SurveyDao
import com.es.multivs.data.models.SurveyPostAnswer

@Database(
    entities = [GatewayData::class,
        UserLocation::class,
        MultiVSData::class,
        MeasurementData::class,
        MedicationData::class,
        CalibrationData::class,
        NonMultiVSResults::class,
        SurveyScheduleItem::class,
        SurveyPostAnswer::class],
    version = 2
)
@TypeConverters(Converters::class)
abstract class AppDataBase : RoomDatabase() {

    abstract fun getGatewayDao(): GatewayDao
    abstract fun getLocationDao(): LocationDao
    abstract fun getMultiVsDao(): MultiVSDao
    abstract fun getMeasurementDao(): MeasurementDao
    abstract fun getMedicationDao(): MedicationDao
    abstract fun getCalibrationDao(): CalibrationDao
    abstract fun getSurveyDao(): SurveyDao
}