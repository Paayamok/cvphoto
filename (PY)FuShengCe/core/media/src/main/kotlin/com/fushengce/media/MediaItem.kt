package com.fushengce.media

enum class MediaKind {
    Image,
    Video,
}

data class MediaItem(
    val id: Long,
    val uri: String,
    val displayName: String,
    val mimeType: String,
    val kind: MediaKind,
    val dateTakenMillis: Long,
    val sizeBytes: Long,
    val width: Int,
    val height: Int,
    val durationMillis: Long?,
    val relativePath: String = "",
) {
    init {
        require(id >= 0) { "Media ID must not be negative" }
        require(uri.isNotBlank()) { "Media URI must not be blank" }
        require(dateTakenMillis >= 0) { "Media date must not be negative" }
        require(sizeBytes >= 0) { "Media size must not be negative" }
        require(width >= 0 && height >= 0) { "Media dimensions must not be negative" }
        require(durationMillis == null || durationMillis >= 0) {
            "Media duration must not be negative"
        }
    }

    companion object {
        val newestFirst: Comparator<MediaItem> =
            compareByDescending<MediaItem> { it.dateTakenMillis }
                .thenByDescending { it.id }
    }
}
