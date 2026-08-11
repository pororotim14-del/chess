package com.chessassistant.domain.repository

import com.chessassistant.domain.model.AppPrefs
import kotlinx.coroutines.flow.Flow

/**
 * Persisted user preferences.
 */
interface PreferencesRepository {

    val prefs: Flow<AppPrefs>

    suspend fun update(transform: (AppPrefs) -> AppPrefs)

    suspend fun set(updated: AppPrefs)
}