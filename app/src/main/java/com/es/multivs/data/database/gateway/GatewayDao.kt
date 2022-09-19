package com.es.multivs.data.database.gateway

import androidx.lifecycle.LiveData
import androidx.room.*
import com.es.multivs.data.database.entities.GatewayData
import com.es.multivs.data.database.sets.*


/**
 * DataDao interface to handle the [androidx.room.RoomDatabase].
 */
@Dao
interface GatewayDao {

    /**
     * @return String livedata object
     */
    @Query("SELECT country FROM gatewaydata")
    fun getUserCountryLiveData(): LiveData<String>

    @Query("SELECT country FROM gatewaydata")
    suspend fun getUserCountry(): String?

    /**
     * @return UserCredentials livedata object
     */
    @Query("SELECT username, password FROM gatewaydata")
    fun getUserCredentials(): LiveData<UserCredentials>

    /**
     * @return UserCredentials object
     */
    @Query("SELECT username, password FROM gatewaydata")
    suspend fun getUserCountrySuspended(): UserCredentials?

    /**
     * @return number of rows
     */
    @Query("SELECT COUNT(id) FROM gatewaydata")
    suspend fun getRows(): Int

    /**
     * Insert initial values to the gatewayData table.
     *
     * @param gatewayData object.
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(gatewayData: GatewayData)

    @Transaction
    suspend fun updateUserIDAndIsManual(isManual: Int, userID: Int) {
        updateIsManual(isManual)
        updateUserID(userID)
    }

    /**
     * Update is_manual in the gatewaydata table. is_manual refers to whether the user can manually
     * input measurement results.
     *
     * @param isManual int type
     */
    @Query("UPDATE gatewayData SET is_manual= :isManual")
    suspend fun updateIsManual(isManual: Int)

    /**
     * Update user_id in the gatewaydata table.
     *
     * @param userID int type
     */
    @Query("UPDATE gatewaydata SET user_id = :userID")
    suspend fun updateUserID(userID: Int)

    /**
     * Update gateway_battery, build, version and mac_address in the gatewaydata table.
     *
     * @param batteryLevel int
     * @param build        String
     * @param version      String type
     */
    @Query("UPDATE gatewaydata SET gateway_battery = :batteryLevel, build = :build, version = :version")
    suspend fun updateGatewayInfo(batteryLevel: Int, build: String, version: String)

    @Query("UPDATE gatewaydata SET username = :username, password = :password WHERE id = 1")
    suspend fun updateUserCredentials(username: String, password: String)

    @Query("UPDATE gatewaydata SET base_url = :url, country = :country WHERE id = 1")
    suspend fun updateBaseURLAndCountry(url: String, country: String?)

    @Query("SELECT base_url FROM gatewaydata")
    suspend fun getBaseURL(): String

    @Query("SELECT username FROM gatewaydata")
    suspend fun getUsername(): String

    @Query("UPDATE gatewaydata SET identifier = :identifier")
    suspend fun saveIdentifier(identifier: String)

    @Query("SELECT identifier FROM gatewaydata")
    suspend fun getIdentifier(): String

    @Query("SELECT user_id FROM gatewaydata")
    suspend fun getUserID(): Int

    @Query("SELECT is_manual FROM gatewaydata")
    suspend fun getIsManualInput(): Int

    @Query("SELECT is_manual FROM gatewaydata")
    fun getIsManualInputLiveData(): LiveData<Int>

    @Query("SELECT post_frequency FROM gatewaydata")
    suspend fun getPostFrequency(): Int

    @Query("UPDATE gatewaydata SET post_frequency=:frequency")
    suspend fun setPostFrequency(frequency: Int)
}