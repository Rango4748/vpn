package com.netshield.vpn

import android.app.Application
import com.netshield.vpn.data.db.ConfigDatabase
import com.netshield.vpn.data.repository.ConfigRepository
import com.netshield.vpn.data.repository.PanelRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

class NetShieldApp : Application() {

    override fun onCreate() {
        super.onCreate()

        // "user" builds have no config-management UI at all (see LocationsScreen.kt /
        // BuildConfig.IS_ADMIN), so their server list has to come from somewhere: the
        // subscription link you (the admin) set at build time. Pull it once, silently,
        // the first time the app runs with an empty server list.
        if (!BuildConfig.IS_ADMIN && BuildConfig.DEFAULT_SUBSCRIPTION_URL.isNotBlank()) {
            CoroutineScope(Dispatchers.IO).launch {
                val repo = ConfigRepository(ConfigDatabase.getInstance(this@NetShieldApp))
                val currentList = repo.observeConfigs().first()
                if (currentList.isEmpty()) {
                    PanelRepository().fetchSubscription(BuildConfig.DEFAULT_SUBSCRIPTION_URL)
                        .onSuccess { configs -> repo.addConfigs(configs) }
                }
            }
        }
    }
}
