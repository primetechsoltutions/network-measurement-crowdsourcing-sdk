import com.ptsl.network_sdk.data_model.entity.AuthEntity
import com.ptsl.network_sdk.db.NetworkDao

object AuthDataHelper {
    suspend fun getAuth(databaseDao: NetworkDao): AuthEntity {
        return databaseDao.getPersistentAuth() ?: AuthEntity()
    }
}