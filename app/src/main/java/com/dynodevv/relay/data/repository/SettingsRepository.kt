package com.dynodevv.relay.data.repository

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "settings")

@Singleton
class SettingsRepository @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val dataStore = context.dataStore

    companion object {
        val THEME_MODE = stringPreferencesKey("theme_mode")
        val DYNAMIC_COLORS = booleanPreferencesKey("dynamic_colors")
        val DEFAULT_PROVIDER_ID = longPreferencesKey("default_provider_id")
        val DEFAULT_MODEL_ID = stringPreferencesKey("default_model_id")
        val GLOBAL_SYSTEM_PROMPT = stringPreferencesKey("global_system_prompt")
        val CAPABILITY_CACHE_LAST_SYNC = longPreferencesKey("capability_cache_last_sync")
        val CAPABILITY_CACHE_AUTO_UPDATE = booleanPreferencesKey("capability_cache_auto_update")
    }

    val themeMode: Flow<String> = dataStore.data.map { prefs ->
        prefs[THEME_MODE] ?: "system"
    }

    val dynamicColors: Flow<Boolean> = dataStore.data.map { prefs ->
        prefs[DYNAMIC_COLORS] ?: true
    }

    val defaultProviderId: Flow<Long?> = dataStore.data.map { prefs ->
        prefs[DEFAULT_PROVIDER_ID]
    }

    val defaultModelId: Flow<String?> = dataStore.data.map { prefs ->
        prefs[DEFAULT_MODEL_ID]
    }

    val globalSystemPrompt: Flow<String?> = dataStore.data.map { prefs ->
        prefs[GLOBAL_SYSTEM_PROMPT]
    }

    val capabilityCacheLastSync: Flow<Long> = dataStore.data.map { prefs ->
        prefs[CAPABILITY_CACHE_LAST_SYNC] ?: 0L
    }

    val capabilityCacheAutoUpdate: Flow<Boolean> = dataStore.data.map { prefs ->
        prefs[CAPABILITY_CACHE_AUTO_UPDATE] ?: true
    }

    suspend fun setThemeMode(mode: String) {
        dataStore.edit { prefs ->
            prefs[THEME_MODE] = mode
        }
    }

    suspend fun setDynamicColors(enabled: Boolean) {
        dataStore.edit { prefs ->
            prefs[DYNAMIC_COLORS] = enabled
        }
    }

    suspend fun setDefaultModel(providerId: Long, modelId: String) {
        dataStore.edit { prefs ->
            prefs[DEFAULT_PROVIDER_ID] = providerId
            prefs[DEFAULT_MODEL_ID] = modelId
        }
    }

    suspend fun clearDefaultModel() {
        dataStore.edit { prefs ->
            prefs.remove(DEFAULT_PROVIDER_ID)
            prefs.remove(DEFAULT_MODEL_ID)
        }
    }

    suspend fun setGlobalSystemPrompt(prompt: String?) {
        dataStore.edit { prefs ->
            if (prompt != null) {
                prefs[GLOBAL_SYSTEM_PROMPT] = prompt
            } else {
                prefs.remove(GLOBAL_SYSTEM_PROMPT)
            }
        }
    }

    suspend fun setCapabilityCacheLastSync(timestamp: Long) {
        dataStore.edit { prefs ->
            prefs[CAPABILITY_CACHE_LAST_SYNC] = timestamp
        }
    }

    suspend fun setCapabilityCacheAutoUpdate(enabled: Boolean) {
        dataStore.edit { prefs ->
            prefs[CAPABILITY_CACHE_AUTO_UPDATE] = enabled
        }
    }
}
