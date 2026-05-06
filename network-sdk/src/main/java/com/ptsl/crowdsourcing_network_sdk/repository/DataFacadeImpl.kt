package com.ptsl.crowdsourcing_network_sdk.repository

import com.ptsl.crowdsourcing_network_sdk.data_model.NetworkDataRequest
import com.ptsl.crowdsourcing_network_sdk.data_model.entity.AuthEntity
import com.ptsl.crowdsourcing_network_sdk.data_model.entity.NetworkDataEntity
import com.ptsl.crowdsourcing_network_sdk.data_model.logger.EventLogModel
import com.ptsl.crowdsourcing_network_sdk.data_model.logger.LogDataWrapper
import okhttp3.RequestBody
import okhttp3.ResponseBody
import retrofit2.Response
import retrofit2.HttpException

internal class DataFacadeImpl(
    private val localCache: LocalCacheRepository,
    private val networkRepo: NetworkRepository
) : DataFacade {

    override suspend fun getAuth(): AuthEntity {
        return localCache.getAuth()
    }

    override suspend fun saveAuth(auth: AuthEntity) {
        localCache.saveAuth(auth)
    }

    override suspend fun syncNetworkData(auth: AuthEntity, freshData: List<NetworkDataEntity>) {
        val cachedData = localCache.getNetworkData()
        val mergedData = freshData + cachedData

        try {
            val response = networkRepo.postNetworkData(NetworkDataRequest(auth, mergedData))
            if (response.isSuccessful) {
                // If successful and there was cached data, clear the cache
                if (cachedData.isNotEmpty()) {
                    localCache.deleteNetworkData()
                }
            } else {
                throw HttpException(response)
            }
        } catch (e: Exception) {
            // On failure, cache the fresh data
            if (freshData.isNotEmpty()) {
                localCache.insertNetworkData(freshData)
            }
            throw e
        }
    }

    override suspend fun sendOrCacheEventLog(auth: AuthEntity, log: EventLogModel) {
        try {
            val cachedLogs = localCache.getEventLogs()
            val logsToSend = cachedLogs + log
            
            val response = networkRepo.postNetworkDataLogs(LogDataWrapper(auth, ArrayList(logsToSend)))
            
            if (response.isSuccessful) {
                // If successful and there were cached logs, clear them
                if (cachedLogs.isNotEmpty()) {
                    localCache.deleteEventLogs()
                }
            } else {
                // If not successful, cache the new log
                localCache.insertEventLog(log)
            }
        } catch (e: Exception) {
            localCache.insertEventLog(log)
        }
    }

    override suspend fun getBandwidthFile(networkType: String): Response<ResponseBody> {
        return networkRepo.getBandwidthFile(networkType)
    }

    override suspend fun saveBandwidthFile(body: RequestBody): Response<Unit> {
        return networkRepo.saveBandwidthFile(body)
    }
}
