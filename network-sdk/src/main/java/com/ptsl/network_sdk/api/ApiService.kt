package com.ptsl.network_sdk.api

import com.ptsl.network_sdk.data_model.BaseResponse
import com.ptsl.network_sdk.data_model.NetworkDataRequest
import com.ptsl.network_sdk.data_model.logger.LogDataWrapper
import okhttp3.RequestBody
import okhttp3.ResponseBody
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Query

interface ApiService {

    @POST("v903/UnifiedNetworkSDK/save-network-event-sdk-data")
    suspend fun postNetworkData(@Body request: NetworkDataRequest): BaseResponse<Any>

    @POST("v903/UnifiedNetworkSDK/save-network-sdk-logs")
    suspend fun postNetworkDataLogs(@Body request: LogDataWrapper): BaseResponse<Any>

    @GET("NetworkMesurment/GetBandwithFile")
    suspend fun getBandwidthFile(@Query("Size") networkType: String?): Response<ResponseBody>

    @POST("NetworkMesurment/SaveBandwithFile")
    suspend fun saveBandwidthFile(@Body body: RequestBody): Response<Unit>

}