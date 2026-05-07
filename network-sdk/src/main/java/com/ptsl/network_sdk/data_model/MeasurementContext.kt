package com.ptsl.network_sdk.data_model

import com.ptsl.network_sdk.data_model.entity.NetworkDataEntity
import com.ptsl.network_sdk.data_model.logger.EventLogModel

data class MeasurementContext(
    val msisdn: String,
    val integratedAppVersion: String,
    val sdkInitiateTimeStamp: String,
    val integratedAppEventName: String,
    val userLatitude: Double,
    val userLongitude: Double
)

fun NetworkDataEntity.enrichWithContext(context: MeasurementContext): NetworkDataEntity {
    return this.apply {
        this.msisdn = context.msisdn
        this.integratedAppVersion = context.integratedAppVersion
        this.sdkInitiateTimeStamp = context.sdkInitiateTimeStamp
        this.integratedAppEventName = context.integratedAppEventName
        this.userLatitude = context.userLatitude
        this.userLongitude = context.userLongitude
    }
}

fun EventLogModel.enrichWithContext(context: MeasurementContext): EventLogModel {
    return this.apply {
        this.msisdn = context.msisdn
        this.integratedAppVersion = context.integratedAppVersion
        this.sdkInitiateTimeStamp = context.sdkInitiateTimeStamp
        this.integratedAppEventName = context.integratedAppEventName
        this.userLatitude = context.userLatitude
        this.userLongitude = context.userLongitude
    }
}
