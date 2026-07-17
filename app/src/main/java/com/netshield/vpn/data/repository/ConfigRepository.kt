package com.netshield.vpn.data.repository

import com.netshield.vpn.data.db.ConfigDatabase
import com.netshield.vpn.data.db.toEntity
import com.netshield.vpn.data.db.toModel
import com.netshield.vpn.data.model.ServerConfig
import com.netshield.vpn.data.parser.ConfigLinkParser
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

/** Single source of truth for all saved configs, regardless of where they came from. */
class ConfigRepository(private val db: ConfigDatabase) {

    fun observeConfigs(): Flow<List<ServerConfig>> =
        db.serverConfigDao().observeAll().map { list -> list.map { it.toModel() } }

    suspend fun addManualLink(rawLink: String): Result<ServerConfig> {
        val config = ConfigLinkParser.parse(rawLink)
            ?: return Result.failure(IllegalArgumentException("فرمت لینک کانفیگ پشتیبانی نمی‌شود"))
        db.serverConfigDao().insert(config.toEntity())
        return Result.success(config)
    }

    suspend fun addConfigs(configs: List<ServerConfig>) {
        db.serverConfigDao().insertAll(configs.map { it.toEntity() })
    }

    suspend fun remove(config: ServerConfig) {
        db.serverConfigDao().deleteById(config.id)
    }
}
