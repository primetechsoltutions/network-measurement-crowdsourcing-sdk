package com.ptsl.crowdsourcing_network_sdk.network_data_worker

import androidx.work.Data

/**
 * Value object that parses all [NetworkDataWorker] input data in one place.
 * Replaces 7+ scattered [Data.getString] / [Data.getDouble] call-sites across [NetworkDataWorker].
 *
 * Design pattern: Value Object + Factory Method ([from]).
 */
internal data class WorkerInputData(
    val msisdn: String,
    val integratedAppVersion: String,
    val sdkInitiateTimeStamp: String,
    val integratedAppEventName: String,
    val userLatitude: Double,
    val userLongitude: Double
) {
    companion object {
        fun from(inputData: Data) = WorkerInputData(
            msisdn                 = inputData.getString(WorkerInputKeys.MSISDN) ?: "",
            integratedAppVersion   = inputData.getString(WorkerInputKeys.INTEGRATED_APP_VERSION) ?: "",
            sdkInitiateTimeStamp   = inputData.getString(WorkerInputKeys.SDK_INITIATE_TIMESTAMP) ?: "",
            integratedAppEventName = inputData.getString(WorkerInputKeys.INTEGRATED_APP_EVENT_NAME) ?: "",
            userLatitude           = inputData.getDouble(WorkerInputKeys.USER_LATITUDE, 0.0),
            userLongitude          = inputData.getDouble(WorkerInputKeys.USER_LONGITUDE, 0.0)
        )
    }
}
