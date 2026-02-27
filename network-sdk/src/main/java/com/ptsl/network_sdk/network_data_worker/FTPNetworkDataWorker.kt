package com.ptsl.network_sdk.network_data_worker

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.os.Build
import android.telephony.SubscriptionManager
import android.util.Log
import androidx.core.app.ActivityCompat
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import androidx.work.workDataOf
import com.google.gson.Gson
import com.ptsl.network_sdk.api.ApiService
import com.ptsl.network_sdk.data_model.FTPCellInfoGetDataRequest
import com.ptsl.network_sdk.data_model.FTPNetworkDataRequest
import com.ptsl.network_sdk.data_model.entity.AuthEntity
import com.ptsl.network_sdk.data_model.entity.FTPCellInfoGetRequest
import com.ptsl.network_sdk.data_model.entity.FTPNetworkDataEntity
import com.ptsl.network_sdk.data_model.entity.FTPThresholdEntity
import com.ptsl.network_sdk.data_model.logger.EventLogModel
import com.ptsl.network_sdk.data_model.logger.LogDataWrapper
import com.ptsl.network_sdk.db.NetworkDao
import com.ptsl.network_sdk.dl_ul_test.DownloadUploadHelper
import com.ptsl.network_sdk.utils.NetworkEventLogger
import com.ptsl.network_sdk.utils.prepareFTPData
import cz.mroczis.netmonster.core.factory.NetMonsterFactory
import cz.mroczis.netmonster.core.model.connection.PrimaryConnection

import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.withTimeout
import retrofit2.HttpException

/**
 * Worker responsible for comprehensive network diagnostic capture (FTP + Cell Info).
 * Performs quality assessments based on dynamic thresholds and returns qualitative results.
 */
