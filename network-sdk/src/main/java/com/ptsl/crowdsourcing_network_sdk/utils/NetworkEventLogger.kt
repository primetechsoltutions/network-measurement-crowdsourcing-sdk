package com.ptsl.crowdsourcing_network_sdk.utils


import android.os.Build
import com.ptsl.crowdsourcing_network_sdk.data_model.logger.EventLogModel

object NetworkEventLogger {

    fun createNetworkRequestFailedLog(
        hostAppName: String,
        eventName: String,
        errorMessage: String,
        stackTrace: String? = null,
        statusCode: Int = 0
    ): EventLogModel {
        val (os, deviceModel) = deviceInfo()
        return EventLogModel(
            logSource = "$hostAppName: $eventName",
            eventType = "Error",
            title = "Network Request Failed",
            description = "Failed to post network data",
            statusCode = statusCode,
            status = if (statusCode in 400..599) "HTTP Error" else "System Error",
            message = errorMessage,
            stackTrace = stackTrace ?: "No stack trace available",
            os = os,
            deviceModel = deviceModel
        )

    }

    fun createNetworkDataFetchFailedLog(
        hostAppName: String,
        eventName: String,
        exceptionMessage: String,
        stackTrace: String,
        statusCode: Int = 902
    ): EventLogModel {
        val (os, deviceModel) = deviceInfo()
        return EventLogModel(
            logSource = "$hostAppName: $eventName",
            eventType = "Error",
            title = "Get Network Request Failed From Exception",
            description = "Failed to get network data",
            statusCode = statusCode,
            status = "False",
            message = exceptionMessage,
            stackTrace = stackTrace,
            os = os,
            deviceModel = deviceModel
        )

    }

    fun createPermissionMissingLog(
        hostAppName: String,
        eventName: String,
        exceptionMessage: String,
        stackTrace: String?,
        statusCode: Int = 901
    ): EventLogModel {
        val (os, deviceModel) = deviceInfo()
        return EventLogModel(
            logSource = "$hostAppName: $eventName",
            eventType = "Error",
            title = "Get Network Request Failed",
            description = "Failed to get network data due to missing permissions",
            statusCode = statusCode,
            status = "False",
            message = exceptionMessage,
            stackTrace = stackTrace ?: "No stack trace available",
            os = os,
            deviceModel = deviceModel
        )
    }

    private fun deviceInfo() = Pair(
        Build.VERSION.SDK_INT.toString(),
        "${Build.MANUFACTURER} ${Build.MODEL}"
    )
}

