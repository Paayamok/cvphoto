package com.fushengce.media

interface MediaRepository {
    val currentGeneration: Long

    fun refresh(): Long

    suspend fun loadPage(
        request: MediaPageRequest = MediaPageRequest(generation = currentGeneration),
    ): MediaQueryResult<MediaPage>

    suspend fun findById(
        mediaId: Long,
        generation: Long = currentGeneration,
    ): MediaQueryResult<MediaItem>

    suspend fun loadWindow(
        mediaId: Long,
        radius: Int = 2,
        generation: Long = currentGeneration,
    ): MediaQueryResult<List<MediaItem>>

    /** Compatibility path for the Phase 0 index worker; task 4 replaces its scheduling policy. */
    suspend fun loadMedia(): List<MediaItem>
}
