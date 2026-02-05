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
    
    /**
     * Get whether user has been asked about notifications
     */
    fun getHasAskedForNotificationPermission(): Flow<Boolean>
    
    /**
     * Set that user has been asked about notifications
     */
    suspend fun setHasAskedForNotificationPermission(hasAsked: Boolean)
}
