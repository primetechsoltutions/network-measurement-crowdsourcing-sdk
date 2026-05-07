package com.ptsl.network_sdk.data_source

import com.ptsl.network_sdk.data_model.entity.AuthEntity
import com.ptsl.network_sdk.data_model.entity.NetworkDataEntity
import com.ptsl.network_sdk.data_model.logger.EventLogModel
import com.ptsl.network_sdk.db.NetworkDao

interface NetworkLocalDataSource {
    suspend fun getPersistentAuth(): AuthEntity?
    suspend fun getCachedNetworkData(): List<NetworkDataEntity>
    suspend fun cacheNetworkData(data: List<NetworkDataEntity>)
    suspend fun clearNetworkData()
    suspend fun getNetworkDataLogEvent(): List<EventLogModel>
    suspend fun deleteNetworkDataLogEvent()
    suspend fun getNetworkDataLogEventCount(): Long
    suspend fun insertNetworkDataLogIntoDB(eventLogModel: EventLogModel)
}

class RoomNetworkLocalDataSource(
    private val dao: NetworkDao
) : NetworkLocalDataSource {
    override suspend fun getPersistentAuth(): AuthEntity? {
        return dao.getPersistentAuth()
    }

    override suspend fun getCachedNetworkData(): List<NetworkDataEntity> {
        return dao.getNetworkData() ?: emptyList()
    }

    override suspend fun cacheNetworkData(data: List<NetworkDataEntity>) {
        if (data.isNotEmpty()) {
            dao.insertNetworkData(data)
        }
    }

    override suspend fun clearNetworkData() {
        dao.deleteNetworkData()
    }

    override suspend fun getNetworkDataLogEvent(): List<EventLogModel> {
        return dao.getNetworkDataLogEvent()
    }

    override suspend fun deleteNetworkDataLogEvent() {
        dao.deleteNetworkDataLogEvent()
    }

    override suspend fun getNetworkDataLogEventCount(): Long {
        return dao.getNetworkDataLogEventCount()
    }

    override suspend fun insertNetworkDataLogIntoDB(eventLogModel: EventLogModel) {
        try {
            dao.insertNetworkDataLogIntoDB(eventLogModel)
        } catch (e: Exception) {
        }
    }
}
