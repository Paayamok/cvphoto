package com.fushengce.app

import androidx.compose.runtime.saveable.Saver

data class GallerySessionState(
    val firstVisibleItemIndex: Int = 0,
    val firstVisibleItemScrollOffset: Int = 0,
) {
    init {
        require(firstVisibleItemIndex >= 0) { "Visible item index must not be negative" }
        require(firstVisibleItemScrollOffset >= 0) { "Scroll offset must not be negative" }
    }

    fun updated(index: Int, offset: Int): GallerySessionState = GallerySessionState(
        firstVisibleItemIndex = index.coerceAtLeast(0),
        firstVisibleItemScrollOffset = offset.coerceAtLeast(0),
    )

    internal fun savedValues(): List<Int> = listOf(
        firstVisibleItemIndex,
        firstVisibleItemScrollOffset,
    )

    companion object {
        val stateSaver: Saver<GallerySessionState, List<Int>> = Saver(
            save = { state -> state.savedValues() },
            restore = ::fromSavedValues,
        )

        internal fun fromSavedValues(values: List<Int>): GallerySessionState =
            GallerySessionState(
                firstVisibleItemIndex = values.getOrNull(0)?.coerceAtLeast(0) ?: 0,
                firstVisibleItemScrollOffset = values.getOrNull(1)?.coerceAtLeast(0) ?: 0,
            )
    }
}
