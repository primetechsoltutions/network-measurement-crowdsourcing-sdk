package com.ptsl.network_sdk.data_model

import androidx.annotation.Keep
import com.google.gson.annotations.SerializedName
import com.ptsl.network_sdk.data_model.entity.AuthEntity
import com.ptsl.network_sdk.data_model.entity.NetworkDataEntity

@Keep
data class NetworkDataRequest (
    @SerializedName("networkUserModel" ) var auth : AuthEntity,
    @SerializedName("networkMeasurementRequestModel" ) var data : List<NetworkDataEntity>,
)