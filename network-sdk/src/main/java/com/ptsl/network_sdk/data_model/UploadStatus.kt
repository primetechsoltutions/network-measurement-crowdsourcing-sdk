package com.ptsl.network_sdk.data_model

data class UploadStatus(
    val isSdkInit: Boolean? = null,
    val isLocationEnabled: Boolean? = null,
    val isPhoneStateGranted: Boolean? = null,
    val dataSaved: Boolean? = null,
    val response: String? = null
)
