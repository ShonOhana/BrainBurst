package com.brainburst.data.storage

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.preferencesDataStore

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "brainburst_preferences")

actual fun createDataStore(context: Any?): DataStore<Preferences> {
    val appContext = context as? Context
        ?: throw IllegalArgumentException("Android context is required")
    return appContext.dataStore
}




