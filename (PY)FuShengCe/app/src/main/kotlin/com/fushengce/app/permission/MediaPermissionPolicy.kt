package com.fushengce.app.permission

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.content.ContextCompat

class MediaPermissionPolicy(
    private val sdkInt: Int = Build.VERSION.SDK_INT,
) {
    fun permissionsToRequest(): List<String> = when {
        sdkInt >= ANDROID_14 -> listOf(
            Manifest.permission.READ_MEDIA_IMAGES,
            Manifest.permission.READ_MEDIA_VIDEO,
            READ_MEDIA_VISUAL_USER_SELECTED,
        )

        sdkInt >= ANDROID_13 -> listOf(
            Manifest.permission.READ_MEDIA_IMAGES,
            Manifest.permission.READ_MEDIA_VIDEO,
        )

        else -> listOf(Manifest.permission.READ_EXTERNAL_STORAGE)
    }

    fun currentState(
        context: Context,
        hasRequestedBefore: Boolean,
        shouldShowRationale: (String) -> Boolean,
    ): MediaPermissionState {
        val requestedPermissions = permissionsToRequest()
        return evaluate(
            hasRequestedBefore = hasRequestedBefore,
            grantedPermissions = requestedPermissions
                .filterTo(mutableSetOf()) { permission ->
                    ContextCompat.checkSelfPermission(context, permission) ==
                        PackageManager.PERMISSION_GRANTED
                },
            rationalePermissions = requestedPermissions
                .filterTo(mutableSetOf(), shouldShowRationale),
        )
    }

    fun evaluate(
        hasRequestedBefore: Boolean,
        grantedPermissions: Set<String>,
        rationalePermissions: Set<String> = emptySet(),
    ): MediaPermissionState {
        if (!hasRequestedBefore && grantedPermissions.isEmpty()) {
            return MediaPermissionState.NotRequested
        }

        if (sdkInt < ANDROID_13) {
            return if (Manifest.permission.READ_EXTERNAL_STORAGE in grantedPermissions) {
                MediaPermissionState.FullAccess
            } else {
                deniedState(rationalePermissions)
            }
        }

        val fullyGrantedTypes = buildSet {
            if (Manifest.permission.READ_MEDIA_IMAGES in grantedPermissions) add(MediaType.Image)
            if (Manifest.permission.READ_MEDIA_VIDEO in grantedPermissions) add(MediaType.Video)
        }
        val hasSelectedMediaAccess = sdkInt >= ANDROID_14 &&
            READ_MEDIA_VISUAL_USER_SELECTED in grantedPermissions

        return when {
            fullyGrantedTypes.size == MediaType.entries.size -> MediaPermissionState.FullAccess
            fullyGrantedTypes.isNotEmpty() || hasSelectedMediaAccess -> {
                MediaPermissionState.PartialAccess(
                    fullyGrantedTypes = fullyGrantedTypes,
                    hasSelectedMediaAccess = hasSelectedMediaAccess,
                )
            }

            else -> deniedState(rationalePermissions)
        }
    }

    private fun deniedState(rationalePermissions: Set<String>) = MediaPermissionState.Denied(
        canRequestAgain = permissionsToRequest().any(rationalePermissions::contains),
    )

    private companion object {
        const val ANDROID_13 = 33
        const val ANDROID_14 = 34
        const val READ_MEDIA_VISUAL_USER_SELECTED =
            "android.permission.READ_MEDIA_VISUAL_USER_SELECTED"
    }
}
