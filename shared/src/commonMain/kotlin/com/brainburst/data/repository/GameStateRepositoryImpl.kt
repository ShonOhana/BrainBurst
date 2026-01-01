package com.brainburst.data.repository

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import com.brainburst.domain.model.SavedGameState
import com.brainburst.domain.repository.GameStateRepository
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

class GameStateRepositoryImpl(
    private val dataStore: DataStore<Preferences>,
    private val json: Json
) : GameStateRepository {
    
    override suspend fun saveGameState(state: SavedGameState) {
        val key = stringPreferencesKey("game_state_${state.puzzleId}")
        val jsonString = json.encodeToString(state)
        
        dataStore.edit { preferences ->
            preferences[key] = jsonString
        }
    }
    
    override suspend fun loadGameState(puzzleId: String): SavedGameState? {
        val key = stringPreferencesKey("game_state_$puzzleId")
        
        return dataStore.data.map { preferences ->
            preferences[key]?.let { jsonString ->
                try {
                    json.decodeFromString<SavedGameState>(jsonString)
                } catch (e: Exception) {
                    // If deserialization fails, return null (corrupted data)
                    null
                }
            }
        }.first()
    }
    
    override suspend fun clearGameState(puzzleId: String) {
        val key = stringPreferencesKey("game_state_$puzzleId")
        
        dataStore.edit { preferences ->
            preferences.remove(key)
        }
    }
    
    override suspend fun clearAllGameStates() {
        dataStore.edit { preferences ->
            val keysToRemove = preferences.asMap().keys.filter { 
                it.name.startsWith("game_state_")
            }
            keysToRemove.forEach { key ->
                preferences.remove(key)
            }
        }
    }
}


