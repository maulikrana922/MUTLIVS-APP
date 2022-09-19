package com.es.multivs.data.database.location

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.es.multivs.data.database.entities.UserLocation

/**
 * Created by Marko on 10/19/2021.
 * Etrog Systems LTD.
 */
@Dao
interface LocationDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertLocation(userLocation: UserLocation)

    @Query("SELECT * FROM UserLocation")
    suspend fun getLocation(): UserLocation
}