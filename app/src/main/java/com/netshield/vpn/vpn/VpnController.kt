package com.netshield.vpn.vpn

import android.content.Context
import android.content.Intent
import android.net.VpnService

/** Small facade the UI layer talks to instead of touching Intents/VpnService directly. */
object VpnController {

    /** Returns an Intent to request VPN permission if needed, or null if already granted. */
    fun prepareIntent(context: Context): Intent? = VpnService.prepare(context)

    fun connect(context: Context, configId: String) {
        val intent = Intent(context, VpnTunnelService::class.java).apply {
            action = VpnTunnelService.ACTION_CONNECT
            putExtra(VpnTunnelService.EXTRA_CONFIG_ID, configId)
        }
        context.startService(intent)
    }

    fun disconnect(context: Context) {
        val intent = Intent(context, VpnTunnelService::class.java).apply {
            action = VpnTunnelService.ACTION_DISCONNECT
        }
        context.startService(intent)
    }

    fun isConnected(): Boolean = VpnTunnelService.isRunning
}
