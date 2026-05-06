package com.ptsl.crowdsourcing_network_sdk.repository

import com.ptsl.crowdsourcing_network_sdk.data_model.entity.AuthEntity
import com.ptsl.crowdsourcing_network_sdk.data_model.entity.NetworkDataEntity
import com.ptsl.crowdsourcing_network_sdk.data_model.logger.EventLogModel
import okhttp3.RequestBody
import okhttp3.ResponseBody
import retrofit2.Response

/**
 * Facade providing a unified interface for all data operations.
 * Handles the orchestration between local caching and remote network operations,
 * ensuring separation of concerns and clean code.
 */
internal interface DataFacade {
    suspend fun getAuth(): AuthEntity
    suspend fun saveAuth(auth: AuthEntity)

    /**
     * Attempts to upload fresh data along with any previously cached data.
     * If the upload fails, it caches the fresh data locally for future retry.
     */
    suspend fun syncNetworkData(auth: AuthEntity, freshData: List<NetworkDataEntity>)

    /**
     * Attempts to upload a new event log along with any cached event logs.
     * If the upload fails, it caches the new log locally.
     */
    suspend fun sendOrCacheEventLog(auth: AuthEntity, log: EventLogModel)

    // Proxy for bandwidth operations
    suspend fun getBandwidthFile(networkType: String): Response<ResponseBody>
    suspend fun saveBandwidthFile(body: RequestBody): Response<Unit>
}
