package com.ptsl.network_sdk.network_data_worker

import android.content.Context
import android.util.Log
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.google.gson.Gson
import com.ptsl.network_sdk.data_model.entity.NetworkDataEntity
import com.ptsl.network_sdk.data_source.ApiNetworkRemoteSource
import com.ptsl.network_sdk.data_source.RoomNetworkLocalDataSource
import com.ptsl.network_sdk.repository.NetworkDataRepository
import com.ptsl.network_sdk.data_model.MeasurementContext
import com.ptsl.network_sdk.data_model.NetworkDataRequest
import com.ptsl.network_sdk.data_model.enrichWithContext
import com.ptsl.network_sdk.utils.CommonUtils
import com.ptsl.network_sdk.location.LocationHelper
import com.ptsl.network_sdk.utils.NetworkEventLogger
import com.ptsl.network_sdk.utils.SdkContainer
import kotlinx.coroutines.withTimeout
import retrofit2.HttpException
import java.io.IOException

/**
 * Worker responsible for periodic background network data collection.
 * Captures signal strength, RTT, and latency, then uploads to the primary backend.
 */
class NetworkDataWorker(
    appContext: Context,
    workerParams: WorkerParameters,
) : CoroutineWorker(appContext, workerParams) {
    private val TAG = "NetworkDataWorker"
    private val sdk by lazy {
        SdkContainer.init(applicationContext)
        SdkContainer
    }
    private val apiService get() = sdk.apiService
    private val downloader get() = sdk.downloadUploadHelper
    private val databaseDao get() = sdk.dao
    private val remoteSource: ApiNetworkRemoteSource?
        get() {
            val service = apiService ?: return null
            return ApiNetworkRemoteSource(service)
        }
    private val repository: NetworkDataRepository?
        get() {
            val dao = databaseDao ?: return null
            val remote = remoteSource ?: return null
            return NetworkDataRepository(
                localDataSource = RoomNetworkLocalDataSource(dao), remoteDataSource = remote
            )
        }

    override suspend fun doWork(): Result {
        val msisdn = inputData.getString("msisdn") ?: ""
        val integratedAppVersion = inputData.getString("integratedAppVersion") ?: ""
        val sdkInitiateTimeStamp = inputData.getString("sdkInitiateTimeStamp") ?: ""
        val integratedAppEventName = inputData.getString("integratedAppEventName") ?: ""
        val userLatitude = inputData.getDouble("userLatitude", 0.0)
        val userLongitude = inputData.getDouble("userLongitude", 0.0)

        val mContext = MeasurementContext(
            msisdn = msisdn,
            integratedAppVersion = integratedAppVersion,
            sdkInitiateTimeStamp = sdkInitiateTimeStamp,
            integratedAppEventName = integratedAppEventName,
            userLatitude = userLatitude,
            userLongitude = userLongitude
        )

        val repository = repository ?: return Result.failure()
        val remoteSource = remoteSource ?: return Result.failure()
        val authEntity = repository.getAuth()
        val newDataList: MutableList<NetworkDataEntity> = mutableListOf()
        if (!SdkContainer.isInitialized()) {
            return Result.failure()
        }
        return try {
            withTimeout(60_000L) {
                // 1. Fetch current location
                val locationPair = getLocationPair()

                // 2. Capture network data (signal, RTT, Latency)
                val collector = NetworkDataCollector(
                    context = applicationContext,
                    downloader = downloader,
                    repository = repository,
                    authProvider = { authEntity })
                val dataList = collector.collect(
                    locationPair = locationPair, measurementContext = mContext
                ).toMutableList()

                for (data in dataList) {
                    newDataList.add(data.enrichWithContext(mContext))
                }

                repository.uploadNetworkData(
                    auth = authEntity,
                    newData = newDataList,
                )

                Log.i(TAG, "✅ Network data uploaded successfully")
                Result.success()

            }
        } catch (e: Exception) {
            val statusCode = if (e is HttpException) e.code() else 0

            val errorMessage = when (e) {
                is HttpException -> "HTTP error: ${e.code()} ${e.message}"
                is IOException -> "Network error: ${e.message}"
                else -> "Unexpected error: ${e.message}"
            }

            repository.cacheNetworkData(newDataList)

            val failedRequest = try {
                Gson().toJson(NetworkDataRequest(authEntity, newDataList))
            } catch (ex: Exception) {
                "Serialization failed: ${ex.message}"
            }

            val eventLogModel = NetworkEventLogger.createNetworkRequestFailedLog(
                authEntity.hostAppName,
                eventName = integratedAppEventName,
                errorMessage = errorMessage,
                stackTrace = failedRequest,
                statusCode = statusCode
            ).enrichWithContext(mContext)

            repository.sendLog(
                auth = authEntity,
                eventLogModel = eventLogModel
            )
            Log.w(TAG, "⚠️ Upload failed: ${e.message}")
            Result.failure()
        }
    }

    private suspend fun getLocationPair(): Pair<Double, Double> {
        val appContext = applicationContext
        return if (CommonUtils.isGpsEnabled(appContext)) {
            LocationHelper.getCurrentLocation(appContext)
        } else {
            Pair(0.0, 0.0)
        }
    }
}