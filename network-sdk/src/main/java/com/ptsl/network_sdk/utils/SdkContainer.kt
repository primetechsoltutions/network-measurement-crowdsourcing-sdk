package com.ptsl.network_sdk.utils

import android.content.Context
import androidx.room.Room
import com.ptsl.network_sdk.api.ApiService
import com.ptsl.network_sdk.api.NetworkModule
import com.ptsl.network_sdk.db.NetworkDao
import com.ptsl.network_sdk.db.NetworkDatabase
import com.ptsl.network_sdk.dl_ul_test.DownloadUploadHelper
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob

internal  object SdkContainer {
    lateinit var database: NetworkDatabase
    lateinit var dao: NetworkDao
    lateinit var coroutineScope: CoroutineScope
    lateinit var apiService: ApiService
    lateinit var downloadUploadHelper: DownloadUploadHelper


    fun init(context: Context){
        if (this::database.isInitialized) return // Already initialized

        database = Room.databaseBuilder(
            context,
            NetworkDatabase::class.java,
            "network_db"
        ).fallbackToDestructiveMigration().build()

        dao = database.networkDao()

        val exceptionHandler = kotlinx.coroutines.CoroutineExceptionHandler { _, exception ->
            android.util.Log.e("SdkContainer", "Caught unhandled exception in SDK coroutine: ${exception.message}", exception)
        }
        coroutineScope = CoroutineScope(
            SupervisorJob() + Dispatchers.Default + exceptionHandler
        )

        apiService = NetworkModule.apiService
        downloadUploadHelper = DownloadUploadHelper(apiService)
    }
}