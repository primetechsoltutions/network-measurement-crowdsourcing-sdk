package com.ptsl.crowdsourcing_network_sdk.repository

import com.ptsl.crowdsourcing_network_sdk.db.NetworkDao
import com.ptsl.crowdsourcing_network_sdk.data_model.entity.AuthEntity
import com.ptsl.crowdsourcing_network_sdk.data_model.entity.NetworkDataEntity
import com.ptsl.crowdsourcing_network_sdk.data_model.logger.EventLogModel

internal class LocalCacheRepositoryImpl(
    private val dao: NetworkDao
) : LocalCacheRepository {

    override suspend fun getAuth(): AuthEntity = dao.getPersistentAuth() ?: AuthEntity()

    override suspend fun saveAuth(auth: AuthEntity) {
        dao.deleteAuthData()
        dao.insertAuthData(auth)
    }

    override suspend fun getNetworkData(): List<NetworkDataEntity> = dao.getNetworkData()

    override suspend fun insertNetworkData(data: List<NetworkDataEntity>) {
        if (data.isNotEmpty()) {
            dao.insertNetworkData(data)
        }
    }

    override suspend fun deleteNetworkData() {
        dao.deleteNetworkData()
    }

    override suspend fun getEventLogs(): List<EventLogModel> = dao.getNetworkDataLogEvent()

    override suspend fun insertEventLog(log: EventLogModel) {
        dao.insertNetworkDataLogIntoDB(log)
    }

    override suspend fun deleteEventLogs() {
        dao.deleteNetworkDataLogEvent()
    }

    override suspend fun getEventLogCount(): Int = dao.getNetworkDataLogEventCount().toInt()
}
