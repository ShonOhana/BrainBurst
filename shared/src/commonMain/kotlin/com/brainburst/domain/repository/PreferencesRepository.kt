package com.brainburst.domain.repository

import kotlinx.coroutines.flow.Flow

/**
 * Repository for managing user preferences
 */
interface PreferencesRepository {
    /**
     * Get notification enabled status as a flow
     */
    fun getNotificationsEnabled(): Flow<Boolean>
    
    /**
     * Set notification enabled status
     */
    suspend fun setNotificationsEnabled(enabled: Boolean)
}
