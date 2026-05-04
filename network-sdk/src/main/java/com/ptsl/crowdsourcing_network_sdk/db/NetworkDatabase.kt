package com.ptsl.crowdsourcing_network_sdk.db

import androidx.room.Database
import androidx.room.RoomDatabase
import com.ptsl.crowdsourcing_network_sdk.data_model.entity.AuthEntity
import com.ptsl.crowdsourcing_network_sdk.data_model.entity.NetworkDataEntity
import com.ptsl.crowdsourcing_network_sdk.data_model.logger.EventLogModel

@Database(
    entities = [NetworkDataEntity::class, AuthEntity::class, EventLogModel::class],
    version = 11,
    exportSchema = false
)
abstract class NetworkDatabase : RoomDatabase() {
    abstract fun networkDao(): NetworkDao
}