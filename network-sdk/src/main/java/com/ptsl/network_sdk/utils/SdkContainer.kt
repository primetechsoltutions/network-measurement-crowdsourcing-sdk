package com.ptsl.network_sdk.utils

import android.content.Context
import android.util.Log
import androidx.room.Room
import com.ptsl.network_sdk.api.ApiService
import com.ptsl.network_sdk.api.NetworkModule
import com.ptsl.network_sdk.db.NetworkDao
import com.ptsl.network_sdk.db.NetworkDatabase
import com.ptsl.network_sdk.bandwidth.DownloadUploadHelper
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob

internal object SdkContainer {
    var database: NetworkDatabase? = null
    var dao: NetworkDao? = null
    var coroutineScope: CoroutineScope? = null
    var apiService: ApiService? = null
    var downloadUploadHelper: DownloadUploadHelper? = null

    @Volatile
    private var initialized = false

    fun isInitialized(): Boolean = initialized &&
            database != null &&
            dao != null &&
            coroutineScope != null &&
            apiService != null &&
            downloadUploadHelper != null

    @Synchronized
    fun init(context: Context) {
        if (isInitialized()) {
            Log.i("SdkContainer", "Already initialized, skipping re-initialization")
            return
        }

        try {
            val appContext = context.applicationContext
            database = Room.databaseBuilder(
                appContext,
                NetworkDatabase::class.java,
                "network_db"
            ).fallbackToDestructiveMigration(true).build()
            dao = database?.networkDao()

            val exceptionHandler = CoroutineExceptionHandler { _, exception ->
                Log.e("SdkContainer", "Coroutine error: ${exception.message}")
            }

            coroutineScope = CoroutineScope(
                SupervisorJob() + Dispatchers.IO + exceptionHandler
            )
            apiService = NetworkModule.apiService
            downloadUploadHelper = apiService?.let { DownloadUploadHelper(it) }

            initialized = true
            Log.i("SdkContainer", "SdkContainer initialized successfully")
        } catch (e: Exception) {
            initialized = false
            Log.e("SdkContainer", "Init failed: ${e.message}")
        }
    }
}