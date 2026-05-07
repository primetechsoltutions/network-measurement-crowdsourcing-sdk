package com.ptsl.network_sdk.bandwidth

import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.ptsl.network_sdk.api.ApiService
import com.ptsl.network_sdk.data_model.BandWidth
import com.ptsl.network_sdk.data_model.BandwidthTestResult
import com.ptsl.network_sdk.data_model.BaseResponse
import com.ptsl.network_sdk.utils.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.withContext
import kotlin.coroutines.coroutineContext
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.RequestBody.Companion.toRequestBody
import java.io.ByteArrayOutputStream
import java.nio.charset.StandardCharsets
import kotlin.math.roundToInt

class DownloadUploadHelper(private val apiService: ApiService) {

    suspend fun getBandWidthSpeed(
        networkType: String = "2G",
        retryCountDownload: Int = 1,
        retryCountUpload: Int = 1,
        hasMobileInternet: Boolean = false,
        currentMnc: String?,
        activeNetworkMnc: String = "-1"
    ): BandwidthTestResult {

        if (!hasMobileInternet || currentMnc?.toIntOrNull() != activeNetworkMnc.toIntOrNull()) {
            return BandwidthTestResult(0.0, 0.0, 0, 0)
        }

        val downloadResult = performDownloadTest(networkType, retryCountDownload)
        val uploadResult =
            performUploadTest(downloadResult.uploadRequestBodyModel, retryCountUpload)

        coroutineContext.ensureActive()

        return BandwidthTestResult(
            downloadSpeedKbps = (downloadResult.speedKbps * 100).roundToInt() / 100.0,
            uploadSpeedKbps = (uploadResult.speedKbps * 100).roundToInt() / 100.0,
            totalDownloadBytes = downloadResult.totalBytes,
            totalUploadBytes = uploadResult.totalBytes
        )
    }

    private data class DownloadTestResult(
        val speedKbps: Double,
        val totalBytes: Long,
        val uploadRequestBodyModel: BaseResponse<BandWidth>?
    )

    private data class UploadTestResult(
        val speedKbps: Double,
        val totalBytes: Long
    )

    private suspend fun performDownloadTest(
        networkType: String,
        retryCount: Int
    ): DownloadTestResult {
        val receivedPacketsInBytes = mutableListOf<Int>()
        val timeTakenInSec = mutableListOf<Double>()
        var speedKbps = 0.0
        var totalBytes = 0L
        var uploadRequestBodyModel: BaseResponse<BandWidth>? = null
        val stopwatch = Stopwatch()

        repeat(retryCount) {
            try {
                stopwatch.start()
                val response = apiService.getBandwidthFile(networkType)
                if (response.isSuccessful) {
                    response.body()?.let { body ->
                        stopwatch.stop()
                        val timeSec = stopwatch.elapsedSeconds()

                        if (uploadRequestBodyModel == null) {
                            val bodyString = readResponseBodyString(body)
                            uploadRequestBodyModel = Gson().fromJson(
                                bodyString,
                                object : TypeToken<BaseResponse<BandWidth>>() {}.type
                            )
                            val bytes = bodyString.length
                            timeTakenInSec.add(timeSec)
                            receivedPacketsInBytes.add(bytes)
                            totalBytes += bytes
                        } else {
                            val bytes = body.getTotalBytes()
                            timeTakenInSec.add(timeSec)
                            receivedPacketsInBytes.add(bytes)
                            totalBytes += bytes
                        }
                    }
                }

                val totalBitsKb = receivedPacketsInBytes.sumOf { it.toDouble() * 8 / 1000 }
                val totalTime = timeTakenInSec.sum()
                if (totalTime > 0) {
                    speedKbps = totalBitsKb / totalTime
                }

                receivedPacketsInBytes.clear()
                timeTakenInSec.clear()
            } catch (e: Exception) {
                if (e is kotlinx.coroutines.CancellationException) throw e
            } finally {
                coroutineContext.ensureActive()
                stopwatch.stop()
                stopwatch.reset()
            }
        }
        return DownloadTestResult(speedKbps, totalBytes, uploadRequestBodyModel)
    }

    private suspend fun performUploadTest(
        reqModel: BaseResponse<BandWidth>?,
        retryCount: Int
    ): UploadTestResult {
        if (reqModel == null) return UploadTestResult(0.0, 0L)

        val receivedPacketsInBytes = mutableListOf<Int>()
        val timeTakenInSec = mutableListOf<Double>()
        var speedKbps = 0.0
        var totalBytes = 0L
        val stopwatch = Stopwatch()

        repeat(retryCount) {
            try {
                stopwatch.start()
                val body =
                    Gson().toJson(reqModel)
                        .toRequestBody("application/json".toMediaTypeOrNull())

                val response = apiService.saveBandwidthFile(body)
                if (response.isSuccessful) {
                    stopwatch.stop()
                    val timeSec = stopwatch.elapsedSeconds()
                    val bytes = body.contentLength().toInt()

                    timeTakenInSec.add(timeSec)
                    receivedPacketsInBytes.add(bytes)
                    totalBytes += bytes
                }

                val totalBitsKb = receivedPacketsInBytes.sumOf { it.toDouble() * 8 / 1000 }
                val totalTime = timeTakenInSec.sum()
                if (totalTime > 0) {
                    speedKbps = totalBitsKb / totalTime
                }

                receivedPacketsInBytes.clear()
                timeTakenInSec.clear()
            } catch (e: Exception) {
                if (e is kotlinx.coroutines.CancellationException) throw e
            } finally {
                coroutineContext.ensureActive()
                stopwatch.stop()
                stopwatch.reset()
            }
        }
        return UploadTestResult(speedKbps, totalBytes)
    }

    private suspend fun readResponseBodyString(body: okhttp3.ResponseBody): String =
        withContext(Dispatchers.IO) {
            body.byteStream().use { inputStream ->
                ByteArrayOutputStream().use { bos ->
                    val buffer = ByteArray(2048)
                    var length: Int
                    while (inputStream.read(buffer).also { length = it } != -1) {
                        coroutineContext.ensureActive()
                        bos.write(buffer, 0, length)
                    }
                    bos.toString(StandardCharsets.UTF_8.name())
                }
            }
        }
}