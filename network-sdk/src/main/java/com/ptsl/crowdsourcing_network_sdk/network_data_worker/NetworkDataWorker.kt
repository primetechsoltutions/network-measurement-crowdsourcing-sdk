package com.ptsl.crowdsourcing_network_sdk.network_data_worker

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.util.Log
import androidx.core.app.ActivityCompat
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.ptsl.crowdsourcing_network_sdk.data_model.NetworkDataRequest
import com.ptsl.crowdsourcing_network_sdk.data_model.entity.AuthEntity
import com.ptsl.crowdsourcing_network_sdk.data_model.entity.NetworkDataEntity
import com.ptsl.crowdsourcing_network_sdk.data_model.logger.EventLogModel
import com.ptsl.crowdsourcing_network_sdk.data_model.logger.LogDataWrapper
import com.ptsl.crowdsourcing_network_sdk.utils.CommonUtils
import com.ptsl.crowdsourcing_network_sdk.utils.NetworkEventLogger
import com.ptsl.crowdsourcing_network_sdk.utils.SdkContainer
import com.ptsl.crowdsourcing_network_sdk.utils.calculateRttAndLatency
import com.ptsl.crowdsourcing_network_sdk.utils.toIntSafe
import cz.mroczis.netmonster.core.factory.NetMonsterFactory
import cz.mroczis.netmonster.core.model.cell.CellCdma
import cz.mroczis.netmonster.core.model.cell.CellGsm
import cz.mroczis.netmonster.core.model.cell.CellLte
import cz.mroczis.netmonster.core.model.cell.CellNr
import cz.mroczis.netmonster.core.model.cell.CellTdscdma
import cz.mroczis.netmonster.core.model.cell.CellWcdma
import cz.mroczis.netmonster.core.model.connection.PrimaryConnection
import kotlinx.coroutines.TimeoutCancellationException
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
    private val networkRepo get() = sdk.networkRepository
    private val localCacheRepo get() = sdk.localCacheRepository
    private val downloader get() = sdk.downloadUploadHelper
    private val cellDataMapper = CellDataMapper()


    override suspend fun doWork(): Result {
        val input = WorkerInputData.from(inputData)
        val authEntity = getAuth()
        if (!SdkContainer.isInitialized()) {
            return Result.failure()
        }

        var enrichedDataList: List<NetworkDataEntity> = emptyList()
        return try {

            withTimeout(60_000L) {
                // 1. Fetch current location
                val locationPair = if (CommonUtils.isGpsEnabled(applicationContext)) {
                    LocationHelper.getCurrentLocation(applicationContext)
                } else {
                    Pair(0.0, 0.0)
                }

                // 2. Capture network data (signal, RTT, Latency)
                val rawDataList = getReqData(locationPair, authEntity, input)

                // 3. Merge with any cached data from previous failed attempts and enrich with session info
                enrichedDataList = rawDataList.map { data ->

                    data.copy(
                        msisdn = input.msisdn,
                        integratedAppVersion = input.integratedAppVersion,
                        sdkInitiateTimeStamp = input.sdkInitiateTimeStamp,
                        integratedAppEventName = input.integratedAppEventName,
                        userLatitude = input.userLatitude,
                        userLongitude = input.userLongitude
                    )
                }

                val localData = localCacheRepo?.getNetworkData() ?: emptyList()
                val mergeData: List<NetworkDataEntity> = enrichedDataList + localData

                // 4. Send network data to backend
                networkRepo?.postNetworkData(
                    NetworkDataRequest(authEntity, mergeData)
                )

                Log.i(TAG, "✅ Network data uploaded successfully")

                localCacheRepo?.deleteNetworkData()

                Result.success()
            }
        }
        catch (e: Exception) {
            var statusCode = 0
            val errorMessage = when (e) {
                is HttpException -> {
                    statusCode = e.code()
                    val errorBody= e.response()?.errorBody()?.string()
                    "HTTP error: ${e.code()} ${e.message}${if (errorBody != null) " | Body: $errorBody" else ""}"
                }

                is IOException -> "Network error: ${e.message}"
                is TimeoutCancellationException -> "Timeout error: ${e.message}"
                else -> "Unexpected error: ${e.message}"
            }

            Log.w(TAG, "⚠️ Upload failed, caching data locally: $errorMessage")
            if (enrichedDataList.isNotEmpty()) {
                localCacheRepo?.insertNetworkData(enrichedDataList)
            }


            val eventLogModel = NetworkEventLogger.createNetworkRequestFailedLog(
                authEntity.hostAppName,
                eventName = input.integratedAppEventName,
                errorMessage = errorMessage,
                stackTrace = e.stackTraceToString(),
                statusCode = statusCode
            ).apply {
                this.msisdn = input.msisdn
                this.integratedAppVersion = input.integratedAppVersion
                this.sdkInitiateTimeStamp = input.sdkInitiateTimeStamp
                this.integratedAppEventName = input.integratedAppEventName
                this.userLatitude = input.userLatitude
                this.userLongitude = input.userLongitude
            }
            sendOrCacheLog(authEntity, eventLogModel)
            Result.failure()
        }
    }


    private suspend fun getAuth(): AuthEntity = localCacheRepo?.getAuth() ?: AuthEntity()


    /**
     * Captures core network metrics and cell info.
     */
    private suspend fun getReqData(
        locationPair: Pair<Double, Double>, auth: AuthEntity, input: WorkerInputData
    ): ArrayList<NetworkDataEntity> {


        val dataList = arrayListOf<NetworkDataEntity>()

        return try {
            val testUrl = "https://crsrcgz.banglalink.net"
            val isMobileConnected = CommonUtils.isMobileNetworkConnected(applicationContext)
            val activeNetworkMnc =
                if (isMobileConnected) CommonUtils.getActiveNetworkMNC(applicationContext) else "-1"

            val metrics = calculateRttAndLatency(
                hasMobileInternet = true, testUrl = testUrl
            )

            Log.d(TAG, "Metrics captured: RTT=${metrics.rtt}ms, Latency=${metrics.latency}ms")

            var permissionException: Exception? = null

            val cells = try {
                val hasPermission = ActivityCompat.checkSelfPermission(
                    applicationContext, Manifest.permission.ACCESS_FINE_LOCATION
                ) == PackageManager.PERMISSION_GRANTED || ActivityCompat.checkSelfPermission(
                    applicationContext, Manifest.permission.ACCESS_COARSE_LOCATION
                ) == PackageManager.PERMISSION_GRANTED

                if (hasPermission) {
                    NetMonsterFactory.get(applicationContext).getCells()
                } else {
                    permissionException = SecurityException("Missing location permission")
                    null
                }
            } catch (e: Exception) {
                permissionException = e
                null
            }

            // Fallback if no cell info is available
            if (cells.isNullOrEmpty()) {
                val eventLogModel = NetworkEventLogger.createPermissionMissingLog(
                    auth.hostAppName,
                    input.integratedAppEventName,
                    permissionException?.message ?: "N/A",
                    permissionException?.stackTraceToString()
                ).apply {
                    this.msisdn = input.msisdn
                    this.integratedAppVersion = input.integratedAppVersion
                    this.sdkInitiateTimeStamp = input.sdkInitiateTimeStamp
                    this.integratedAppEventName = input.integratedAppEventName
                    this.userLatitude = input.userLatitude
                    this.userLongitude = input.userLongitude
                }
                sendOrCacheLog(
                    auth, eventLogModel
                )


                dataList.add(
                    NetworkDataEntity(
                        lattitude = 0.0,
                        longitude = 0.0,
                        data = if (isMobileConnected) "Mobile" else "Wifi",
                        usedSimSlot = CommonUtils.getSimCount(applicationContext),
                        rtt = CommonUtils.round2(metrics.rtt),
                        latency = CommonUtils.round2(metrics.latency),
                        time = CommonUtils.getCurrentDateTime(),
                        date = CommonUtils.getCurrentDate(),
                        deviceModel = Build.MODEL,
                        deviceManufacture = Build.MANUFACTURER,
                        deviceOsVersion = Build.VERSION.SDK_INT.toString()
                    )
                )
                return dataList
            }

            // Process connection-primary cells
            cells.forEach { cell ->
                if (cell.connectionStatus is PrimaryConnection) {
                    downloader?.let { dl ->
                        val mnc = cell.network?.mnc
                        val networkType = when (cell) {
                            is CellCdma, is CellGsm -> "2G"
                            is CellWcdma, is CellTdscdma -> "3G"
                            is CellLte, is CellNr -> "4G"
                            else -> "Unknown"
                        }
                        val retryCount = if (networkType == "4G") 2 else 1

                        val speedPair = dl.getBandWidthSpeed(
                            networkType = networkType,
                            hasMobileInternet = isMobileConnected,
                            currentMnc = mnc?.let { toIntSafe(it).toString() },
                            activeNetworkMnc = activeNetworkMnc,
                            retryCountDownload = retryCount,
                            retryCountUpload = retryCount
                        )

                        dataList.add(
                            cellDataMapper.mapToEntity(
                                cell = cell,
                                locationPair = locationPair,
                                speedResult = speedPair,
                                hasMobileInternet = isMobileConnected,
                                usedSimSlot = CommonUtils.getSimCount(applicationContext),
                                rtt = CommonUtils.round2(metrics.rtt),
                                latency = CommonUtils.round2(metrics.latency),
                                context = applicationContext
                            )
                        )
                    }
                }
            }


            dataList

        } catch (e: Exception) {

            val eventLogModel = NetworkEventLogger.createNetworkDataFetchFailedLog(
                auth.hostAppName,
                input.integratedAppEventName,
                e.message ?: "N/A",
                e.stackTraceToString()
            ).apply {
                this.msisdn = input.msisdn
                this.integratedAppVersion = input.integratedAppVersion
                this.sdkInitiateTimeStamp = input.sdkInitiateTimeStamp
                this.integratedAppEventName = input.integratedAppEventName
                this.userLatitude = input.userLatitude
                this.userLongitude = input.userLongitude
            }
            sendOrCacheLog(
                auth, eventLogModel
            )


            dataList
        }
    }


    private suspend fun sendOrCacheLog(auth: AuthEntity, eventLogModel: EventLogModel) {

        try {
            val logsToSend = mutableListOf<EventLogModel>()
            val cachedLogs = localCacheRepo?.getEventLogs() ?: emptyList()
            if (cachedLogs.isNotEmpty()) {
                logsToSend.addAll(cachedLogs)
            }
            logsToSend.add(eventLogModel)

            val response =
                networkRepo?.postNetworkDataLogs(LogDataWrapper(auth, ArrayList(logsToSend)))

            if (response?.isSuccessful == true) {
                if (cachedLogs.isNotEmpty()) {
                    localCacheRepo?.deleteEventLogs()
                }
            } else {
                localCacheRepo?.insertEventLog(eventLogModel)
            }
        } catch (e: Exception) {
            localCacheRepo?.insertEventLog(eventLogModel)
        }
    }
}