package com.fushengce.home

import android.content.Context
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.preferencesDataStore
import java.io.IOException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map

private val Context.mainRealmDataStore by preferencesDataStore(name = "main-realm")

class MainRealmStore(context: Context) {
    private val dataStore = context.applicationContext.mainRealmDataStore

    val mainRealm: Flow<MainRealm> = dataStore.data
        .catch { error ->
            if (error is IOException) {
                emit(emptyPreferences())
            } else {
                throw error
            }
        }
        .map { preferences ->
            preferences[MAIN_REALM_KEY].toMainRealm()
        }

    suspend fun save(realm: MainRealm) {
        dataStore.edit { preferences ->
            preferences[MAIN_REALM_KEY] = realm.storageValue
        }
    }

    private companion object {
        val MAIN_REALM_KEY = stringPreferencesKey("selected-main-realm")
    }
}

internal val MainRealm.storageValue: String
    get() = when (this) {
        MainRealm.Life -> "life"
        MainRealm.Affairs -> "affairs"
    }

internal fun String?.toMainRealm(): MainRealm = when (this) {
    MainRealm.Affairs.storageValue -> MainRealm.Affairs
    else -> MainRealm.Life
}
