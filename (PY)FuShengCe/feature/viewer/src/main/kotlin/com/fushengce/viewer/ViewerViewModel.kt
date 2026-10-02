package com.fushengce.viewer

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.fushengce.media.MediaQueryError
import com.fushengce.media.MediaQueryResult
import com.fushengce.media.MediaRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class ViewerViewModel(
    private val repository: MediaRepository,
    initialMediaId: Long,
    private val savedStateHandle: SavedStateHandle,
) : ViewModel() {
    private val restoredMediaId = resolveViewerMediaId(
        initialMediaId = initialMediaId,
        restoredMediaId = savedStateHandle[CURRENT_MEDIA_ID],
    )
    private val mutableState = MutableStateFlow<ViewerUiState>(
        ViewerUiState.Loading(restoredMediaId),
    )
    private var requestSerial = 0L

    val state: StateFlow<ViewerUiState> = mutableState.asStateFlow()

    init {
        savedStateHandle[CURRENT_MEDIA_ID] = restoredMediaId
        load(restoredMediaId, showLoading = true)
    }

    fun retry() = load(mutableState.value.mediaId, showLoading = true)

    fun select(mediaId: Long) {
        val current = mutableState.value as? ViewerUiState.Content ?: return
        val selected = ViewerStateReducer.select(current, mediaId)
        if (selected.mediaId == current.mediaId) return

        savedStateHandle[CURRENT_MEDIA_ID] = selected.mediaId
        mutableState.value = selected
        load(selected.mediaId, showLoading = false)
    }

    fun toggleControls() {
        val current = mutableState.value as? ViewerUiState.Content ?: return
        mutableState.value = ViewerStateReducer.toggleControls(current)
    }

    private fun load(mediaId: Long, showLoading: Boolean) {
        val request = ++requestSerial
        val controlsVisible = (mutableState.value as? ViewerUiState.Content)?.controlsVisible ?: true
        if (showLoading) mutableState.value = ViewerUiState.Loading(mediaId)

        viewModelScope.launch {
            val firstGeneration = repository.currentGeneration
            val first = repository.loadWindow(
                mediaId = mediaId,
                radius = WINDOW_RADIUS,
                generation = firstGeneration,
            )
            val result = if (
                first is MediaQueryResult.Failure &&
                first.error is MediaQueryError.StaleGeneration
            ) {
                repository.loadWindow(
                    mediaId = mediaId,
                    radius = WINDOW_RADIUS,
                    generation = repository.currentGeneration,
                )
            } else {
                first
            }

            if (request != requestSerial || savedStateHandle.get<Long>(CURRENT_MEDIA_ID) != mediaId) {
                return@launch
            }
            mutableState.value = ViewerStateReducer.loaded(mediaId, result, controlsVisible)
        }
    }

    private companion object {
        const val CURRENT_MEDIA_ID = "viewer.currentMediaId"
        const val WINDOW_RADIUS = 2
    }
}

internal fun resolveViewerMediaId(initialMediaId: Long, restoredMediaId: Long?): Long {
    require(initialMediaId >= 0) { "Initial media ID must not be negative" }
    return restoredMediaId?.takeIf { it >= 0 } ?: initialMediaId
}
