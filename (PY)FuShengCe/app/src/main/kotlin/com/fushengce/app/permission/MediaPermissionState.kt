package com.fushengce.app.permission

enum class MediaType {
    Image,
    Video,
}

sealed interface MediaPermissionState {
    data object NotRequested : MediaPermissionState

    data object FullAccess : MediaPermissionState

    data class PartialAccess(
        val fullyGrantedTypes: Set<MediaType>,
        val hasSelectedMediaAccess: Boolean,
    ) : MediaPermissionState

    data class Denied(
        val canRequestAgain: Boolean,
    ) : MediaPermissionState {
        val requiresSystemSettings: Boolean
            get() = !canRequestAgain
    }
}
