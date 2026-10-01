package com.offlinejournal.service.ai

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.offlinejournal.BuildConfig
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

private val Context.aiSettingsDataStore: DataStore<Preferences> by preferencesDataStore(
    name = "ai_settings"
)

class AiSettingsRepository(private val context: Context) {

    private val dataStore = context.aiSettingsDataStore

    val transcriptionMode: Flow<TranscriptionPipelineMode> = dataStore.data.map { prefs ->
        when (prefs[KEY_TRANSCRIPTION_MODE]) {
            MODE_AI -> TranscriptionPipelineMode.AI
            else -> TranscriptionPipelineMode.OFFLINE
        }
    }

    val apiKey: Flow<String?> = dataStore.data.map { prefs ->
        prefs[KEY_API_KEY]?.trim()?.takeIf { it.isNotEmpty() }
    }

    val modelIdOverride: Flow<String?> = dataStore.data.map { prefs ->
        prefs[KEY_MODEL_ID]?.trim()?.takeIf { it.isNotEmpty() }
    }

    val backendCleanupUrl: Flow<String?> = dataStore.data.map { prefs ->
        prefs[KEY_BACKEND_CLEANUP_URL]?.trim()?.takeIf { it.isNotEmpty() }
    }

    suspend fun setTranscriptionMode(mode: TranscriptionPipelineMode) {
        dataStore.edit { prefs ->
            prefs[KEY_TRANSCRIPTION_MODE] = when (mode) {
                TranscriptionPipelineMode.OFFLINE -> MODE_OFFLINE
                TranscriptionPipelineMode.AI -> MODE_AI
            }
        }
    }

    suspend fun setApiKey(key: String?) {
        dataStore.edit { prefs ->
            if (key.isNullOrBlank()) prefs.remove(KEY_API_KEY)
            else prefs[KEY_API_KEY] = key.trim()
        }
    }

    suspend fun setModelIdOverride(modelId: String?) {
        dataStore.edit { prefs ->
            if (modelId.isNullOrBlank()) prefs.remove(KEY_MODEL_ID)
            else prefs[KEY_MODEL_ID] = modelId.trim()
        }
    }

    suspend fun runtimeConfig(): AiRuntimeConfig {
        val prefs = dataStore.data.first()
        val modelFromStore = prefs[KEY_MODEL_ID]?.trim()?.takeIf { it.isNotEmpty() }
        val modelFromBuild = BuildConfig.ARVAN_MODEL.trim().takeIf { it.isNotEmpty() }
        return AiRuntimeConfig(
            baseUrl = BuildConfig.ARVAN_BASE_URL.trim().trimEnd('/'),
            modelId = modelFromStore ?: modelFromBuild.orEmpty(),
            apiKey = prefs[KEY_API_KEY]?.trim()?.takeIf { it.isNotEmpty() },
            backendCleanupUrl = prefs[KEY_BACKEND_CLEANUP_URL]?.trim()?.takeIf { it.isNotEmpty() }
                ?: BuildConfig.ARVAN_BACKEND_CLEANUP_URL.trim().takeIf { it.isNotEmpty() }
        )
    }

    companion object {
        private val KEY_TRANSCRIPTION_MODE = stringPreferencesKey("transcription_mode")
        private val KEY_API_KEY = stringPreferencesKey("arvan_api_key")
        private val KEY_MODEL_ID = stringPreferencesKey("arvan_model_id")
        private val KEY_BACKEND_CLEANUP_URL = stringPreferencesKey("backend_cleanup_url")

        private const val MODE_OFFLINE = "offline"
        private const val MODE_AI = "ai"
    }
}
