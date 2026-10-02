package com.fushengce.app

import com.fushengce.app.permission.MediaPermissionState

sealed interface AppDestination {
    data object Permission : AppDestination
    data object Home : AppDestination
    data object Gallery : AppDestination
    data class Viewer(val mediaId: Long) : AppDestination {
        init {
            require(AppDestination.isValidMediaId(mediaId)) {
                "Viewer media ID must not be negative"
            }
        }
    }

    companion object {
        const val HOME_ROUTE = "home"
        const val GALLERY_ROUTE = "gallery"
        const val VIEWER_ARGUMENT = "mediaId"
        const val VIEWER_PATTERN = "viewer/{$VIEWER_ARGUMENT}"

        fun viewerRoute(mediaId: Long): String {
            require(isValidMediaId(mediaId)) { "Viewer media ID must not be negative" }
            return "viewer/$mediaId"
        }

        fun rootFor(permissionState: MediaPermissionState): AppDestination =
            if (permissionState.hasMediaAccess) Home else Permission

        fun isValidMediaId(mediaId: Long?): Boolean = mediaId != null && mediaId >= 0
    }
}

internal val MediaPermissionState.hasMediaAccess: Boolean
    get() = this is MediaPermissionState.FullAccess || this is MediaPermissionState.PartialAccess
