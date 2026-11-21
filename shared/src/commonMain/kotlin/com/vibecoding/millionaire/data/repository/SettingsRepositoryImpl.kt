package com.vibecoding.millionaire.data.repository

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.longPreferencesKey
import com.vibecoding.millionaire.domain.repository.AppSettings
import com.vibecoding.millionaire.domain.repository.SettingsRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.map

/** The `DataStore` instance is created by the platform (see `PlatformContext.settingsDataStore`). */
class SettingsRepositoryImpl(
    private val dataStore: DataStore<Preferences>,
) : SettingsRepository {

    override val settings: Flow<AppSettings> = dataStore.data.map { preferences ->
        AppSettings(
            soundEnabled = preferences[SOUND_ENABLED] ?: true,
            vibrationEnabled = preferences[VIBRATION_ENABLED] ?: true,
        )
    }

    override suspend fun setSoundEnabled(enabled: Boolean) {
        dataStore.edit { it[SOUND_ENABLED] = enabled }
    }

    override suspend fun setVibrationEnabled(enabled: Boolean) {
        dataStore.edit { it[VIBRATION_ENABLED] = enabled }
    }

    override suspend fun lastPlayerId(): Long? = dataStore.data.map { it[LAST_PLAYER_ID] }.firstOrNull()

    override suspend fun setLastPlayerId(id: Long?) {
        dataStore.edit { preferences ->
            if (id == null) preferences.remove(LAST_PLAYER_ID) else preferences[LAST_PLAYER_ID] = id
        }
    }

    private companion object {
        val SOUND_ENABLED = booleanPreferencesKey("sound_enabled")
        val VIBRATION_ENABLED = booleanPreferencesKey("vibration_enabled")
        val LAST_PLAYER_ID = longPreferencesKey("last_player_id")
    }
}