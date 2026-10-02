package com.fushengce.media

import android.content.ContentResolver
import android.content.ContentUris
import android.os.Bundle
import android.os.Build
import android.provider.MediaStore
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class AndroidMediaRepository(
    private val contentResolver: ContentResolver,
    private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO,
) : MediaRepository {
    private val generations = MediaGeneration()
    private val collection = MediaStore.Files.getContentUri("external")

    override val currentGeneration: Long
        get() = generations.current

    override fun refresh(): Long = generations.refresh()

    override suspend fun loadPage(request: MediaPageRequest): MediaQueryResult<MediaPage> =
        withContext(ioDispatcher) {
            if (!generations.isCurrent(request.generation)) {
                return@withContext MediaQueryResult.Failure(
                    generations.staleError(request.generation),
                )
            }

            try {
                val queried = queryRows(
                    offset = request.offset,
                    limit = request.limit + 1,
                )
                if (!generations.isCurrent(request.generation)) {
                    return@withContext MediaQueryResult.Failure(
                        generations.staleError(request.generation),
                    )
                }

                val consumedRows = queried.rows.take(request.limit)
                MediaQueryResult.Success(
                    MediaPage(
                        items = consumedRows.filterNotNull(),
                        offset = request.offset,
                        hasMore = queried.rows.size > request.limit,
                        generation = request.generation,
                        skippedItemCount = consumedRows.count { it == null },
                        consumedRowCount = consumedRows.size,
                    ),
                )
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (error: Throwable) {
                MediaQueryResult.Failure(error.toMediaQueryError())
            }
        }

    override suspend fun findById(
        mediaId: Long,
        generation: Long,
    ): MediaQueryResult<MediaItem> = withContext(ioDispatcher) {
        if (!generations.isCurrent(generation)) {
            return@withContext MediaQueryResult.Failure(generations.staleError(generation))
        }

        try {
            val item = queryRows(
                offset = 0,
                limit = 1,
                selection = "$MEDIA_SELECTION AND ${MediaStore.Files.FileColumns._ID} = ?",
                selectionArgs = MEDIA_SELECTION_ARGS + mediaId.toString(),
            ).rows.firstOrNull()
            when {
                !generations.isCurrent(generation) -> {
                    MediaQueryResult.Failure(generations.staleError(generation))
                }

                item == null -> MediaQueryResult.Failure(
                    MediaQueryError.MediaUnavailable(mediaId),
                )

                else -> MediaQueryResult.Success(item)
            }
        } catch (cancelled: CancellationException) {
            throw cancelled
        } catch (error: Throwable) {
            MediaQueryResult.Failure(error.toMediaQueryError(mediaId))
        }
    }

    override suspend fun loadWindow(
        mediaId: Long,
        radius: Int,
        generation: Long,
    ): MediaQueryResult<List<MediaItem>> {
        require(radius >= 0) { "Window radius must not be negative" }
        val before = ArrayDeque<MediaItem>(radius)
        var offset = 0

        while (true) {
            when (
                val result = loadPage(
                    MediaPageRequest(offset = offset, generation = generation),
                )
            ) {
                is MediaQueryResult.Failure -> return result
                is MediaQueryResult.Success -> {
                    val page = result.value
                    page.items.forEachIndexed { index, item ->
                        if (item.id == mediaId) {
                            val window = before.toMutableList()
                            window += item
                            window += page.items.drop(index + 1).take(radius)
                            var nextOffset = page.nextOffset
                            while (window.size < before.size + 1 + radius && nextOffset != null) {
                                when (
                                    val next = loadPage(
                                        MediaPageRequest(
                                            offset = nextOffset,
                                            generation = generation,
                                        ),
                                    )
                                ) {
                                    is MediaQueryResult.Failure -> return next
                                    is MediaQueryResult.Success -> {
                                        val needed = before.size + 1 + radius - window.size
                                        window += next.value.items.take(needed)
                                        nextOffset = next.value.nextOffset
                                    }
                                }
                            }
                            return MediaQueryResult.Success(window)
                        }

                        if (radius > 0) {
                            if (before.size == radius) before.removeFirst()
                            before.addLast(item)
                        }
                    }

                    offset = page.nextOffset
                        ?: return MediaQueryResult.Failure(
                            MediaQueryError.MediaUnavailable(mediaId),
                        )
                }
            }
        }
    }

    override suspend fun loadMedia(): List<MediaItem> {
        val generation = currentGeneration
        val allItems = mutableListOf<MediaItem>()
        var offset = 0
        while (true) {
            when (
                val result = loadPage(
                    MediaPageRequest(offset = offset, generation = generation),
                )
            ) {
                is MediaQueryResult.Failure -> throw MediaQueryException(result.error)
                is MediaQueryResult.Success -> {
                    allItems += result.value.items
                    offset = result.value.nextOffset ?: return allItems
                }
            }
        }
    }

    private fun queryRows(
        offset: Int,
        limit: Int,
        selection: String = MEDIA_SELECTION,
        selectionArgs: Array<String> = MEDIA_SELECTION_ARGS,
    ): QueriedMediaRows {
        val queryArgs = Bundle().apply {
            putString(ContentResolver.QUERY_ARG_SQL_SELECTION, selection)
            putStringArray(ContentResolver.QUERY_ARG_SQL_SELECTION_ARGS, selectionArgs)
            putStringArray(
                ContentResolver.QUERY_ARG_SORT_COLUMNS,
                arrayOf(
                    MediaStore.Images.ImageColumns.DATE_TAKEN,
                    MediaStore.Files.FileColumns._ID,
                ),
            )
            putInt(
                ContentResolver.QUERY_ARG_SORT_DIRECTION,
                ContentResolver.QUERY_SORT_DIRECTION_DESCENDING,
            )
            putInt(ContentResolver.QUERY_ARG_LIMIT, limit)
            putInt(ContentResolver.QUERY_ARG_OFFSET, offset)
        }

        val projection = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            PROJECTION + MediaStore.MediaColumns.RELATIVE_PATH
        } else PROJECTION
        val cursor = contentResolver.query(collection, projection, queryArgs, null)
            ?: error("MediaStore returned no cursor")
        return cursor.use {
            val idIndex = it.getColumnIndexOrThrow(MediaStore.Files.FileColumns._ID)
            val nameIndex = it.getColumnIndexOrThrow(MediaStore.Files.FileColumns.DISPLAY_NAME)
            val mimeIndex = it.getColumnIndexOrThrow(MediaStore.Files.FileColumns.MIME_TYPE)
            val typeIndex = it.getColumnIndexOrThrow(MediaStore.Files.FileColumns.MEDIA_TYPE)
            val takenIndex = it.getColumnIndexOrThrow(MediaStore.Images.ImageColumns.DATE_TAKEN)
            val modifiedIndex = it.getColumnIndexOrThrow(MediaStore.Files.FileColumns.DATE_MODIFIED)
            val sizeIndex = it.getColumnIndexOrThrow(MediaStore.Files.FileColumns.SIZE)
            val widthIndex = it.getColumnIndexOrThrow(MediaStore.Files.FileColumns.WIDTH)
            val heightIndex = it.getColumnIndexOrThrow(MediaStore.Files.FileColumns.HEIGHT)
            val durationIndex = it.getColumnIndexOrThrow(MediaStore.Video.VideoColumns.DURATION)
            val pathIndex = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                it.getColumnIndex(MediaStore.MediaColumns.RELATIVE_PATH)
            } else -1

            QueriedMediaRows(
                rows = buildList {
                    while (it.moveToNext()) {
                        val id = it.getLong(idIndex)
                        add(
                            MediaRow(
                                id = id,
                                displayName = it.getString(nameIndex).orEmpty(),
                                mimeType = it.getString(mimeIndex).orEmpty(),
                                mediaType = it.getInt(typeIndex),
                                dateTakenMillis = it.getLong(takenIndex),
                                dateModifiedSeconds = it.getLong(modifiedIndex),
                                sizeBytes = it.getLong(sizeIndex),
                                width = it.getInt(widthIndex),
                                height = it.getInt(heightIndex),
                                durationMillis = it.getLong(durationIndex),
                                relativePath = if (pathIndex >= 0) it.getString(pathIndex).orEmpty() else "",
                            ).toMediaItemOrNull(
                                ContentUris.withAppendedId(collection, id).toString(),
                            ),
                        )
                    }
                },
            )
        }
    }

    private companion object {
        val PROJECTION = arrayOf(
            MediaStore.Files.FileColumns._ID,
            MediaStore.Files.FileColumns.DISPLAY_NAME,
            MediaStore.Files.FileColumns.MIME_TYPE,
            MediaStore.Files.FileColumns.MEDIA_TYPE,
            MediaStore.Images.ImageColumns.DATE_TAKEN,
            MediaStore.Files.FileColumns.DATE_MODIFIED,
            MediaStore.Files.FileColumns.SIZE,
            MediaStore.Files.FileColumns.WIDTH,
            MediaStore.Files.FileColumns.HEIGHT,
            MediaStore.Video.VideoColumns.DURATION,
        )
        const val MEDIA_SELECTION = "${MediaStore.Files.FileColumns.MEDIA_TYPE} IN (?, ?)"
        val MEDIA_SELECTION_ARGS = arrayOf(
            MediaStore.Files.FileColumns.MEDIA_TYPE_IMAGE.toString(),
            MediaStore.Files.FileColumns.MEDIA_TYPE_VIDEO.toString(),
        )
    }
}

