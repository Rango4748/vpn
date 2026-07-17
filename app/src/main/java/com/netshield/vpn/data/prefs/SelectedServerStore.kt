package com.netshield.vpn.data.prefs

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.selectedServerDataStore by preferencesDataStore(name = "selected_server")

/**
 * Single source of truth for "which server is currently selected for connect".
 *
 * This used to live only as in-memory state inside StatusViewModel, set once
 * from `configs.firstOrNull()` and then NEVER re-validated against the config
 * list. That's the root cause of "I delete a server, add a different one, and
 * it still tries to connect to the old one": the old ServerConfig object
 * stayed cached in memory even after being deleted from the DB. Persisting the
 * selected id here (and always resolving it against the live config list, see
 * StatusViewModel) fixes that, and also makes the selection survive process
 * death / screen navigation.
 */
object SelectedServerStore {
    private val KEY_SELECTED_ID = stringPreferencesKey("selected_config_id")

    fun observe(context: Context): Flow<String?> =
        context.selectedServerDataStore.data.map { it[KEY_SELECTED_ID] }

    suspend fun set(context: Context, configId: String) {
        context.selectedServerDataStore.edit { it[KEY_SELECTED_ID] = configId }
    }

    suspend fun clear(context: Context) {
        context.selectedServerDataStore.edit { it.remove(KEY_SELECTED_ID) }
    }
}
