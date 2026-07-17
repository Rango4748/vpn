package com.netshield.vpn.data.repository

import com.netshield.vpn.data.model.ConfigSource
import com.netshield.vpn.data.model.PanelProfile
import com.netshield.vpn.data.model.ServerConfig
import com.netshield.vpn.data.network.PanelApi
import com.netshield.vpn.data.parser.ConfigLinkParser

/**
 * Talks to a user-supplied panel (Marzban/X-UI/3x-UI) and turns whatever it returns
 * (a subscription link full of base64 configs, or a direct list of vmess/vless/trojan
 * links) into [ServerConfig] objects the rest of the app understands.
 */
class PanelRepository {

    suspend fun connectAndFetch(profile: PanelProfile): Result<List<ServerConfig>> {
        return try {
            val api = PanelApi.create(profile.baseUrl)
            val loginResp = api.login(profile.username, profile.passwordOrToken)
            if (!loginResp.isSuccessful) {
                return Result.failure(Exception("ورود به پنل ناموفق بود (${loginResp.code()})"))
            }
            val token = loginResp.body()?.access_token
                ?: return Result.failure(Exception("توکن دریافت نشد"))

            val userResp = api.getUser("Bearer $token", profile.username)
            if (!userResp.isSuccessful) {
                return Result.failure(Exception("دریافت اطلاعات کاربر ناموفق بود (${userResp.code()})"))
            }
            val body = userResp.body() ?: return Result.failure(Exception("پاسخ خالی از پنل"))

            val configs = (body.links ?: emptyList())
                .mapNotNull { ConfigLinkParser.parse(it) }
                .map { it.copy(source = ConfigSource.PANEL) }

            Result.success(configs)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /** Fetches & decodes a plain subscription URL (base64 blob of share-links, one per line). */
    suspend fun fetchSubscription(url: String): Result<List<ServerConfig>> {
        return try {
            val raw = okhttp3.OkHttpClient().newCall(
                okhttp3.Request.Builder().url(url).build()
            ).execute().body?.string() ?: return Result.failure(Exception("پاسخ خالی"))

            val decoded = try {
                String(android.util.Base64.decode(raw, android.util.Base64.DEFAULT))
            } catch (e: Exception) {
                raw
            }

            val configs = decoded.lines()
                .filter { it.isNotBlank() }
                .mapNotNull { ConfigLinkParser.parse(it) }
                .map { it.copy(source = ConfigSource.SUBSCRIPTION) }

            Result.success(configs)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
