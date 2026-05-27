package com.lechenmusic.data.repository

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.first

val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "lechen_settings")

class SettingsRepository(private val context: Context) {

    companion object {
        val SERVER_URL = stringPreferencesKey("server_url")
        val USERNAME = stringPreferencesKey("username")
        val PASSWORD = stringPreferencesKey("password")
        val THEME_MODE = stringPreferencesKey("theme_mode") // "dark" or "light"
        val CACHE_SIZE = intPreferencesKey("cache_size_gb")
        val RECENT_PLAY_IDS = stringPreferencesKey("recent_play_ids")
        // Song cache keys
        val CACHED_SONGS_JSON = stringPreferencesKey("cached_songs_json")
        val SONGS_CACHE_TIMESTAMP = longPreferencesKey("songs_cache_timestamp")
        val CACHED_SERVER_SONGS_PREFIX = stringPreferencesKey("cached_server_songs_")
    }

    val serverUrl: Flow<String> = context.dataStore.data.map { it[SERVER_URL] ?: "" }
    val username: Flow<String> = context.dataStore.data.map { it[USERNAME] ?: "" }
    val password: Flow<String> = context.dataStore.data.map { it[PASSWORD] ?: "" }
    val themeMode: Flow<String> = context.dataStore.data.map { it[THEME_MODE] ?: "dark" }
    val cacheSize: Flow<Int> = context.dataStore.data.map { it[CACHE_SIZE] ?: 4 }
    val recentPlayIds: Flow<String> = context.dataStore.data.map { it[RECENT_PLAY_IDS] ?: "" }
    val cachedSongsJson: Flow<String> = context.dataStore.data.map { it[CACHED_SONGS_JSON] ?: "" }
    val songsCacheTimestamp: Flow<Long> = context.dataStore.data.map { it[SONGS_CACHE_TIMESTAMP] ?: 0L }

    suspend fun saveLogin(serverUrl: String, username: String, password: String) {
        context.dataStore.edit { prefs ->
            prefs[SERVER_URL] = serverUrl
            prefs[USERNAME] = username
            prefs[PASSWORD] = password
        }
    }

    suspend fun clearLogin() {
        context.dataStore.edit { prefs ->
            prefs.remove(SERVER_URL)
            prefs.remove(USERNAME)
            prefs.remove(PASSWORD)
        }
    }

    suspend fun setThemeMode(mode: String) {
        context.dataStore.edit { it[THEME_MODE] = mode }
    }

    suspend fun setCacheSize(sizeGb: Int) {
        context.dataStore.edit { it[CACHE_SIZE] = sizeGb }
    }

    suspend fun addRecentPlay(songId: String) {
        context.dataStore.edit { prefs ->
            val current = prefs[RECENT_PLAY_IDS] ?: ""
            val ids = current.split(",").filter { it.isNotEmpty() }.toMutableList()
            ids.remove(songId)
            ids.add(0, songId)
            prefs[RECENT_PLAY_IDS] = ids.take(100).joinToString(",")
        }
    }

    suspend fun saveCachedSongs(json: String) {
        context.dataStore.edit { prefs ->
            prefs[CACHED_SONGS_JSON] = json
            prefs[SONGS_CACHE_TIMESTAMP] = System.currentTimeMillis()
        }
    }

    suspend fun getCachedSongs(): String {
        return context.dataStore.data.map { it[CACHED_SONGS_JSON] ?: "" }.first()
    }

    suspend fun getSongsCacheTimestamp(): Long {
        return context.dataStore.data.map { it[SONGS_CACHE_TIMESTAMP] ?: 0L }.first()
    }

    suspend fun clearSongsCache() {
        context.dataStore.edit { prefs ->
            prefs.remove(CACHED_SONGS_JSON)
            prefs.remove(SONGS_CACHE_TIMESTAMP)
        }
    }

    suspend fun isLoggedIn(): Boolean {
        val url = context.dataStore.data.map { it[SERVER_URL] ?: "" }
        return true // Will be checked in ViewModel
    }
}
