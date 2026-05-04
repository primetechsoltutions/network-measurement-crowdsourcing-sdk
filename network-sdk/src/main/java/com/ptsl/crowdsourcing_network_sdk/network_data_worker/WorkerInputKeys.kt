package com.ptsl.crowdsourcing_network_sdk.network_data_worker

/**
 * Centralizes all WorkManager input-data key strings used when building and reading
 * [androidx.work.Data] in [NetworkDataWorker] and [NetworkCrowdSourcingDataUploader].
 * Eliminates magic strings scattered across multiple call sites.
 */
internal object WorkerInputKeys {
    const val MSISDN                    = "msisdn"
    const val INTEGRATED_APP_VERSION    = "integratedAppVersion"
    const val SDK_INITIATE_TIMESTAMP    = "sdkInitiateTimeStamp"
    const val INTEGRATED_APP_EVENT_NAME = "integratedAppEventName"
    const val SDK_VERSION               = "sdkVersion"
    const val USER_LATITUDE             = "userLatitude"
    const val USER_LONGITUDE            = "userLongitude"
}
