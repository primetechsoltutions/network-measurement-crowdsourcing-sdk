package com.ptsl.crowdsourcing_network_sdk.repository

import com.ptsl.crowdsourcing_network_sdk.data_model.entity.AuthEntity
import com.ptsl.crowdsourcing_network_sdk.data_model.entity.NetworkDataEntity
import com.ptsl.crowdsourcing_network_sdk.data_model.logger.EventLogModel

/**
 * Interface for local data operations.
 * Decouples the SDK from Room-specific DAO implementations.
 */
internal interface LocalCacheRepository {
    suspend fun getAuth(): AuthEntity
    suspend fun saveAuth(auth: AuthEntity)
    
    suspend fun getNetworkData(): List<NetworkDataEntity>
    suspend fun insertNetworkData(data: List<NetworkDataEntity>)
    suspend fun deleteNetworkData()
    
    suspend fun getEventLogs(): List<EventLogModel>
    suspend fun insertEventLog(log: EventLogModel)
    suspend fun deleteEventLogs()
    suspend fun getEventLogCount(): Int
}
