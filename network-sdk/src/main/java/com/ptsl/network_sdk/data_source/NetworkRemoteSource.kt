package com.ptsl.network_sdk.data_source

import com.ptsl.network_sdk.api.ApiService
import com.ptsl.network_sdk.data_model.NetworkDataRequest
import com.ptsl.network_sdk.data_model.entity.AuthEntity
import com.ptsl.network_sdk.data_model.entity.NetworkDataEntity
import com.ptsl.network_sdk.data_model.logger.EventLogModel
import com.ptsl.network_sdk.data_model.logger.LogDataWrapper
import com.ptsl.network_sdk.db.NetworkDao

interface NetworkRemoteSource {
    suspend fun postNetworkData(auth: AuthEntity, data: List<NetworkDataEntity>)
    suspend fun sendLog(request: LogDataWrapper)
}

class ApiNetworkRemoteSource(
    private val apiService: ApiService,
) : NetworkRemoteSource {
    override suspend fun postNetworkData(auth: AuthEntity, data: List<NetworkDataEntity>) {
        apiService.postNetworkData(NetworkDataRequest(auth, data))
    }

    override suspend fun sendLog(request: LogDataWrapper) {
        apiService.postNetworkDataLogs(request)
    }
}
