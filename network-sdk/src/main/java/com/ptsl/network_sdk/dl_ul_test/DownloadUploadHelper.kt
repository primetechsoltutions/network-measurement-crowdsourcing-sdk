package com.ptsl.network_sdk.dl_ul_test

import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.ptsl.network_sdk.api.ApiService
import com.ptsl.network_sdk.data_model.BandWidth
import com.ptsl.network_sdk.data_model.BandwidthTestResult
import com.ptsl.network_sdk.data_model.BaseResponse
import com.ptsl.network_sdk.utils.*
import kotlinx.coroutines.ensureActive
import kotlin.coroutines.coroutineContext
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.RequestBody
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

        val receivedPacketsInBytes = mutableListOf<Int>()
        val timeTakenInSec = mutableListOf<Double>()

        var downloadSpeedKbps = 0.0
        var uploadSpeedKbps = 0.0

        var totalDownloadBytes = 0L
        var totalUploadBytes = 0L

        // ---------------- DOWNLOAD ----------------
        var uploadRequestBodyModel: BaseResponse<BandWidth>? = null
        val stopwatch = Stopwatch()
        repeat(retryCountDownload) {

            try {
                stopwatch.start()
                val response = apiService.getBandwidthFile(networkType)
                if (response.isSuccessful) {
                    response.body()?.let { body ->
                        stopwatch.stop()
                        val timeSec = stopwatch.elapsedSeconds()

                        // Capture the response for the upload phase if not already captured
                        if (uploadRequestBodyModel == null) {
                            val bodyString = readBodyStringCancellably(body)
                            uploadRequestBodyModel = Gson().fromJson(
                                bodyString,
                                object : TypeToken<BaseResponse<BandWidth>>() {}.type
                            )
                            // Calculate bytes from the captured string
                            val bytes = bodyString.length
                            timeTakenInSec.add(timeSec)
                            receivedPacketsInBytes.add(bytes)
                            totalDownloadBytes += bytes
                        } else {
                            val bytes = body.getTotalBytes()
                            timeTakenInSec.add(timeSec)
                            receivedPacketsInBytes.add(bytes)
                            totalDownloadBytes += bytes
                        }
                    }
                }

                val totalBitsKb = receivedPacketsInBytes.sumOf { it.toDouble() * 8 / 1000 }
                val totalTime = timeTakenInSec.sum()
                if (totalTime > 0) {
                    downloadSpeedKbps = totalBitsKb / totalTime
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

        repeat(retryCountUpload) {
        // ---------------- UPLOAD ----------------
        try {
            // Reuse the model captured during download instead of calling apiService.getBandwidthFile again
            uploadRequestBodyModel?.let { reqModel ->

                        stopwatch.start()
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

                }

                val totalBitsKb = receivedPacketsInBytes.sumOf { it.toDouble() * 8 / 1000 }
                val totalTime = timeTakenInSec.sum()
                if (totalTime > 0) {
                    uploadSpeedKbps = totalBitsKb / totalTime
                }

                receivedPacketsInBytes.clear()
                timeTakenInSec.clear()

        } catch (e: Exception) {
            if (e is kotlinx.coroutines.CancellationException) throw e
        }finally {
            coroutineContext.ensureActive()
            stopwatch.stop()
            stopwatch.reset()
        }}

        coroutineContext.ensureActive()

        return BandwidthTestResult(
            downloadSpeedKbps = (downloadSpeedKbps * 100).roundToInt() / 100.0,
            uploadSpeedKbps = (uploadSpeedKbps * 100).roundToInt() / 100.0,
            totalDownloadBytes = totalDownloadBytes,
            totalUploadBytes = totalUploadBytes
        )
    }

    private suspend fun readBodyStringCancellably(body: okhttp3.ResponseBody): String {
        val inputStream = body.byteStream()
        val bos = ByteArrayOutputStream()
        val buffer = ByteArray(2048)
        var length: Int
        try {
            while (inputStream.read(buffer).also { length = it } != -1) {
                coroutineContext.ensureActive()
                bos.write(buffer, 0, length)
            }
            return bos.toString(StandardCharsets.UTF_8.name())
        } finally {
            inputStream.close()
            bos.close()
        }
    }

}