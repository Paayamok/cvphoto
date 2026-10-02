package com.fushengce.viewer

internal enum class PlayerLifecycleEvent {
    Create,
    MediaChanged,
    AppBackgrounded,
    Destroyed,
    PlaybackFailed,
}

internal data class PlayerLifecycleDecision(
    val createPlayer: Boolean = false,
    val pause: Boolean = false,
    val release: Boolean = false,
    val showError: Boolean = false,
)

internal fun playerLifecycleDecision(event: PlayerLifecycleEvent): PlayerLifecycleDecision =
    when (event) {
        PlayerLifecycleEvent.Create -> PlayerLifecycleDecision(createPlayer = true)
        PlayerLifecycleEvent.MediaChanged,
        PlayerLifecycleEvent.AppBackgrounded,
        PlayerLifecycleEvent.Destroyed,
        -> PlayerLifecycleDecision(pause = true, release = true)

        PlayerLifecycleEvent.PlaybackFailed -> PlayerLifecycleDecision(
            pause = true,
            showError = true,
        )
    }
