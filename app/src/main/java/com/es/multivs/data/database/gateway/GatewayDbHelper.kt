package com.es.multivs.data.database.gateway

import androidx.lifecycle.LiveData
import com.es.multivs.data.database.entities.GatewayData
import com.es.multivs.data.database.sets.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import javax.inject.Inject

class GatewayDbHelper @Inject constructor(
    private val dataDao: GatewayDao
) {

    /**
     * @return String livedata object
     */
    fun getUserCountryLiveData(): LiveData<String> {
        return dataDao.getUserCountryLiveData();
    }

    suspend fun getUserCountrySuspended(): String? {
        return dataDao.getUserCountry()
    }

    /**
     * @return UserCredentials livedata object
     */
    fun getUserCredentials(): LiveData<UserCredentials> {
        return dataDao.getUserCredentials()
    }

    /**
     * @return UserCredentials object
     */
    suspend fun getUserCredentialsSuspended(): UserCredentials? {
        return dataDao.getUserCountrySuspended()
    }

    /**
     * @return number of rows
     */
    suspend fun getGatewayRowCount(): Int {
        return dataDao.getRows()
    }

    /**
     * Insert initial values to the gatewayData table.
     *
     * @param gatewayData object.
     */
    suspend fun insert(gatewayData: GatewayData) {
        dataDao.insert(gatewayData)
    }

    suspend fun updateUserIDAndIsManual(userID: Int, manual: Int) {
        dataDao.updateUserIDAndIsManual(manual, userID)

    }

    suspend fun updateGatewayInfo(batteryLevel: Int, build: String, version: String) {
        dataDao.updateGatewayInfo(batteryLevel, build, version)
    }

    suspend fun updateUserCredentials(username: String, password: String) {
        dataDao.updateUserCredentials(username, password);
    }

    suspend fun updateBaseURLAndCountry(url: String, country: String?) {
        dataDao.updateBaseURLAndCountry(url, country)
    }

    suspend fun getBaseURL(): String {
        return withContext(Dispatchers.IO) {
            dataDao.getBaseURL()
        }
//        return dataDao.getBaseURL();
    }

    suspend fun getUsername(): String {
        return withContext(Dispatchers.IO) {
            dataDao.getUsername()
        }
//        return dataDao.getUsername()
    }

    suspend fun saveIdentifier(identifier: String) {
        dataDao.saveIdentifier(identifier)
    }

    suspend fun getIdentifier(): String {
        return withContext(Dispatchers.IO) {
            dataDao.getIdentifier()
        }
    }

    suspend fun getUserID(): Int {
        return withContext(Dispatchers.IO) {
            dataDao.getUserID()
        }
    }

    suspend fun getIsManualInput(): Int {
        return dataDao.getIsManualInput()
    }

    fun getIsManualInputLiveData(): LiveData<Int> {
        return dataDao.getIsManualInputLiveData()
    }

    suspend fun getPostFrequency(): Int {
        return dataDao.getPostFrequency()
    }

    suspend fun setPostFrequency(frequency: Int) {
        dataDao.setPostFrequency(frequency)
    }
}