package com.ptsl.network_sdk.dl_ul_test

import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.ptsl.network_sdk.api.ApiService
import com.ptsl.network_sdk.data_model.BandWidth
import com.ptsl.network_sdk.data_model.BandwidthTestResult
import com.ptsl.network_sdk.data_model.BaseResponse
import com.ptsl.network_sdk.utils.getTotalBytes
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.RequestBody
import kotlin.math.roundToInt


class DownloadUploadHelper (private val apiService: ApiService) {


    suspend fun getBandWidthSpeed(
        networkType: String = "2G",
        retryCountDownload: Int = 1,
        retryCountUpload: Int = 1,
        hasMobileInternet: Boolean = false,
        currentMnc: String?,
        activeNetworkMnc: String = "-1"
    ): BandwidthTestResult {

        if (!hasMobileInternet || currentMnc?.toInt() != activeNetworkMnc.toInt()) {
            return BandwidthTestResult(0.0, 0.0, 0, 0)
        }

        val receivedPacketsInBytes = mutableListOf<Int>()
        val timeTakenInSec = mutableListOf<Double>()

        var downloadSpeedKbps = 0.0
        var uploadSpeedKbps = 0.0

        var totalDownloadBytes = 0L
        var totalUploadBytes = 0L

        // ---------------- DOWNLOAD ----------------
        var uploadRequestBodyModel: BaseResponse<BandWidth>? = null
        try {
            repeat(retryCountDownload) {
                val stopwatch = Stopwatch()
                stopwatch.start()
                try {
                    val response = apiService.getBandwidthFile(networkType)
                    if (response.isSuccessful) {
                        response.body()?.let { body ->
                            stopwatch.stop()
                            val timeSec = stopwatch.elapsedSeconds()
                            
                            // Capture the response for the upload phase if not already captured
                            if (uploadRequestBodyModel == null) {
                                try {
                                    val bodyString = body.string()
                                    uploadRequestBodyModel = Gson().fromJson(
                                        bodyString,
                                        object : TypeToken<BaseResponse<BandWidth>>() {}.type
                                    )
                                    // Calculate bytes from the captured string
                                    val bytes = bodyString.length
                                    timeTakenInSec.add(timeSec)
                                    receivedPacketsInBytes.add(bytes)
                                    totalDownloadBytes += bytes
                                } catch (e: Exception) {
                                    // Fallback if parsing fails
                                }
                            } else {
                                val bytes = body.getTotalBytes()
                                timeTakenInSec.add(timeSec)
                                receivedPacketsInBytes.add(bytes)
                                totalDownloadBytes += bytes
                            }
                        }
                    }
                } finally {
                    stopwatch.stop()
                    stopwatch.reset()
                }
            }

            val totalBitsKb = receivedPacketsInBytes.sumOf { it.toDouble() * 8 / 1000 }
            val totalTime = timeTakenInSec.sum()
            if (totalTime > 0) {
                downloadSpeedKbps = totalBitsKb / totalTime
            }

            receivedPacketsInBytes.clear()
            timeTakenInSec.clear()

        } catch (_: Exception) {
            downloadSpeedKbps = 0.0
        }

        // ---------------- UPLOAD ----------------
        try {
            // Reuse the model captured during download instead of calling apiService.getBandwidthFile again
            uploadRequestBodyModel?.let { reqModel ->
                repeat(retryCountUpload) {
                    val stopwatch = Stopwatch()
                    stopwatch.start()
                    try {
                        val body = RequestBody.create(
                            "application/json".toMediaTypeOrNull(),
                            Gson().toJson(reqModel)
                        )

                        val response = apiService.saveBandwidthFile(body)
                        if (response.isSuccessful) {
                            stopwatch.stop()
                            val timeSec = stopwatch.elapsedSeconds()
                            val bytes = body.contentLength().toInt()

                            timeTakenInSec.add(timeSec)
                            receivedPacketsInBytes.add(bytes)
                            totalUploadBytes += bytes
                        }
                    } finally {
                        stopwatch.stop()
                        stopwatch.reset()
                    }
                }

                val totalBitsKb = receivedPacketsInBytes.sumOf { it.toDouble() * 8 / 1000 }
                val totalTime = timeTakenInSec.sum()
                if (totalTime > 0) {
                    uploadSpeedKbps = totalBitsKb / totalTime
                }

                receivedPacketsInBytes.clear()
                timeTakenInSec.clear()
            }
        } catch (_: Exception) {
            uploadSpeedKbps = 0.0
        }

        return BandwidthTestResult(
            downloadSpeedKbps = (downloadSpeedKbps * 100).roundToInt() / 100.0,
            uploadSpeedKbps = (uploadSpeedKbps * 100).roundToInt() / 100.0,
            totalDownloadBytes = totalDownloadBytes,
            totalUploadBytes = totalUploadBytes
        )
    }

}