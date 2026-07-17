package com.netshield.vpn.vpn

import com.google.gson.JsonArray
import com.google.gson.JsonObject
import com.netshield.vpn.data.model.ProtocolType
import com.netshield.vpn.data.model.ServerConfig

/**
 * Turns a [ServerConfig] into the JSON config xray-core expects (the same schema
 * used by v2rayNG / v2rayN). SOCKS_PORT is where AndroidLibXrayLite's internal
 * tun2socks sends packets that came off the TUN device; xray-core listens there
 * and forwards them out through the outbound built below.
 */
object XrayConfigBuilder {

    const val SOCKS_PORT = 10808

    fun build(config: ServerConfig): String {
        val root = JsonObject()

        root.add("log", JsonObject().apply { addProperty("loglevel", "warning") })

        // Local inbound: tun2socks connects here.
        val inbounds = JsonArray()
        inbounds.add(JsonObject().apply {
            addProperty("tag", "socks-in")
            addProperty("port", SOCKS_PORT)
            addProperty("listen", "127.0.0.1")
            addProperty("protocol", "socks")
            add("settings", JsonObject().apply {
                addProperty("udp", true)
                addProperty("auth", "noauth")
            })
            add("sniffing", JsonObject().apply {
                addProperty("enabled", true)
                add("destOverride", JsonArray().apply { add("http"); add("tls") })
            })
        })
        root.add("inbounds", inbounds)

        val outbounds = JsonArray()
        outbounds.add(buildOutbound(config))
        outbounds.add(JsonObject().apply {
            addProperty("tag", "direct")
            addProperty("protocol", "freedom")
        })
        outbounds.add(JsonObject().apply {
            addProperty("tag", "block")
            addProperty("protocol", "blackhole")
        })
        root.add("outbounds", outbounds)

        root.add("routing", JsonObject().apply {
            addProperty("domainStrategy", "AsIs")
            add("rules", JsonArray().apply {
                add(JsonObject().apply {
                    addProperty("type", "field")
                    addProperty("inboundTag", "socks-in")
                    addProperty("outboundTag", "proxy")
                })
            })
        })

        return root.toString()
    }

    private fun buildOutbound(c: ServerConfig): JsonObject = JsonObject().apply {
        addProperty("tag", "proxy")
        addProperty("protocol", c.protocol.name.lowercase())
        add("streamSettings", buildStreamSettings(c))

        val settings = JsonObject()
        when (c.protocol) {
            ProtocolType.VMESS -> settings.add("vnext", JsonArray().apply {
                add(JsonObject().apply {
                    addProperty("address", c.address)
                    addProperty("port", c.port)
                    add("users", JsonArray().apply {
                        add(JsonObject().apply {
                            addProperty("id", c.uuidOrPassword)
                            addProperty("alterId", 0)
                            addProperty("security", "auto")
                        })
                    })
                })
            })
            ProtocolType.VLESS -> settings.add("vnext", JsonArray().apply {
                add(JsonObject().apply {
                    addProperty("address", c.address)
                    addProperty("port", c.port)
                    add("users", JsonArray().apply {
                        add(JsonObject().apply {
                            addProperty("id", c.uuidOrPassword)
                            addProperty("encryption", "none")
                            if (!c.flow.isNullOrBlank()) addProperty("flow", c.flow)
                        })
                    })
                })
            })
            ProtocolType.TROJAN -> settings.add("servers", JsonArray().apply {
                add(JsonObject().apply {
                    addProperty("address", c.address)
                    addProperty("port", c.port)
                    addProperty("password", c.uuidOrPassword)
                })
            })
            ProtocolType.SHADOWSOCKS -> settings.add("servers", JsonArray().apply {
                add(JsonObject().apply {
                    addProperty("address", c.address)
                    addProperty("port", c.port)
                    addProperty("password", c.uuidOrPassword)
                    addProperty("method", c.network.ifBlank { "aes-256-gcm" })
                })
            })
            ProtocolType.WIREGUARD -> {
                // WireGuard uses a different outbound shape; not wired here since
                // xray-core's WG outbound needs privateKey/peer fields this app
                // doesn't currently collect from ConfigLinkParser.
            }
        }
        add("settings", settings)
    }

    private fun buildStreamSettings(c: ServerConfig): JsonObject = JsonObject().apply {
        addProperty("network", c.network.ifBlank { "tcp" })
        addProperty("security", c.security.ifBlank { "none" })

        if (c.security == "tls") {
            add("tlsSettings", JsonObject().apply {
                if (!c.sni.isNullOrBlank()) addProperty("serverName", c.sni)
                if (!c.alpn.isNullOrBlank()) add("alpn", JsonArray().apply { c.alpn.split(",").forEach { add(it) } })
                if (!c.fingerprint.isNullOrBlank()) addProperty("fingerprint", c.fingerprint)
            })
        } else if (c.security == "reality") {
            add("realitySettings", JsonObject().apply {
                if (!c.sni.isNullOrBlank()) addProperty("serverName", c.sni)
                if (!c.publicKey.isNullOrBlank()) addProperty("publicKey", c.publicKey)
                if (!c.shortId.isNullOrBlank()) addProperty("shortId", c.shortId)
                if (!c.fingerprint.isNullOrBlank()) addProperty("fingerprint", c.fingerprint)
            })
        }

        when (c.network) {
            "ws" -> add("wsSettings", JsonObject().apply {
                addProperty("path", c.path ?: "/")
                if (!c.host.isNullOrBlank()) add("headers", JsonObject().apply { addProperty("Host", c.host) })
            })
            "grpc" -> add("grpcSettings", JsonObject().apply {
                addProperty("serviceName", c.path ?: "")
            })
            "http", "h2" -> add("httpSettings", JsonObject().apply {
                addProperty("path", c.path ?: "/")
                if (!c.host.isNullOrBlank()) add("host", JsonArray().apply { add(c.host) })
            })
            "quic" -> add("quicSettings", JsonObject())
        }
    }
}
