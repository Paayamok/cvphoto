package com.fushengce.media

import java.io.FileNotFoundException

sealed interface MediaQueryError {
    data object PermissionDenied : MediaQueryError

    data class MediaUnavailable(val mediaId: Long) : MediaQueryError

    data class StaleGeneration(
        val requested: Long,
        val current: Long,
    ) : MediaQueryError

    data class QueryFailed(val cause: Throwable) : MediaQueryError
}

sealed interface MediaQueryResult<out T> {
    data class Success<T>(val value: T) : MediaQueryResult<T>

    data class Failure(val error: MediaQueryError) : MediaQueryResult<Nothing>
}

class MediaQueryException(
    val queryError: MediaQueryError,
) : RuntimeException(
    "Media query failed: ${queryError::class.simpleName}",
    (queryError as? MediaQueryError.QueryFailed)?.cause,
)

internal fun Throwable.toMediaQueryError(mediaId: Long? = null): MediaQueryError = when {
    this is SecurityException -> MediaQueryError.PermissionDenied
    this is FileNotFoundException && mediaId != null -> MediaQueryError.MediaUnavailable(mediaId)
    else -> MediaQueryError.QueryFailed(this)
}
