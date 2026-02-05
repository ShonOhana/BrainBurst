package com.brainburst.data.repository

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import com.brainburst.domain.repository.PreferencesRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class PreferencesRepositoryImpl(
    private val dataStore: DataStore<Preferences>
) : PreferencesRepository {
    
    companion object {
        private val NOTIFICATIONS_ENABLED_KEY = booleanPreferencesKey("notifications_enabled")
        private val HAS_ASKED_FOR_NOTIFICATION_PERMISSION_KEY = booleanPreferencesKey("has_asked_for_notification_permission")
    }
    
    override fun getNotificationsEnabled(): Flow<Boolean> {
        return dataStore.data.map { preferences ->
            preferences[NOTIFICATIONS_ENABLED_KEY] ?: false
        }
    }
    
    override suspend fun setNotificationsEnabled(enabled: Boolean) {
        dataStore.edit { preferences ->
            preferences[NOTIFICATIONS_ENABLED_KEY] = enabled
        }
    }
    
    override fun getHasAskedForNotificationPermission(): Flow<Boolean> {
        return dataStore.data.map { preferences ->
            preferences[HAS_ASKED_FOR_NOTIFICATION_PERMISSION_KEY] ?: false
        }
    }
    
    override suspend fun setHasAskedForNotificationPermission(hasAsked: Boolean) {
        dataStore.edit { preferences ->
            preferences[HAS_ASKED_FOR_NOTIFICATION_PERMISSION_KEY] = hasAsked
        }
    }
}