class FTPNetworkDataWorker(
    appContext: Context,
    workerParams: WorkerParameters,
    private val apiService: ApiService,
    private val downloader: DownloadUploadHelper,
    private val databaseDao: NetworkDao,
) : CoroutineWorker(appContext, workerParams) {

    private val TAG = "FTPNetworkDataWorker"

    override suspend fun doWork(): Result {
        return try {
            withTimeout(60000) { // Global 60-second timeout
                // 1. Initial Validations (Permissions, IP Connectivity, SIM)
                val validationError = performPreFlightChecks()
                if (validationError != null) return@withTimeout validationError
                
                // 1.5 Sync Thresholds if necessary
                updateThresholdsIfNeeded()

                // 2. Data Preparation
                val authEntity = getAuth()
                val locationPair = LocationHelper.getCurrentLocation(applicationContext)
                val ftpData = getCapturedNetworkData(locationPair)

                // 2.5 Technology Validation (Restricted to 4G)
                if (ftpData.technologyType == "NON_4G_IGNORED") {
                    Log.w(TAG, "FTP Capture ignored: Not on 4G network")
                    return@withTimeout returnResultToHost(
                        "Failed", "Failed", 400,
                        "FTP Capture is only supported on 4G (LTE) technology.", null
                    )
                }

                // 3. Input Data Enrichment
                enrichDataWithInput(ftpData)

                // 4. Remote Enrichment (Cell Info API)
                enrichDataWithCellInfo(ftpData, authEntity)

                // 5. Post Captured Data to Backend
                val response = apiService.postFTPNetworkData(
                    FTPNetworkDataRequest(auth = authEntity, data = ftpData)
                )

                // 6. Evaluate Result based on thresholds and return qualitatively
                processAndReturnFinalResult(ftpData, response.data.assessmentId)
            }
        } catch (e: TimeoutCancellationException) {
            Log.e(TAG, "❌ Global Timeout during assessment")
            logError(e, "FTP_CAPTURE_TIMEOUT", inputData.getString("integratedAppEventName") ?: "Event")
            returnResultToHost(
                "Failed", "Failed", 408,
                "Network assessment timed out. Please check your internet connection.", null
            )
        } catch (e: java.io.IOException) {
            Log.e(TAG, "❌ Network Error: ${e.message}")
            logError(e, "FTP_CAPTURE_NETWORK_ERROR", inputData.getString("integratedAppEventName") ?: "Event")
            returnResultToHost(
                "Failed", "Failed", 400,
                "Network assessment failed. Please check your internet connection.", null
            )
        } catch (e: Exception) {
            Log.e(TAG, "❌ Unexpected Execution Error: ${e.message}")
            logError(e, "FTP_CAPTURE_EXECUTION_ERROR", inputData.getString("integratedAppEventName") ?: "Event")
            returnResultToHost(
                "Failed", "Failed", 400,
                "Network assessment failed due to internal error.", null
            )
        }
    }

    private fun performPreFlightChecks(): Result? {
        // Permission Check
        if (!hasRequiredPermissions()) {
            Log.w(TAG, "Missing required permissions for FTP Capture")
            return returnResultToHost(
                "Failed", "Failed", 400,
                "Permission are required for FTP Capture", null
            )
        }

        // Internet Connectivity Check
        if (!isInternetAvailable()) {
            Log.w(TAG, "No internet access for diagnostic capture")
            return returnResultToHost(
                "Failed", "Failed", 400,
                "Internet connectivity is mandatory for network assessment.", null
            )
        }

        // Banglalink SIM and Mobile Data Check
        if (!isBanglalinkDataEnabled()) {
            Log.w(TAG, "Banglalink data not active or SIM mismatch")
            return returnResultToHost(
                "Failed", "Failed", 400,
                "Banglalink SIM and mobile data must be enabled for FTP Capture.", null
            )
        }

        return null
    }

    private fun hasRequiredPermissions(): Boolean {
        return ActivityCompat.checkSelfPermission(
            applicationContext, Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED ||
        ActivityCompat.checkSelfPermission(
            applicationContext, Manifest.permission.ACCESS_COARSE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED
    }

    private fun isInternetAvailable(): Boolean {
        return try {
            val cm = applicationContext.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                val network = cm.activeNetwork ?: return false
                val caps = cm.getNetworkCapabilities(network) ?: return false
                caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) &&
                        caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED)
            } else {
                @Suppress("DEPRECATION")
                val activeNetworkInfo = cm.activeNetworkInfo
                activeNetworkInfo != null && activeNetworkInfo.isConnected
            }
        } catch (e: Exception) {
            false
        }
    }

    private fun isBanglalinkDataEnabled(): Boolean {
        return try {
            val sm = SubscriptionManager.from(applicationContext)
            if (ActivityCompat.checkSelfPermission(applicationContext, Manifest.permission.READ_PHONE_STATE) != PackageManager.PERMISSION_GRANTED) {
                return false
            }

            val subscriptions = sm.activeSubscriptionInfoList
            if (subscriptions.isNullOrEmpty()) return false

            val hasBLSim = subscriptions.any { it.mnc.toString().removePrefix("0") == "3" }
            if (!hasBLSim) return false

            val defaultDataId = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                SubscriptionManager.getDefaultDataSubscriptionId()
            } else {
                -1
            }

            if (defaultDataId != -1) {
                val info = sm.getActiveSubscriptionInfo(defaultDataId)
                info?.mnc?.toString()?.removePrefix("0") == "3"
            } else {
                subscriptions.firstOrNull()?.mnc?.toString()?.removePrefix("0") == "3"
            }
        } catch (e: Exception) {
            false
        }
    }

    private suspend fun getAuth(): AuthEntity = databaseDao.getPersistentAuth() ?: AuthEntity()

    private fun enrichDataWithInput(ftpData: FTPNetworkDataEntity) {
        ftpData.apply {
            msisdn = inputData.getString("msisdn") ?: ""
            integratedAppVersion = inputData.getString("integratedAppVersion") ?: ""
            sdkInitiateTimeStamp = inputData.getString("sdkInitiateTimeStamp") ?: ""
            integratedAppEventName = inputData.getString("integratedAppEventName") ?: ""
            userLatitude = inputData.getDouble("userLatitude", 0.0)
            userLongitude = inputData.getDouble("userLongitude", 0.0)
        }
    }

    private suspend fun enrichDataWithCellInfo(ftpData: FTPNetworkDataEntity, auth: AuthEntity) {
        if (ftpData.cid == 0 || ftpData.enb == 0) return

        try {
            val request = FTPCellInfoGetDataRequest(
                auth = auth,
                data = FTPCellInfoGetRequest(eNB = ftpData.enb, cID = ftpData.cid)
            )
            val response = apiService.postFTPCellInfo(request)
            if (response.statusCode == 200 && response.data.isNotEmpty()) {
                val cellInfo = response.data.first()
                ftpData.apply {
                    eNodeBName = cellInfo.eNodeBName
                    cellName = cellInfo.cellName
                    eNodeBId = cellInfo.eNodeBId
                    sector = cellInfo.sector
                    nbhDlThroughputMbps = cellInfo.nbhDlThroughputMbps
                    nbhTrafficGb = cellInfo.nbhTrafficGb
                }
                Log.d(TAG, "Cell Info Enriched: ${cellInfo.cellName}")
            }
        } catch (e: Exception) {
            Log.w(TAG, "Cell Enrichment failed: ${e.message}")
        }
    }

    private suspend fun getCapturedNetworkData(locationPair: Pair<Double, Double>): FTPNetworkDataEntity {
        val isMobileConnected = isMobileNetworkConnected()
        val activeMnc = if (isMobileConnected) getActiveNetworkMNC() else "-1"

        if (isMobileConnected && activeMnc.removePrefix("0") != "3") {
            return FTPNetworkDataEntity().apply { technologyType = "SKIP_MNC_MISMATCH" }
        }

        val cells = try {
            if (hasRequiredPermissions()) NetMonsterFactory.get(applicationContext).getCells() else null
        } catch (e: Exception) {
            null
        }

        if (cells.isNullOrEmpty()) return FTPNetworkDataEntity()

        for (cell in cells) {
            if (cell.connectionStatus is PrimaryConnection) {
                // Validation: Only allow 4G (LTE)
                if (cell !is cz.mroczis.netmonster.core.model.cell.CellLte) {
                    return FTPNetworkDataEntity().apply { technologyType = "NON_4G_IGNORED" }
                }

                val cellMnc = cell.network?.mnc ?: ""
                if (cellMnc.removePrefix("0") == "3") {
                    return cell.prepareFTPData(
                        locationPair, downloader, isMobileConnected, activeMnc,
                        getSimCount(), applicationContext
                    )
                }
            }
        }
        return FTPNetworkDataEntity()
    }

    private fun processAndReturnFinalResult(ftpData: FTPNetworkDataEntity, assessmentId: Long): Result {
        val thresholds = kotlinx.coroutines.runBlocking { databaseDao.getFTPThresholds() } ?: FTPThresholdEntity()

        val isRsrpPass = Math.abs(ftpData.rsrp) <= thresholds.rsrpThreshold
        val isDlSpeedPass = ftpData.dlSpeed > thresholds.dlSpeedThreshold
        val isNbhDlThroughputPass = ftpData.nbhDlThroughputMbps > thresholds.nbhDlThroughputThreshold

        val isPass = isRsrpPass && isDlSpeedPass && isNbhDlThroughputPass
        
        val status = if (isPass) "Success" else "Failed"
        val testResult = if (isPass) "Pass" else "Failed"
        val statusCode = if (isPass) 200 else 400
        val message = if (isPass) "Your network assessment was successful." else "Your network assessment failed."

        val dataMap = mapOf(
            "assessmentId" to assessmentId,
            "networkData" to mapOf("RSRP" to ftpData.rsrp, "SNR" to ftpData.snr, "RSRQ" to ftpData.rsrq),
            "cellInfo" to mapOf(
                "cellName" to ftpData.cellName,
                "eNodeBName" to ftpData.eNodeBName,
                "nbhDlThroughputMbps" to ftpData.nbhDlThroughputMbps,
                "nbhTrafficGB" to ftpData.nbhTrafficGb
            ),
            "speedPair" to mapOf("ulSpeedKbps" to ftpData.ulSpeed, "dlSpeedKbps" to ftpData.dlSpeed),
            "userInfo" to mapOf(
                "deviceManufacture" to ftpData.deviceManufacture,
                "deviceModel" to ftpData.deviceModel,
                "deviceOsVersion" to ftpData.deviceOsVersion,
                "latitude" to ftpData.latitude,
                "longitude" to ftpData.longitude,
                "msisdn" to ftpData.msisdn,
            )
        )

        return returnResultToHost(status, testResult, statusCode, message, dataMap)
    }

    private suspend fun updateThresholdsIfNeeded() {
        try {
            val cached = databaseDao.getFTPThresholds()
            val shouldFetch = cached == null || (System.currentTimeMillis() - cached.lastUpdated > 24 * 60 * 60 * 1000)
            
            if (shouldFetch) {
                val response = apiService.getFTPThresholds()
                if (response.statusCode == 200 && response.data != null) {
                    val newThresholds = response.data.apply { 
                        lastUpdated = System.currentTimeMillis()
                        id = 1 
                    }
                    databaseDao.insertFTPThresholds(newThresholds)
                    Log.i(TAG, "Thresholds sync successful")
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Threshold Update failed: ${e.message}")
        }
    }

    private suspend fun logError(e: Exception, errorCodePrefix: String, eventName: String) {
        val auth = getAuth()
        var statusCode = 0
        val errorMessage = when (e) {
            is HttpException -> {
                statusCode = e.code()
                "HTTP error: ${e.message}"
            }
            is java.io.IOException -> "Network error: ${e.message}"
            else -> "Unexpected error: ${e.message}"
        }

        val eventLogModel = NetworkEventLogger.createNetworkRequestFailedLog(
            auth.hostAppName, eventName, errorMessage, e.stackTraceToString(), statusCode
        ).apply {
            msisdn = inputData.getString("msisdn") ?: ""
            integratedAppVersion = inputData.getString("integratedAppVersion") ?: ""
            sdkInitiateTimeStamp = inputData.getString("sdkInitiateTimeStamp") ?: ""
            integratedAppEventName = eventName
            userLatitude = inputData.getDouble("userLatitude", 0.0)
            userLongitude = inputData.getDouble("userLongitude", 0.0)
        }

        preparedLogEventData(auth, eventLogModel)
    }

    private suspend fun preparedLogEventData(auth: AuthEntity, eventLogModel: EventLogModel) {
        try {
            val logsToSend = mutableListOf<EventLogModel>()
            val cachedCount = databaseDao.getNetworkDataLogEventCount()
            if (cachedCount > 0) {
                logsToSend.addAll(databaseDao.getNetworkDataLogEvent())
            }
            logsToSend.add(eventLogModel)

            apiService.postNetworkDataLogs(LogDataWrapper(auth, ArrayList(logsToSend)))

            if (cachedCount > 0) {
                databaseDao.deleteNetworkDataLogEvent()
            }
        } catch (e: Exception) {
            databaseDao.insertNetworkDataLogIntoDB(eventLogModel)
        }
    }

    private fun returnResultToHost(
        status: String,
        testResult: String,
        statusCode: Int,
        message: String,
        data: Any?
    ): Result {
        val responseMap = mapOf(
            "status" to status,
            "testResult" to testResult,
            "statusCode" to statusCode,
            "message" to message,
            "data" to data
        )

        val responseJson = Gson().toJson(responseMap)
        Log.i(TAG, "Assessment complete: Status=$status, Result=$testResult")
        return Result.success(workDataOf("hostAppResponse" to responseJson))
    }

    private fun isMobileNetworkConnected(): Boolean {
        return try {
            val cm = applicationContext.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
            val network = cm.activeNetwork ?: return false
            val caps = cm.getNetworkCapabilities(network) ?: return false
            caps.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR)
        } catch (e: Exception) {
            false
        }
    }

    private fun getActiveNetworkMNC(): String {
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                val sm = SubscriptionManager.from(applicationContext)
                val dataSubId = SubscriptionManager.getDefaultDataSubscriptionId()
                if (dataSubId != SubscriptionManager.INVALID_SUBSCRIPTION_ID) {
                    val si = sm.getActiveSubscriptionInfo(dataSubId)
                    return "0${si?.mnc ?: -1}"
                }
            }
        } catch (_: Exception) {}
        return "0-1"
    }

    private fun getSimCount(): Int {
        return try {
            val sm = SubscriptionManager.from(applicationContext)
            sm.activeSubscriptionInfoList?.size ?: 0
        } catch (e: Exception) {
            0
        }
    }
}