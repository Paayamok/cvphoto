package com.fushengce.metadata

import android.content.Context
import java.util.UUID

data class LocalAlbum(
    val id: String,
    val title: String,
    val mediaUris: Set<String>,
)

/** Named local collections. Membership is app metadata; source photos are never moved. */
class LocalAlbums(context: Context) {
    private val preferences = context.applicationContext.getSharedPreferences(
        "local-albums", Context.MODE_PRIVATE,
    )

    fun read(): List<LocalAlbum> = preferences.getStringSet(INDEX, emptySet()).orEmpty()
        .mapNotNull { id ->
            preferences.getString("title:$id", null)?.let { title ->
                LocalAlbum(id, title, preferences.getStringSet("media:$id", emptySet()).orEmpty().toSet())
            }
        }
        .sortedWith(compareBy(String.CASE_INSENSITIVE_ORDER) { it.title })

    fun create(rawTitle: String): LocalAlbum? {
        val title = rawTitle.trim().take(30)
        if (title.isBlank()) return null
        val existing = read()
        if (existing.any { it.title.equals(title, ignoreCase = true) }) return null
        val id = UUID.randomUUID().toString()
        preferences.edit()
            .putStringSet(INDEX, existing.mapTo(mutableSetOf()) { it.id } + id)
            .putString("title:$id", title)
            .putStringSet("media:$id", emptySet())
            .apply()
        return LocalAlbum(id, title, emptySet())
    }

    fun rename(albumId: String, rawTitle: String): List<LocalAlbum>? {
        val title = rawTitle.trim().take(30)
        val existing = read()
        if (title.isBlank() || existing.none { it.id == albumId } ||
            existing.any { it.id != albumId && it.title.equals(title, ignoreCase = true) }
        ) return null
        preferences.edit().putString("title:$albumId", title).apply()
        return read()
    }

    /** Removes only app-owned collection metadata; source MediaStore items are untouched. */
    fun delete(albumId: String): List<LocalAlbum> {
        val existing = read()
        if (existing.none { it.id == albumId }) return existing
        preferences.edit()
            .putStringSet(INDEX, existing.mapNotNull { it.id.takeIf { id -> id != albumId } }.toSet())
            .remove("title:$albumId")
            .remove("media:$albumId")
            .apply()
        return read()
    }

    fun addMembers(albumId: String, uris: Set<String>): List<LocalAlbum> {
        val album = read().firstOrNull { it.id == albumId } ?: return read()
        if (uris.isEmpty()) return read()
        preferences.edit()
            .putStringSet("media:$albumId", album.mediaUris + uris)
            .apply()
        return read()
    }

    fun removeMember(albumId: String, uri: String): List<LocalAlbum> {
        val album = read().firstOrNull { it.id == albumId } ?: return read()
        if (uri !in album.mediaUris) return read()
        preferences.edit().putStringSet("media:$albumId", album.mediaUris - uri).apply()
        return read()
    }

    fun toggleMembership(albumId: String, uri: String): List<LocalAlbum> {
        val album = read().firstOrNull { it.id == albumId } ?: return read()
        val updated = album.mediaUris.toMutableSet()
        if (!updated.add(uri)) updated.remove(uri)
        preferences.edit().putStringSet("media:$albumId", updated).apply()
        return read()
    }

    private companion object {
        const val INDEX = "album-ids"
    }
}
