package com.ptsl.network_sdk.utils

import android.content.Context
import android.util.Log
import androidx.work.Configuration
import androidx.work.WorkManager

internal object SdkManager {
    fun init(context: Context) {
        try {
            val config = Configuration.Builder()
                .setWorkerFactory(SdkWorkerFactory())
                .build()
            WorkManager.initialize(context.applicationContext, config)
        } catch (e: IllegalStateException) {
            // Log if already initialized, but don't crash the host app
            Log.w("SdkManager", "WorkManager is already initialized. Skipping initialization.")
        } catch (e: Exception) {
            Log.e("SdkManager", "Failed to initialize WorkManager", e)
        }
    }
}