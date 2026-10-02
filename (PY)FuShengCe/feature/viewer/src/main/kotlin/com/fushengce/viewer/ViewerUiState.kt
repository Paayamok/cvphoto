package com.fushengce.viewer

import com.fushengce.media.MediaItem
import com.fushengce.media.MediaQueryError
import com.fushengce.media.MediaQueryResult

sealed interface ViewerUiState {
    val mediaId: Long

    data class Loading(override val mediaId: Long) : ViewerUiState

    data class Content(
        override val mediaId: Long,
        val items: List<MediaItem>,
        val controlsVisible: Boolean = true,
    ) : ViewerUiState {
        init {
            require(items.any { it.id == mediaId }) { "The active media must be in the window" }
            require(items.map(MediaItem::id).distinct().size == items.size) {
                "The viewer window must not contain duplicate media IDs"
            }
        }

        val currentIndex: Int
            get() = items.indexOfFirst { it.id == mediaId }

        val currentItem: MediaItem
            get() = items[currentIndex]

        val canMovePrevious: Boolean
            get() = currentIndex > 0

        val canMoveNext: Boolean
            get() = currentIndex < items.lastIndex
    }

    data class PermissionRequired(override val mediaId: Long) : ViewerUiState

    data class Unavailable(override val mediaId: Long) : ViewerUiState

    data class Failure(
        override val mediaId: Long,
        val message: String = "暂不能读取此影，请重试",
    ) : ViewerUiState
}

internal object ViewerStateReducer {
    fun loaded(
        mediaId: Long,
        result: MediaQueryResult<List<MediaItem>>,
        controlsVisible: Boolean = true,
    ): ViewerUiState = when (result) {
        is MediaQueryResult.Success -> {
            val items = result.value.distinctBy(MediaItem::id)
            if (items.any { it.id == mediaId }) {
                ViewerUiState.Content(mediaId, items, controlsVisible)
            } else {
                ViewerUiState.Unavailable(mediaId)
            }
        }

        is MediaQueryResult.Failure -> when (result.error) {
            MediaQueryError.PermissionDenied -> ViewerUiState.PermissionRequired(mediaId)
            is MediaQueryError.MediaUnavailable -> ViewerUiState.Unavailable(mediaId)
            is MediaQueryError.QueryFailed,
            is MediaQueryError.StaleGeneration,
            -> ViewerUiState.Failure(mediaId)
        }
    }

    fun select(current: ViewerUiState.Content, mediaId: Long): ViewerUiState.Content =
        if (current.items.any { it.id == mediaId }) {
            current.copy(mediaId = mediaId)
        } else {
            current
        }

    fun toggleControls(current: ViewerUiState.Content): ViewerUiState.Content =
        current.copy(controlsVisible = !current.controlsVisible)
}
