package com.ptsl.crowdsourcing_network_sdk.repository

import com.ptsl.crowdsourcing_network_sdk.data_model.BaseResponse
import com.ptsl.crowdsourcing_network_sdk.api.ApiService
import com.ptsl.crowdsourcing_network_sdk.data_model.NetworkDataRequest
import com.ptsl.crowdsourcing_network_sdk.data_model.NetworkDataResponse
import com.ptsl.crowdsourcing_network_sdk.data_model.logger.LogDataWrapper
import okhttp3.RequestBody
import okhttp3.ResponseBody
import retrofit2.Response

internal class NetworkRepositoryImpl(
    private val apiService: ApiService
) : NetworkRepository {

    override suspend fun postNetworkData(request: NetworkDataRequest): Response<BaseResponse<NetworkDataResponse>> {
        return apiService.postNetworkData(request)
    }

    override suspend fun postNetworkDataLogs(wrapper: LogDataWrapper): Response<BaseResponse<Any>> {
        return apiService.postNetworkDataLogs(wrapper)
    }

    override suspend fun getBandwidthFile(networkType: String): Response<ResponseBody> {
        return apiService.getBandwidthFile(networkType)
    }

    override suspend fun saveBandwidthFile(body: RequestBody): Response<Unit> {
        return apiService.saveBandwidthFile(body)
    }
}
