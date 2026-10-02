package com.fushengce.metadata

import android.content.Context

/** User-written captions remain in app storage; the underlying media is never edited. */
class LocalCaptions(context: Context) {
    private val preferences = context.applicationContext.getSharedPreferences(
        "local-captions", Context.MODE_PRIVATE,
    )

    fun read(): Map<String, String> = preferences.all.mapNotNull { (uri, value) ->
        (value as? String)?.let { uri to it }
    }.toMap()

    fun save(uri: String, caption: String): Map<String, String> {
        val normalized = caption.trim().take(MAX_CAPTION_LENGTH)
        val edit = preferences.edit()
        if (normalized.isEmpty()) edit.remove(uri) else edit.putString(uri, normalized)
        edit.apply()
        return read()
    }

    private companion object {
        const val MAX_CAPTION_LENGTH = 200
    }
}
