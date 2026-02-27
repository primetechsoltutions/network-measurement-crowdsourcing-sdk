package com.ptsl.network_sdk.utils

import android.content.Context
import androidx.work.ListenableWorker
import androidx.work.WorkerFactory
import androidx.work.WorkerParameters
import com.ptsl.network_sdk.network_data_worker.FTPNetworkDataWorker
import com.ptsl.network_sdk.network_data_worker.NetworkDataWorker

internal class SdkWorkerFactory : WorkerFactory(){
    override fun createWorker(
        appContext: Context,
        workerClassName: String,
        workerParameters: WorkerParameters
    ): ListenableWorker? {
        return when (workerClassName){
            NetworkDataWorker::class.java.name->
                NetworkDataWorker(
                    appContext,
                    workerParameters,
                    SdkContainer.apiService,
                    SdkContainer.downloadUploadHelper,
                    SdkContainer.dao
                )
            FTPNetworkDataWorker::class.java.name ->
                FTPNetworkDataWorker(
                    appContext,
                    workerParameters,
                    SdkContainer.apiService,
                    SdkContainer.downloadUploadHelper,
                    SdkContainer.dao
                )
            else -> null
        }
    }
}