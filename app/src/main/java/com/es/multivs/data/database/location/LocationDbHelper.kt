package com.es.multivs.data.database.location

import com.es.multivs.data.database.entities.UserLocation
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class LocationDbHelper @Inject constructor(
    private val dataDao: LocationDao
) {

    suspend fun insertLocation(userLocation: UserLocation) {
        dataDao.insertLocation(userLocation)
    }

    suspend fun getLocation(): UserLocation {
        return withContext(Dispatchers.IO) {
            dataDao.getLocation()
        }
    }
}
