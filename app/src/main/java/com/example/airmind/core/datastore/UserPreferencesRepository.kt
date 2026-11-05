package com.example.airmind.core.datastore

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.floatPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

val Context.dataStore by preferencesDataStore(name = "airmind_user_preferences")

data class RemoteListeningConfig(
    val volume: Float = 0.7f,
    val balance: Float = 0.0f, // -1.0 left to +1.0 right
    val noiseReduction: Boolean = true,
    val voiceEnhancement: Boolean = true
)

class UserPreferencesRepository(private val context: Context) {

    private object PreferencesKeys {
        val KEY_VOLUME = floatPreferencesKey("listening_volume")
        val KEY_BALANCE = floatPreferencesKey("listening_balance")
        val KEY_NOISE_REDUCTION = booleanPreferencesKey("noise_reduction")
        val KEY_VOICE_ENHANCEMENT = booleanPreferencesKey("voice_enhancement")
        val KEY_ONBOARDING_DONE = booleanPreferencesKey("onboarding_done")
        val KEY_AI_VOICE_FEEDBACK = booleanPreferencesKey("ai_voice_feedback")
        val KEY_CONNECTION_ALERTS = booleanPreferencesKey("connection_alerts")
        val KEY_LAST_DEVICE_NAME = stringPreferencesKey("last_device_name")
        val KEY_DARK_MODE = booleanPreferencesKey("dark_mode")
    }

    val remoteListeningConfig: Flow<RemoteListeningConfig> = context.dataStore.data.map { prefs ->
        RemoteListeningConfig(
            volume = prefs[PreferencesKeys.KEY_VOLUME] ?: 0.7f,
            balance = prefs[PreferencesKeys.KEY_BALANCE] ?: 0.0f,
            noiseReduction = prefs[PreferencesKeys.KEY_NOISE_REDUCTION] ?: true,
            voiceEnhancement = prefs[PreferencesKeys.KEY_VOICE_ENHANCEMENT] ?: true
        )
    }

    val isOnboardingCompleted: Flow<Boolean> = context.dataStore.data.map { prefs ->
        prefs[PreferencesKeys.KEY_ONBOARDING_DONE] ?: false
    }

    val isVoiceFeedbackEnabled: Flow<Boolean> = context.dataStore.data.map { prefs ->
        prefs[PreferencesKeys.KEY_AI_VOICE_FEEDBACK] ?: true
    }

    val isConnectionAlertsEnabled: Flow<Boolean> = context.dataStore.data.map { prefs ->
        prefs[PreferencesKeys.KEY_CONNECTION_ALERTS] ?: true
    }

    suspend fun setListeningVolume(vol: Float) {
        context.dataStore.edit { it[PreferencesKeys.KEY_VOLUME] = vol.coerceIn(0f, 1f) }
    }

    suspend fun setListeningBalance(balance: Float) {
        context.dataStore.edit { it[PreferencesKeys.KEY_BALANCE] = balance.coerceIn(-1f, 1f) }
    }

    suspend fun setNoiseReduction(enabled: Boolean) {
        context.dataStore.edit { it[PreferencesKeys.KEY_NOISE_REDUCTION] = enabled }
    }

    suspend fun setVoiceEnhancement(enabled: Boolean) {
        context.dataStore.edit { it[PreferencesKeys.KEY_VOICE_ENHANCEMENT] = enabled }
    }

    suspend fun setOnboardingCompleted(completed: Boolean) {
        context.dataStore.edit { it[PreferencesKeys.KEY_ONBOARDING_DONE] = completed }
    }

    suspend fun setVoiceFeedback(enabled: Boolean) {
        context.dataStore.edit { it[PreferencesKeys.KEY_AI_VOICE_FEEDBACK] = enabled }
    }

    suspend fun setConnectionAlerts(enabled: Boolean) {
        context.dataStore.edit { it[PreferencesKeys.KEY_CONNECTION_ALERTS] = enabled }
    }
}
