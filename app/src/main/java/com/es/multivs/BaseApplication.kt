package com.es.multivs

import androidx.hilt.work.HiltWorkerFactory
import androidx.work.Configuration
import com.es.multivs.data.utils.AppUtils
import com.es.multivs.data.utils.SharedPrefs
import com.example.bluetoothlibrary.Config
import dagger.hilt.android.HiltAndroidApp
import javax.inject.Inject

/*
* created by Marko
Etrog Systems LTD. 30/8/2021.
*/

@HiltAndroidApp
class BaseApplication : Config(), Configuration.Provider {

    @Inject lateinit var workerFactory: HiltWorkerFactory

    override fun getWorkManagerConfiguration(): Configuration {
        return Configuration.Builder().setWorkerFactory(workerFactory).build()
    }

    override fun onCreate() {
        super.onCreate()
        AppUtils.application = this
        SharedPrefs.loadAppPrefs(this)
    }
}