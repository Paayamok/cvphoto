package com.fushengce.metadata

import android.content.Context

/** App-local bookmarks keyed by the authorized MediaStore URI. Never edits the source media. */
class LocalFavorites(context: Context) {
    private val preferences = context.applicationContext.getSharedPreferences(
        "local-favorites", Context.MODE_PRIVATE,
    )

    fun read(): Set<String> = preferences.getStringSet(KEY, emptySet()).orEmpty().toSet()

    fun toggle(uri: String): Set<String> {
        val updated = read().toMutableSet()
        if (!updated.add(uri)) updated.remove(uri)
        preferences.edit().putStringSet(KEY, updated).apply()
        return updated.toSet()
    }

    private companion object {
        const val KEY = "media-uris"
    }
}
