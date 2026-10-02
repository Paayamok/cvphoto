package com.fushengce.gallery

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.fushengce.media.MediaPageRequest
import com.fushengce.media.MediaQueryError
import com.fushengce.media.MediaQueryResult
import com.fushengce.media.MediaRepository
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class GalleryViewModel(
    private val repository: MediaRepository,
) : ViewModel() {
    private val mutableUiState = MutableStateFlow<GalleryUiState>(
        GalleryUiState.PermissionRequired,
    )
    val uiState: StateFlow<GalleryUiState> = mutableUiState.asStateFlow()

    fun setAccess(access: GalleryAccess) {
        if (access == GalleryAccess.None) {
            mutableUiState.value = GalleryUiState.PermissionRequired
            return
        }
        if (mutableUiState.value.access == access &&
            mutableUiState.value !is GalleryUiState.PermissionRequired
        ) {
            return
        }
        refresh(access)
    }

    fun refresh() {
        val access = mutableUiState.value.access
        if (access != GalleryAccess.None) refresh(access)
    }

    fun retry() = refresh()

    fun loadNextPage() {
        val current = mutableUiState.value as? GalleryUiState.Content ?: return
        val offset = current.nextOffset ?: return
        val loading = GalleryStateReducer.beginAppend(current)
        if (loading == current) return
        mutableUiState.value = loading
        requestPage(
            offset = offset,
            generation = current.generation,
            append = true,
        )
    }

    private fun refresh(access: GalleryAccess) {
        val generation = repository.refresh()
        mutableUiState.value = GalleryStateReducer.beginRefresh(
            current = mutableUiState.value,
            access = access,
            generation = generation,
        )
        requestPage(offset = 0, generation = generation, append = false)
    }

    private fun requestPage(
        offset: Int,
        generation: Long,
        append: Boolean,
    ) {
        viewModelScope.launch {
            val result = try {
                repository.loadPage(
                    MediaPageRequest(offset = offset, generation = generation),
                )
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (error: Throwable) {
                MediaQueryResult.Failure(MediaQueryError.QueryFailed(error))
            }

            mutableUiState.value = when (result) {
                is MediaQueryResult.Success -> GalleryStateReducer.pageLoaded(
                    current = mutableUiState.value,
                    page = result.value,
                    append = append,
                )

                is MediaQueryResult.Failure -> GalleryStateReducer.pageFailed(
                    current = mutableUiState.value,
                    requestGeneration = generation,
                    error = result.error,
                )
            }
        }
    }
}
