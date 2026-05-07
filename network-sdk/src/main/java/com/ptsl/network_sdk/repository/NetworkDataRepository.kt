package com.ptsl.network_sdk.repository

import com.google.gson.Gson
import com.ptsl.network_sdk.data_model.MeasurementContext
import com.ptsl.network_sdk.data_model.NetworkDataRequest
import com.ptsl.network_sdk.data_model.enrichWithContext
import com.ptsl.network_sdk.utils.NetworkEventLogger
import com.ptsl.network_sdk.data_model.entity.AuthEntity
import com.ptsl.network_sdk.data_model.entity.NetworkDataEntity
import com.ptsl.network_sdk.data_model.logger.EventLogModel
import com.ptsl.network_sdk.data_model.logger.LogDataWrapper
import com.ptsl.network_sdk.data_source.NetworkLocalDataSource
import com.ptsl.network_sdk.data_source.NetworkRemoteSource
import kotlinx.coroutines.delay
import retrofit2.HttpException
import java.io.IOException

class NetworkDataRepository(
    private val localDataSource: NetworkLocalDataSource,
    private val remoteDataSource: NetworkRemoteSource
) {
    suspend fun getAuth(): AuthEntity {
        return localDataSource.getPersistentAuth() ?: AuthEntity()
    }

    suspend fun uploadNetworkData(
        auth: AuthEntity,
        newData: List<NetworkDataEntity>,
    ) {
        val cached = localDataSource.getCachedNetworkData()
        val merged = if (cached.isEmpty()) newData else newData + cached
        remoteDataSource.postNetworkData(auth, merged)
        localDataSource.clearNetworkData()
    }

    suspend fun sendLog(auth: AuthEntity, eventLogModel: EventLogModel) {
        try {
            val logsToSend = mutableListOf<EventLogModel>()
            val cachedCount = localDataSource.getNetworkDataLogEventCount()
            if (cachedCount > 0) {
                logsToSend.addAll(localDataSource.getNetworkDataLogEvent())
            }
            logsToSend.add(eventLogModel)

            remoteDataSource.sendLog(LogDataWrapper(auth, ArrayList(logsToSend)))
            if (cachedCount > 0) {
                localDataSource.deleteNetworkDataLogEvent()
            }
        } catch (_: Exception) {
            localDataSource.insertNetworkDataLogIntoDB(eventLogModel)
        }
    }


    suspend fun cacheNetworkData(data: List<NetworkDataEntity>) {
        localDataSource.cacheNetworkData(data)
    }
}
