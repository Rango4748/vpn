package com.netshield.vpn.data.parser

import android.util.Base64
import com.google.gson.Gson
import com.google.gson.JsonObject
import com.netshield.vpn.data.model.ConfigSource
import com.netshield.vpn.data.model.ProtocolType
import com.netshield.vpn.data.model.ServerConfig
import java.net.URI
import java.net.URLDecoder
import java.util.UUID

/**
 * Parses share links a user pastes manually (vmess://, vless://, trojan://, ss://)
 * into a [ServerConfig]. This is the "manual config" entry point requested by users
 * who don't want to connect through a panel.
 */
object ConfigLinkParser {

    private val gson = Gson()

    fun parse(rawLink: String): ServerConfig? {
        val link = rawLink.trim()
        return when {
            link.startsWith("vmess://") -> parseVmess(link)
            link.startsWith("vless://") -> parseVlessOrTrojan(link, ProtocolType.VLESS)
            link.startsWith("trojan://") -> parseVlessOrTrojan(link, ProtocolType.TROJAN)
            link.startsWith("ss://") -> parseShadowsocks(link)
            else -> null
        }
    }

    private fun parseVmess(link: String): ServerConfig? = try {
        val b64 = link.removePrefix("vmess://")
        val json = String(Base64.decode(b64, Base64.DEFAULT))
        val obj = gson.fromJson(json, JsonObject::class.java)
        ServerConfig(
            id = UUID.randomUUID().toString(),
            remark = obj.get("ps")?.asString ?: "VMess",
            protocol = ProtocolType.VMESS,
            address = obj.get("add").asString,
            port = obj.get("port").asString.toInt(),
            uuidOrPassword = obj.get("id").asString,
            network = obj.get("net")?.asString ?: "tcp",
            security = obj.get("tls")?.asString?.ifBlank { "none" } ?: "none",
            path = obj.get("path")?.asString,
            host = obj.get("host")?.asString,
            sni = obj.get("sni")?.asString,
            source = ConfigSource.MANUAL,
            rawLink = link
        )
    } catch (e: Exception) {
        null
    }

    private fun parseVlessOrTrojan(link: String, protocol: ProtocolType): ServerConfig? = try {
        val uri = URI(link)
        val query = parseQuery(uri.rawQuery)
        ServerConfig(
            id = UUID.randomUUID().toString(),
            remark = uri.fragment?.let { URLDecoder.decode(it, "UTF-8") } ?: protocol.name,
            protocol = protocol,
            address = uri.host,
            port = uri.port,
            uuidOrPassword = uri.userInfo ?: "",
            network = query["type"] ?: "tcp",
            security = query["security"] ?: "none",
            path = query["path"],
            host = query["host"],
            sni = query["sni"],
            alpn = query["alpn"],
            flow = query["flow"],
            fingerprint = query["fp"],
            publicKey = query["pbk"],
            shortId = query["sid"],
            source = ConfigSource.MANUAL,
            rawLink = link
        )
    } catch (e: Exception) {
        null
    }

    private fun parseShadowsocks(link: String): ServerConfig? = try {
        val body = link.removePrefix("ss://")
        val hashIdx = body.indexOf('#')
        val remark = if (hashIdx >= 0) URLDecoder.decode(body.substring(hashIdx + 1), "UTF-8") else "Shadowsocks"
        val main = if (hashIdx >= 0) body.substring(0, hashIdx) else body

        val atIdx = main.indexOf('@')
        val (userInfo, hostPort) = if (atIdx >= 0) {
            main.substring(0, atIdx) to main.substring(atIdx + 1)
        } else {
            val decoded = String(Base64.decode(main, Base64.DEFAULT))
            val at = decoded.indexOf('@')
            decoded.substring(0, at) to decoded.substring(at + 1)
        }

        val decodedUserInfo = try {
            String(Base64.decode(userInfo, Base64.DEFAULT))
        } catch (e: Exception) {
            userInfo
        }
        val (_, password) = decodedUserInfo.split(":", limit = 2).let { it[0] to it.getOrElse(1) { "" } }
        val (host, port) = hostPort.split(":", limit = 2).let { it[0] to it.getOrElse(1) { "8388" } }

        ServerConfig(
            id = UUID.randomUUID().toString(),
            remark = remark,
            protocol = ProtocolType.SHADOWSOCKS,
            address = host,
            port = port.substringBefore('/').toInt(),
            uuidOrPassword = password,
            source = ConfigSource.MANUAL,
            rawLink = link
        )
    } catch (e: Exception) {
        null
    }

    private fun parseQuery(raw: String?): Map<String, String> {
        if (raw.isNullOrBlank()) return emptyMap()
        return raw.split("&").mapNotNull {
            val parts = it.split("=", limit = 2)
            if (parts.size == 2) URLDecoder.decode(parts[0], "UTF-8") to URLDecoder.decode(parts[1], "UTF-8") else null
        }.toMap()
    }
}
