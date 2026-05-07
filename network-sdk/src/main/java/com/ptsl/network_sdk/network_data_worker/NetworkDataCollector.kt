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
import com.ptsl.network_sdk.data_model.entity.AuthEntity
import com.ptsl.network_sdk.data_model.entity.NetworkDataEntity
import com.ptsl.network_sdk.data_source.ApiNetworkRemoteSource
import com.ptsl.network_sdk.bandwidth.DownloadUploadHelper
import com.ptsl.network_sdk.utils.CommonUtils
import com.ptsl.network_sdk.utils.NetworkEventLogger
import com.ptsl.network_sdk.permission.PermissionHandler
import com.ptsl.network_sdk.data_model.MeasurementContext
import com.ptsl.network_sdk.data_model.enrichWithContext
import com.ptsl.network_sdk.repository.NetworkDataRepository
import com.ptsl.network_sdk.utils.calculateRttAndLatency
import com.ptsl.network_sdk.utils.prepareData
import cz.mroczis.netmonster.core.factory.NetMonsterFactory
import cz.mroczis.netmonster.core.model.connection.PrimaryConnection

class NetworkDataCollector(
    private val context: Context,
    private val downloader: DownloadUploadHelper?,
    private val repository: NetworkDataRepository,
    private val authProvider: suspend () -> AuthEntity
) {
    private val tag = "NetworkDataCollector"

    suspend fun collect(
        locationPair: Pair<Double, Double>,
        measurementContext: MeasurementContext
    ): List<NetworkDataEntity> {
        val dataList = arrayListOf<NetworkDataEntity>()

        return try {
            val testUrl = "https://crsrcgz.banglalink.net"
            val isMobileConnected = isMobileNetworkConnected()
            val activeNetworkMnc = if (isMobileConnected) getActiveNetworkMNC() else "-1"

            val metrics = calculateRttAndLatency(
                hasMobileInternet = true, testUrl = testUrl
            )

            Log.d(tag, "Metrics captured: RTT=${metrics.rtt}ms, Latency=${metrics.latency}ms")

            var permissionException: Exception? = null

            val cells = try {
                val hasPermission = PermissionHandler.isLocationPermissionGranted(context)

                if (hasPermission) {
                    NetMonsterFactory.get(context).getCells()
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
                val auth = authProvider()
                val eventLogModel = NetworkEventLogger.createPermissionMissingLog(
                    auth.hostAppName,
                    measurementContext.integratedAppEventName,
                    permissionException?.message ?: "N/A",
                    permissionException?.stackTraceToString()
                ).enrichWithContext(measurementContext)
                repository.sendLog(auth, eventLogModel)

                dataList.add(
                    NetworkDataEntity(
                        lattitude = 0.0,
                        longitude = 0.0,
                        data = if (isMobileConnected) "Mobile" else "Wifi",
                        usedSimSlot = getSimCount(),
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
                    downloader?.let {
                        dataList.add(
                            cell.prepareData(
                                locationPair,
                                it,
                                isMobileConnected,
                                activeNetworkMnc,
                                getSimCount(),
                                rtt = CommonUtils.round2(metrics.rtt),
                                latency = CommonUtils.round2(metrics.latency),
                                context
                            )
                        )
                    }
                }
            }

            dataList
        } catch (e: Exception) {
            val auth = authProvider()
            val eventLogModel = NetworkEventLogger.createNetworkDataFetchFailedLog(
                auth.hostAppName,
                measurementContext.integratedAppEventName,
                e.message ?: "N/A",
                e.stackTraceToString()
            ).enrichWithContext(measurementContext)
            repository.sendLog(auth, eventLogModel)

            dataList
        }
    }

    private fun isMobileNetworkConnected(): Boolean {
        return try {
            val cm = context.getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager
            val network = cm?.activeNetwork ?: return false
            val caps = cm.getNetworkCapabilities(network) ?: return false
            caps.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR)
        } catch (_: Exception) {
            false
        }
    }

    private fun getActiveNetworkMNC(): String {
        var mnc = "-1"
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                val hasPhoneStatePermission =
                    PermissionHandler.isPhoneStatePermissionGranted(context)
                if (!hasPhoneStatePermission) return "0${mnc}"

                val sm =
                    context.getSystemService(SubscriptionManager::class.java) ?: return "0${mnc}"
                val dataSubId = SubscriptionManager.getDefaultDataSubscriptionId()
                if (dataSubId != SubscriptionManager.INVALID_SUBSCRIPTION_ID) {
                    val si = sm.getActiveSubscriptionInfo(dataSubId)
                    mnc = getSubscriptionMnc(si)
                }
            }
        } catch (_: Exception) {
        }
        return "0${mnc}"
    }

    private fun getSubscriptionMnc(info: android.telephony.SubscriptionInfo?): String {
        if (info == null) return "-1"
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            info.mncString ?: "-1"
        } else {
            @Suppress("DEPRECATION") info.mnc.toString()
        }
    }

    private fun getSimCount(): Int {
        return try {
            val hasPhoneStatePermission = PermissionHandler.isPhoneStatePermissionGranted(context)
            if (!hasPhoneStatePermission) return 0

            val sm = context.getSystemService(SubscriptionManager::class.java)
            sm?.activeSubscriptionInfoList?.size ?: 0
        } catch (_: Exception) {
            0
        }
    }
}
