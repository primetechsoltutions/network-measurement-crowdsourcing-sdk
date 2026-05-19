package com.ptsl.crowdsourcing_network_sdk.utils

import android.content.Context
import com.ptsl.crowdsourcing_network_sdk.utils.SdkLogger as Log
import androidx.room.Room
import com.ptsl.crowdsourcing_network_sdk.api.ApiService
import com.ptsl.crowdsourcing_network_sdk.api.NetworkModule
import com.ptsl.crowdsourcing_network_sdk.db.NetworkDao
import com.ptsl.crowdsourcing_network_sdk.db.NetworkDatabase
import com.ptsl.crowdsourcing_network_sdk.dl_ul_test.DownloadUploadHelper
import com.ptsl.crowdsourcing_network_sdk.repository.DataFacade
import com.ptsl.crowdsourcing_network_sdk.repository.DataFacadeImpl
import com.ptsl.crowdsourcing_network_sdk.repository.LocalCacheRepositoryImpl
import com.ptsl.crowdsourcing_network_sdk.repository.NetworkRepositoryImpl
import kotlinx.coroutines.CoroutineExceptionHandler

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob

internal object SdkContainer {
    internal var database: NetworkDatabase? = null
        private set
    internal var coroutineScope: CoroutineScope? = null
        private set
    internal var downloadUploadHelper: DownloadUploadHelper? = null
        private set

    internal var dataFacade: DataFacade? = null
        private set

    @Volatile
    private var initialized = false

    fun isInitialized(): Boolean = initialized &&
            database != null &&
            coroutineScope != null &&
            downloadUploadHelper != null &&
            dataFacade != null



    @Synchronized
    fun init(context: Context) {
        if (isInitialized()) {
            Log.i("SdkContainer", "Already initialized, skipping re-initialization")
            return
        }

        try {
            val appContext = context.applicationContext
            val database = Room.databaseBuilder(
                appContext,
                NetworkDatabase::class.java,
                "network_db"
            ).fallbackToDestructiveMigration().build()
            this.database = database
            val dao = database.networkDao()

            val exceptionHandler = CoroutineExceptionHandler { _, exception ->
                Log.e("SdkContainer", "Coroutine error: ${exception.message}")
            }

            coroutineScope = CoroutineScope(
                SupervisorJob() + Dispatchers.IO + exceptionHandler
            )
            
            val apiService = NetworkModule.apiService
            
            // Initialize Repositories and Facade
            val localCacheRepository = LocalCacheRepositoryImpl(dao)
            val networkRepository = NetworkRepositoryImpl(apiService)
            dataFacade = DataFacadeImpl(localCacheRepository, networkRepository)

            downloadUploadHelper = DownloadUploadHelper(dataFacade!!)

            initialized = true


            Log.i("SdkContainer", "SdkContainer initialized successfully")
        } catch (e: Exception) {
            initialized = false
            Log.e("SdkContainer", "Init failed: ${e.message}")
        }
    }
}