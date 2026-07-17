package com.netshield.vpn.vpn

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Intent
import android.net.VpnService
import android.os.Build
import android.os.ParcelFileDescriptor
import androidx.core.app.NotificationCompat
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import com.netshield.vpn.MainActivity
import com.netshield.vpn.data.db.ConfigDatabase
import com.netshield.vpn.data.db.toModel
import com.netshield.vpn.data.model.ServerConfig
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch

/**
 * Foreground VpnService that owns the local TUN interface and forwards traffic
 * into xray-core via [XrayCoreBridge] (AndroidLibXrayLite).
 */
class VpnTunnelService : VpnService() {

    companion object {
        const val ACTION_CONNECT = "com.netshield.vpn.CONNECT"
        const val ACTION_DISCONNECT = "com.netshield.vpn.DISCONNECT"
        const val EXTRA_CONFIG_ID = "extra_config_id"

        const val ACTION_STATE_CHANGED = "com.netshield.vpn.STATE_CHANGED"
        const val EXTRA_CONNECTED = "extra_connected"
        const val EXTRA_MESSAGE = "extra_message"

        private const val CHANNEL_ID = "netshield_vpn_channel"
        private const val NOTIF_ID = 1001

        var isRunning: Boolean = false
            private set
    }

    private var tunFd: ParcelFileDescriptor? = null
    private val serviceJob = Job()
    private val scope = CoroutineScope(Dispatchers.IO + serviceJob)
    private var proxyCore: ProxyCoreBridge? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_CONNECT -> {
                val configId = intent.getStringExtra(EXTRA_CONFIG_ID)
                startForeground(NOTIF_ID, buildNotification("در حال اتصال..."))
                connect(configId)
            }
            ACTION_DISCONNECT -> disconnect()
        }
        return START_STICKY
    }

    private fun connect(configId: String?) {
        scope.launch {
            try {
                val config = configId
                    ?.let { ConfigDatabase.getInstance(this@VpnTunnelService).serverConfigDao().getById(it) }
                    ?.toModel()
                    ?: throw IllegalStateException("کانفیگ پیدا نشد")

                establishTun()

                val bridge = XrayCoreBridge(this@VpnTunnelService) { connected, message ->
                    isRunning = connected
                    updateNotification(message)
                    broadcastState(connected, message)
                }
                proxyCore = bridge
                bridge.start(tunFd, config)
            } catch (e: Exception) {
                isRunning = false
                val msg = "خطا در اتصال: ${e.message ?: "نامشخص"}"
                updateNotification(msg)
                broadcastState(false, msg)
                tunFd?.close()
                tunFd = null
                stopForeground(STOP_FOREGROUND_REMOVE)
                stopSelf()
            }
        }
    }

    private fun establishTun() {
        val builder = Builder()
            .setSession("NetShield")
            .addAddress("10.10.0.2", 32)
            .addDnsServer("1.1.1.1")
            .addDnsServer("8.8.8.8")
            .addRoute("0.0.0.0", 0)
            .setMtu(1500)

        tunFd = builder.establish()
            ?: throw IllegalStateException("امکان ساخت رابط TUN وجود ندارد (مجوز VPN؟)")
    }

    private fun disconnect() {
        proxyCore?.stop()
        proxyCore = null
        tunFd?.close()
        tunFd = null
        isRunning = false
        broadcastState(false, "قطع شد")
        stopForeground(STOP_FOREGROUND_REMOVE)
        stopSelf()
    }

    private fun broadcastState(connected: Boolean, message: String) {
        val intent = Intent(ACTION_STATE_CHANGED)
            .putExtra(EXTRA_CONNECTED, connected)
            .putExtra(EXTRA_MESSAGE, message)
        LocalBroadcastManager.getInstance(this).sendBroadcast(intent)
    }

    private fun buildNotification(text: String): Notification {
        val manager = getSystemService(NotificationManager::class.java)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(CHANNEL_ID, "NetShield VPN", NotificationManager.IMPORTANCE_LOW)
            manager.createNotificationChannel(channel)
        }
        val pendingIntent = PendingIntent.getActivity(
            this, 0, Intent(this, MainActivity::class.java),
            PendingIntent.FLAG_IMMUTABLE
        )
        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("NetShield")
            .setContentText(text)
            .setSmallIcon(android.R.drawable.ic_lock_lock)
            .setContentIntent(pendingIntent)
            .setOngoing(true)
            .build()
    }

    private fun updateNotification(text: String) {
        val manager = getSystemService(NotificationManager::class.java)
        manager.notify(NOTIF_ID, buildNotification(text))
    }

    override fun onDestroy() {
        disconnect()
        serviceJob.cancel()
        super.onDestroy()
    }

    override fun onRevoke() {
        // User revoked VPN permission from system settings while connected.
        disconnect()
        super.onRevoke()
    }
}

/** Boundary interface to the actual xray-core engine, implemented by [XrayCoreBridge]. */
interface ProxyCoreBridge {
    fun start(tunFd: ParcelFileDescriptor?, config: ServerConfig)
    fun stop()
    fun currentLatencyMs(): Int?
}
