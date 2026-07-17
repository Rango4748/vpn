package com.netshield.vpn.ui.screens

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.netshield.vpn.data.db.ConfigDatabase
import com.netshield.vpn.data.model.PanelProfile
import com.netshield.vpn.data.model.PanelType
import com.netshield.vpn.data.model.ServerConfig
import com.netshield.vpn.data.prefs.SelectedServerStore
import com.netshield.vpn.data.repository.ConfigRepository
import com.netshield.vpn.data.repository.PanelRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch
import java.util.UUID

data class LocationsUiState(
    val configs: List<ServerConfig> = emptyList(),
    val selectedConfigId: String? = null,
    val loading: Boolean = false,
    val errorMessage: String? = null,
    val infoMessage: String? = null
)

class LocationsViewModel(app: Application) : AndroidViewModel(app) {

    private val configRepo = ConfigRepository(ConfigDatabase.getInstance(app))
    private val panelRepo = PanelRepository()

    private val _uiState = MutableStateFlow(LocationsUiState())
    val uiState: StateFlow<LocationsUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            combine(
                configRepo.observeConfigs(),
                SelectedServerStore.observe(app)
            ) { list, selectedId -> list to selectedId }
                .collect { (list, selectedId) ->
                    _uiState.value = _uiState.value.copy(configs = list, selectedConfigId = selectedId)
                }
        }
    }

    /** Marks a server as the one used for the next connect — this was previously impossible
     * to do from this screen at all (tapping a row only offered delete), which was the real
     * cause of "it always connects to some other server than the one I picked". */
    fun selectServer(config: ServerConfig) {
        viewModelScope.launch { SelectedServerStore.set(getApplication(), config.id) }
    }

    /** Manual mode: user pastes a vmess/vless/trojan/ss link directly. */
    fun addManualLink(link: String) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(loading = true, errorMessage = null)
            val result = configRepo.addManualLink(link)
            _uiState.value = _uiState.value.copy(
                loading = false,
                errorMessage = result.exceptionOrNull()?.message,
                infoMessage = if (result.isSuccess) "کانفیگ اضافه شد" else null
            )
        }
    }

    /** Panel mode: user logs into a Marzban/X-UI panel and pulls their configs. */
    fun connectPanel(baseUrl: String, username: String, password: String, panelType: PanelType) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(loading = true, errorMessage = null)
            val profile = PanelProfile(
                id = UUID.randomUUID().toString(),
                name = username,
                baseUrl = baseUrl,
                username = username,
                passwordOrToken = password,
                panelType = panelType
            )
            val result = panelRepo.connectAndFetch(profile)
            result.onSuccess { configs ->
                configRepo.addConfigs(configs)
                _uiState.value = _uiState.value.copy(loading = false, infoMessage = "${configs.size} سرور از پنل دریافت شد")
            }.onFailure { e ->
                _uiState.value = _uiState.value.copy(loading = false, errorMessage = e.message)
            }
        }
    }

    /** Subscription mode: a plain base64 subscription URL (common with resellers). */
    fun addSubscription(url: String) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(loading = true, errorMessage = null)
            val result = panelRepo.fetchSubscription(url)
            result.onSuccess { configs ->
                configRepo.addConfigs(configs)
                _uiState.value = _uiState.value.copy(loading = false, infoMessage = "${configs.size} سرور دریافت شد")
            }.onFailure { e ->
                _uiState.value = _uiState.value.copy(loading = false, errorMessage = e.message)
            }
        }
    }

    fun removeConfig(config: ServerConfig) {
        viewModelScope.launch { configRepo.remove(config) }
    }

    fun clearMessages() {
        _uiState.value = _uiState.value.copy(errorMessage = null, infoMessage = null)
    }
}
