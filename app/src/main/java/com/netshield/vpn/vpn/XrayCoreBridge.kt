package com.netshield.vpn.vpn

import android.net.VpnService
import android.os.ParcelFileDescriptor
import android.util.Log
import com.netshield.vpn.data.model.ServerConfig
import go.Seq
import libv2ray.CoreCallbackHandler
import libv2ray.CoreController
import libv2ray.Libv2ray

/**
 * Wraps AndroidLibXrayLite's CoreController (current API as of the library's
 * v26.x releases: libv2ray.NewCoreController / CoreController.StartLoop(config, tunFd)).
 *
 * IMPORTANT: this library's API has changed more than once (older builds used
 * a V2RayPoint/V2RayVPNServiceSupportsSet shape instead). If your libv2ray.aar
 * doesn't match this, diff against the current 2dust/v2rayNG app's
 * V2RayServiceManager.kt / SimpleVPNService equivalent — that's the
 * maintained reference implementation for this exact library.
 *
 * Also note: CoreCallbackHandler in the current API does NOT expose a
 * protect() callback the way older versions did. Verify against v2rayNG's
 * current service code whether protect() is still required on your AAR
 * version, or whether the core now handles socket protection internally.
 */
class XrayCoreBridge(
    private val vpnService: VpnService,
    private val onStatus: (connected: Boolean, message: String) -> Unit
) : CoreCallbackHandler, ProxyCoreBridge {

    private var controller: CoreController? = null

    init {
        Seq.setContext(vpnService.applicationContext)
        Libv2ray.initCoreEnv(vpnService.applicationContext.filesDir.absolutePath, "")
    }

    override fun start(tunFd: ParcelFileDescriptor?, config: ServerConfig) {
        val fd = tunFd?.fd ?: run {
            onStatus(false, "فایل‌دیسکریپتور TUN موجود نیست")
            return
        }
        try {
            val c = Libv2ray.newCoreController(this)
            controller = c
            c.startLoop(XrayConfigBuilder.build(config), fd)
            onStatus(true, "متصل شد")
        } catch (e: Exception) {
            Log.e("XrayCoreBridge", "failed to start xray core", e)
            onStatus(false, "خطا در اتصال به هسته: ${e.message}")
        }
    }

    override fun stop() {
        try {
            controller?.stopLoop()
        } catch (e: Exception) {
            Log.e("XrayCoreBridge", "failed to stop xray core", e)
        }
        controller = null
    }

    override fun currentLatencyMs(): Int? = try {
        controller?.measureDelay("https://www.gstatic.com/generate_204")?.toInt()
    } catch (e: Exception) {
        null
    }

    // ---- CoreCallbackHandler: callbacks the native core calls into ----

    override fun startup(): Int = 0

    override fun shutdown(): Int {
        vpnService.stopSelf()
        return 0
    }

    override fun onEmitStatus(code: Int, message: String?): Int {
        Log.d("XrayCoreBridge", "status($code): $message")
        return 0
    }
}