private data class QueriedMediaRows(
    val rows: List<MediaItem?>,
)

internal data class MediaRow(
    val id: Long,
    val displayName: String,
    val mimeType: String,
    val mediaType: Int,
    val dateTakenMillis: Long,
    val dateModifiedSeconds: Long,
    val sizeBytes: Long,
    val width: Int,
    val height: Int,
    val durationMillis: Long,
    val relativePath: String = "",
)

internal fun MediaRow.toMediaItemOrNull(uri: String): MediaItem? = try {
    toMediaItem(uri)
} catch (_: IllegalArgumentException) {
    null
} catch (_: IllegalStateException) {
    null
}

internal fun MediaRow.toMediaItem(uri: String): MediaItem {
    val kind = when (mediaType) {
        MediaStore.Files.FileColumns.MEDIA_TYPE_IMAGE -> MediaKind.Image
        MediaStore.Files.FileColumns.MEDIA_TYPE_VIDEO -> MediaKind.Video
        else -> error("Unsupported media type: $mediaType")
    }
    return MediaItem(
        id = id,
        uri = uri,
        displayName = displayName,
        mimeType = mimeType,
        kind = kind,
        dateTakenMillis = dateTakenMillis.takeIf { it > 0 }
            ?: (dateModifiedSeconds.coerceAtLeast(0) * 1_000),
        sizeBytes = sizeBytes.coerceAtLeast(0),
        width = width.coerceAtLeast(0),
        height = height.coerceAtLeast(0),
        durationMillis = durationMillis
            .takeIf { kind == MediaKind.Video }
            ?.coerceAtLeast(0),
        relativePath = relativePath,
    )
}
