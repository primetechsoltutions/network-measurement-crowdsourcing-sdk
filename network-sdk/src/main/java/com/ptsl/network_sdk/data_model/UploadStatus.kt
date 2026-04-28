package com.ptsl.network_sdk.data_model

import androidx.annotation.Keep
import com.google.gson.annotations.SerializedName

@Keep
data class UploadStatus(
    @SerializedName("isSdkInit") val isSdkInit: Boolean? = null,
    @SerializedName("isLocationEnabled") val isLocationEnabled: Boolean? = null,
    @SerializedName("isPhoneStateGranted") val isPhoneStateGranted: Boolean? = null,
    @SerializedName("response") val response: String? = null
)
