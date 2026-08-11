package com.chessassistant.data.repository

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.chessassistant.domain.model.AppPrefs
import com.chessassistant.domain.repository.PreferencesRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

private val Context.prefsStore: DataStore<Preferences> by preferencesDataStore(name = "app_prefs")

/**
 * Preferences backed by Jetpack DataStore, JSON-encoded with kotlinx.
 */
class DataStorePreferencesRepository(private val context: Context) : PreferencesRepository {

    private val key = stringPreferencesKey("app_prefs.json")
    private val json = Json { ignoreUnknownKeys = true }

    override val prefs: Flow<AppPrefs> =
        context.prefsStore.data.map { p ->
            p[key]?.let { raw ->
                runCatching { json.decodeFromString<AppPrefs>(raw) }.getOrNull()
            } ?: AppPrefs()
        }

    override suspend fun update(transform: (AppPrefs) -> AppPrefs) {
        context.prefsStore.edit { p ->
            val current = p[key]?.let { raw ->
                runCatching { json.decodeFromString<AppPrefs>(raw) }.getOrNull()
            } ?: AppPrefs()
            p[key] = json.encodeToString(transform(current))
        }
    }

    override suspend fun set(updated: AppPrefs) {
        context.prefsStore.edit { p ->
            p[key] = json.encodeToString(updated)
        }
    }
}