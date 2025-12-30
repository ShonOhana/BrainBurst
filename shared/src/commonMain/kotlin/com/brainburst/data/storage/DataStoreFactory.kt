package com.brainburst.data.storage

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.core.Preferences
import okio.Path.Companion.toPath

/**
 * Expect function for creating platform-specific DataStore instances
 */
expect fun createDataStore(context: Any? = null): DataStore<Preferences>

/**
 * Helper for iOS to create DataStore with a path
 */
internal fun createDataStore(producePath: () -> String): DataStore<Preferences> {
    return PreferenceDataStoreFactory.createWithPath(
        produceFile = { producePath().toPath() }
    )
}

