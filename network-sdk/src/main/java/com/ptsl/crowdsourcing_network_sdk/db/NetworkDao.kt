package com.ptsl.crowdsourcing_network_sdk.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.ptsl.crowdsourcing_network_sdk.data_model.entity.AuthEntity
import com.ptsl.crowdsourcing_network_sdk.data_model.entity.NetworkDataEntity
import com.ptsl.crowdsourcing_network_sdk.data_model.logger.EventLogModel


@Dao
interface NetworkDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAuthData(authRequest: AuthEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertNetworkData(workEntity: List<NetworkDataEntity>): List<Long>

    @Query("SELECT * FROM networkdataentity ORDER BY date DESC, time DESC LIMIT 1")
    suspend fun getLastEntry(): NetworkDataEntity?

    @Query("SELECT * FROM authentity")
    suspend fun getPersistentAuth(): AuthEntity?

    @Query("SELECT * FROM networkdataentity")
    suspend fun getNetworkData(): List<NetworkDataEntity>?

    @Query("DELETE FROM networkdataentity")
    suspend fun deleteNetworkData()

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertNetworkDataLogIntoDB(logModel: EventLogModel): Long

    @Query("SELECT * FROM eventlogmodel")
    suspend fun getNetworkDataLogEvent(): List<EventLogModel>

    @Query("DELETE FROM eventlogmodel")
    suspend fun deleteNetworkDataLogEvent()

    @Query("SELECT COUNT(*) FROM eventlogmodel")
    suspend fun getNetworkDataLogEventCount(): Long

}