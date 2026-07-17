package com.netshield.vpn.ui.screens

import android.app.Application
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import androidx.core.content.ContextCompat
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.netshield.vpn.data.db.ConfigDatabase
import com.netshield.vpn.data.model.ServerConfig
import com.netshield.vpn.data.repository.ConfigRepository
import com.netshield.vpn.vpn.VpnController
import com.netshield.vpn.vpn.VpnTunnelService
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

enum class ConnectionState { DISCONNECTED, CONNECTING, CONNECTED }

data class StatusUiState(
    val connectionState: ConnectionState = ConnectionState.DISCONNECTED,
    val selectedConfig: ServerConfig? = null,
    val configs: List<ServerConfig> = emptyList(),
    val elapsedSeconds: Long = 0,
    val statusMessage: String? = null
)

class StatusViewModel(app: Application) : AndroidViewModel(app) {

    private val repo = ConfigRepository(ConfigDatabase.getInstance(app))
    private val _uiState = MutableStateFlow(StatusUiState())
    val uiState: StateFlow<StatusUiState> = _uiState.asStateFlow()

    private val stateReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            val connected = intent?.getBooleanExtra(VpnTunnelService.EXTRA_CONNECTED, false) ?: false
            val message = intent?.getStringExtra(VpnTunnelService.EXTRA_MESSAGE)
            _uiState.value = _uiState.value.copy(
                connectionState = if (connected) ConnectionState.CONNECTED else ConnectionState.DISCONNECTED,
                statusMessage = message
            )
        }
    }

    init {
        viewModelScope.launch {
            repo.observeConfigs().collect { configs ->
                _uiState.value = _uiState.value.copy(
                    configs = configs,
                    selectedConfig = _uiState.value.selectedConfig ?: configs.firstOrNull()
                )
            }
        }
        ContextCompat.registerReceiver(
            app,
            stateReceiver,
            IntentFilter(VpnTunnelService.ACTION_STATE_CHANGED),
            ContextCompat.RECEIVER_NOT_EXPORTED
        )
    }

    override fun onCleared() {
        getApplication<Application>().unregisterReceiver(stateReceiver)
        super.onCleared()
    }

    fun selectConfig(config: ServerConfig) {
        _uiState.value = _uiState.value.copy(selectedConfig = config)
    }

    fun toggleConnection() {
        val context = getApplication<android.app.Application>()
        val current = _uiState.value

        if (current.connectionState != ConnectionState.DISCONNECTED) {
            VpnController.disconnect(context)
            _uiState.value = current.copy(connectionState = ConnectionState.DISCONNECTED, elapsedSeconds = 0)
            return
        }

        val config = current.selectedConfig ?: return
        val permissionIntent = VpnController.prepareIntent(context)
        if (permissionIntent != null) {
            // UI layer (MainActivity) must launch this intent for the user to grant VPN
            // permission before calling connectAfterPermission(). See MainActivity.
            pendingConfigId = config.id
            _pendingPermissionRequest.value = permissionIntent
            return
        }

        connectNow(config.id)
    }

    private var pendingConfigId: String? = null
    private val _pendingPermissionRequest = MutableStateFlow<android.content.Intent?>(null)
    val pendingPermissionRequest: StateFlow<android.content.Intent?> = _pendingPermissionRequest.asStateFlow()

    fun onPermissionGranted() {
        _pendingPermissionRequest.value = null
        pendingConfigId?.let { connectNow(it) }
    }

    fun onPermissionDenied() {
        _pendingPermissionRequest.value = null
        pendingConfigId = null
    }

    private fun connectNow(configId: String) {
        val context = getApplication<android.app.Application>()
        _uiState.value = _uiState.value.copy(connectionState = ConnectionState.CONNECTING)
        VpnController.connect(context, configId)
        // Real connected/failed state now arrives via VpnTunnelService's broadcast
        // (handled in stateReceiver above), not assumed here.
    }
}
