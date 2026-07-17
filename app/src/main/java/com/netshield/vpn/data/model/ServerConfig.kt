package com.netshield.vpn.data.model

/** Supported proxy protocols (V2Ray/Xray family). */
enum class ProtocolType { VMESS, VLESS, TROJAN, SHADOWSOCKS, WIREGUARD }

/** Where a config came from: typed by hand, imported via URI, or synced from a panel. */
enum class ConfigSource { MANUAL, PANEL, SUBSCRIPTION }

/**
 * A single connectable server/config entry. This is the shared shape used by
 * both manually-added configs and configs pulled from a remote panel
 * (Marzban / X-UI / 3x-ui / Xray API compatible).
 */
data class ServerConfig(
    val id: String,
    val remark: String,
    val protocol: ProtocolType,
    val address: String,
    val port: Int,
    val uuidOrPassword: String,
    val network: String = "tcp",       // tcp, ws, grpc, http, quic
    val security: String = "none",      // none, tls, reality
    val path: String? = null,
    val host: String? = null,
    val sni: String? = null,
    val alpn: String? = null,
    val flow: String? = null,           // vless flow, e.g. xtls-rprx-vision
    val fingerprint: String? = null,
    val publicKey: String? = null,      // reality / wireguard
    val shortId: String? = null,        // reality
    val countryCode: String? = null,    // for the Locations screen flag/ping
    val source: ConfigSource = ConfigSource.MANUAL,
    val rawLink: String? = null,
    val latencyMs: Int? = null
)

/** A saved panel connection (Marzban/X-UI style REST API). */
data class PanelProfile(
    val id: String,
    val name: String,
    val baseUrl: String,
    val username: String,
    val passwordOrToken: String,
    val panelType: PanelType
)

enum class PanelType { MARZBAN, X_UI, THREE_X_UI, GENERIC_XRAY_API }
