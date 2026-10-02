package com.fushengce.media

import java.util.concurrent.atomic.AtomicLong

const val MAX_MEDIA_PAGE_SIZE = 60

data class MediaPageRequest(
    val offset: Int = 0,
    val limit: Int = MAX_MEDIA_PAGE_SIZE,
    val generation: Long,
) {
    init {
        require(offset >= 0) { "Page offset must not be negative" }
        require(limit in 1..MAX_MEDIA_PAGE_SIZE) {
            "Page limit must be between 1 and $MAX_MEDIA_PAGE_SIZE"
        }
        require(generation >= 0) { "Page generation must not be negative" }
    }
}

data class MediaPage(
    val items: List<MediaItem>,
    val offset: Int,
    val hasMore: Boolean,
    val generation: Long,
    val skippedItemCount: Int = 0,
    val consumedRowCount: Int = items.size + skippedItemCount,
) {
    init {
        require(items.size <= MAX_MEDIA_PAGE_SIZE) { "A media page cannot exceed 60 items" }
        require(skippedItemCount >= 0) { "Skipped item count must not be negative" }
        require(consumedRowCount in 0..MAX_MEDIA_PAGE_SIZE) {
            "Consumed row count must be between 0 and $MAX_MEDIA_PAGE_SIZE"
        }
        require(items.size + skippedItemCount == consumedRowCount) {
            "Every consumed row must be returned or counted as skipped"
        }
        require(!hasMore || consumedRowCount > 0) {
            "A continuing page must consume at least one source row"
        }
        require(offset >= 0) { "Page offset must not be negative" }
        require(generation >= 0) { "Page generation must not be negative" }
    }

    val nextOffset: Int?
        get() = if (hasMore) offset + consumedRowCount else null
}

internal class MediaGeneration {
    private val value = AtomicLong(0)

    val current: Long
        get() = value.get()

    fun refresh(): Long = value.incrementAndGet()

    fun isCurrent(generation: Long): Boolean = generation == current

    fun staleError(generation: Long) = MediaQueryError.StaleGeneration(
        requested = generation,
        current = current,
    )
}
