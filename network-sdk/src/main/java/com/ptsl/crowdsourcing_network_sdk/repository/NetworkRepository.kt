package com.ptsl.crowdsourcing_network_sdk.repository

import com.ptsl.crowdsourcing_network_sdk.data_model.BaseResponse
import com.ptsl.crowdsourcing_network_sdk.data_model.NetworkDataRequest
import com.ptsl.crowdsourcing_network_sdk.data_model.NetworkDataResponse
import com.ptsl.crowdsourcing_network_sdk.data_model.logger.LogDataWrapper
import okhttp3.RequestBody
import okhttp3.ResponseBody
import retrofit2.Response

/**
 * Interface for remote network operations.
 * Decouples the SDK from Retrofit-specific ApiService implementations.
 */
internal interface NetworkRepository {
    suspend fun postNetworkData(request: NetworkDataRequest): Response<BaseResponse<NetworkDataResponse>>
    suspend fun postNetworkDataLogs(wrapper: LogDataWrapper): Response<BaseResponse<Any>>
    suspend fun getBandwidthFile(networkType: String): Response<ResponseBody>
    suspend fun saveBandwidthFile(body: RequestBody): Response<Unit>
}
