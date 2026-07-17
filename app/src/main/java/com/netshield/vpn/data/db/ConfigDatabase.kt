package com.netshield.vpn.data.db

import androidx.room.*
import com.netshield.vpn.data.model.ConfigSource
import com.netshield.vpn.data.model.ProtocolType
import com.netshield.vpn.data.model.ServerConfig
import kotlinx.coroutines.flow.Flow

@Entity(tableName = "server_configs")
data class ServerConfigEntity(
    @PrimaryKey val id: String,
    val remark: String,
    val protocol: String,
    val address: String,
    val port: Int,
    val uuidOrPassword: String,
    val network: String,
    val security: String,
    val path: String?,
    val host: String?,
    val sni: String?,
    val alpn: String?,
    val flow: String?,
    val fingerprint: String?,
    val publicKey: String?,
    val shortId: String?,
    val countryCode: String?,
    val source: String,
    val rawLink: String?,
    val latencyMs: Int?
)

fun ServerConfig.toEntity() = ServerConfigEntity(
    id, remark, protocol.name, address, port, uuidOrPassword, network, security,
    path, host, sni, alpn, flow, fingerprint, publicKey, shortId, countryCode,
    source.name, rawLink, latencyMs
)

fun ServerConfigEntity.toModel() = ServerConfig(
    id, remark, ProtocolType.valueOf(protocol), address, port, uuidOrPassword, network, security,
    path, host, sni, alpn, flow, fingerprint, publicKey, shortId, countryCode,
    ConfigSource.valueOf(source), rawLink, latencyMs
)

@Dao
interface ServerConfigDao {
    @Query("SELECT * FROM server_configs ORDER BY remark ASC")
    fun observeAll(): Flow<List<ServerConfigEntity>>

    @Query("SELECT * FROM server_configs WHERE id = :id LIMIT 1")
    suspend fun getById(id: String): ServerConfigEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(config: ServerConfigEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(configs: List<ServerConfigEntity>)

    @Delete
    suspend fun delete(config: ServerConfigEntity)

    @Query("DELETE FROM server_configs WHERE id = :id")
    suspend fun deleteById(id: String)
}

@Database(entities = [ServerConfigEntity::class], version = 1, exportSchema = false)
abstract class ConfigDatabase : RoomDatabase() {
    abstract fun serverConfigDao(): ServerConfigDao

    companion object {
        @Volatile private var INSTANCE: ConfigDatabase? = null

        fun getInstance(context: android.content.Context): ConfigDatabase =
            INSTANCE ?: synchronized(this) {
                INSTANCE ?: Room.databaseBuilder(
                    context.applicationContext,
                    ConfigDatabase::class.java,
                    "netshield.db"
                ).build().also { INSTANCE = it }
            }
    }
}
