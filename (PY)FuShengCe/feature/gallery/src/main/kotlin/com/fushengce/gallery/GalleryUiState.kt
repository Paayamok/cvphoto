package com.fushengce.gallery

import com.fushengce.media.MediaItem
import com.fushengce.media.MediaPage
import com.fushengce.media.MediaQueryError

enum class GalleryAccess {
    None,
    Partial,
    Full,
}

sealed interface GalleryUiState {
    val access: GalleryAccess
    val generation: Long

    data object PermissionRequired : GalleryUiState {
        override val access = GalleryAccess.None
        override val generation = 0L
    }

    data class Loading(
        override val access: GalleryAccess,
        override val generation: Long,
    ) : GalleryUiState

    data class Empty(
        override val access: GalleryAccess,
        override val generation: Long,
    ) : GalleryUiState

    data class Failure(
        override val access: GalleryAccess,
        override val generation: Long,
        val message: String,
    ) : GalleryUiState

    data class Content(
        override val access: GalleryAccess,
        override val generation: Long,
        val items: List<MediaItem>,
        val nextOffset: Int?,
        val isRefreshing: Boolean = false,
        val isLoadingMore: Boolean = false,
        val errorMessage: String? = null,
    ) : GalleryUiState {
        val hasMore: Boolean
            get() = nextOffset != null
    }
}

internal object GalleryStateReducer {
    fun beginRefresh(
        current: GalleryUiState,
        access: GalleryAccess,
        generation: Long,
    ): GalleryUiState {
        if (access == GalleryAccess.None) return GalleryUiState.PermissionRequired
        return if (current is GalleryUiState.Content) {
            current.copy(
                access = access,
                generation = generation,
                isRefreshing = true,
                isLoadingMore = false,
                errorMessage = null,
            )
        } else {
            GalleryUiState.Loading(access = access, generation = generation)
        }
    }

    fun beginAppend(current: GalleryUiState.Content): GalleryUiState.Content = when {
        current.nextOffset == null || current.isRefreshing || current.isLoadingMore -> current
        else -> current.copy(isLoadingMore = true, errorMessage = null)
    }

    fun pageLoaded(
        current: GalleryUiState,
        page: MediaPage,
        append: Boolean,
    ): GalleryUiState {
        if (current.generation != page.generation) return current
        val previousItems = if (append && current is GalleryUiState.Content) {
            current.items
        } else {
            emptyList()
        }
        val items = (previousItems + page.items).distinctBy(MediaItem::id)
        return if (items.isEmpty()) {
            GalleryUiState.Empty(access = current.access, generation = page.generation)
        } else {
            GalleryUiState.Content(
                access = current.access,
                generation = page.generation,
                items = items,
                nextOffset = page.nextOffset,
            )
        }
    }

    fun pageFailed(
        current: GalleryUiState,
        requestGeneration: Long,
        error: MediaQueryError,
    ): GalleryUiState {
        if (current.generation != requestGeneration) return current
        if (error == MediaQueryError.PermissionDenied) {
            return GalleryUiState.PermissionRequired
        }

        val message = when (error) {
            MediaQueryError.PermissionDenied -> error("Handled above")
            is MediaQueryError.MediaUnavailable -> "部分影像已无法读取，请刷新重试"
            is MediaQueryError.QueryFailed -> "读取影像失败，请重试"
            is MediaQueryError.StaleGeneration -> "影像列表已更新，请重试"
        }
        return if (current is GalleryUiState.Content) {
            current.copy(
                isRefreshing = false,
                isLoadingMore = false,
                errorMessage = message,
            )
        } else {
            GalleryUiState.Failure(
                access = current.access,
                generation = current.generation,
                message = message,
            )
        }
    }
}
