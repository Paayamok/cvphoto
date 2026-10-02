package com.fushengce.app

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.fushengce.app.permission.MediaPermissionGate
import com.fushengce.app.permission.MediaPermissionState
import com.fushengce.gallery.GalleryAccess
import com.fushengce.gallery.MediaGalleryFlow
import com.fushengce.tasks.TaskWorkspace

@Composable
fun FuShengCeApp(
    permissionState: MediaPermissionState,
    onRequestPermission: () -> Unit,
    onDismissPermission: () -> Unit,
    onOpenSystemSettings: () -> Unit,
    mediaRefreshToken: Int = 0,
    eveningReminderEnabled: Boolean = false,
    initialCleanupOpen: Boolean = false,
    onToggleEveningReminder: () -> Unit = {},
    incomingText: String? = null,
    incomingTaskId: String? = null,
    onConsumeIncoming: () -> Unit = {},
    onRequestTaskNotifications: () -> Unit = {},
    cleanupRequestToken: Int = 0,
    modifier: Modifier = Modifier,
) {
    var galleryOpen by rememberSaveable { mutableStateOf(initialCleanupOpen) }
    var cleanupLaunchPending by rememberSaveable { mutableStateOf(initialCleanupOpen) }
    var tasksOpen by rememberSaveable { mutableStateOf(false) }
    var taskSearchOpen by rememberSaveable { mutableStateOf(false) }
    LaunchedEffect(incomingText, incomingTaskId) {
        if (incomingText != null || incomingTaskId != null) tasksOpen = true
    }
    LaunchedEffect(cleanupRequestToken) {
        if (cleanupRequestToken > 0) {
            tasksOpen = false
            galleryOpen = true
            cleanupLaunchPending = true
        }
    }
    FuShengCeTheme {
        if (tasksOpen) {
            TaskWorkspace(
                onBack = { tasksOpen = false; taskSearchOpen = false },
                hasMediaAccess = permissionState.hasMediaAccess,
                limitedMediaAccess = permissionState is MediaPermissionState.PartialAccess,
                onRequestMediaAccess = onRequestPermission,
                onRequestNotifications = onRequestTaskNotifications,
                refreshToken = mediaRefreshToken,
                incomingText = incomingText,
                incomingTaskId = incomingTaskId,
                onConsumeIncoming = onConsumeIncoming,
                initialSearch = taskSearchOpen,
            )
        } else if (galleryOpen) {
            BackHandler(enabled = !permissionState.hasMediaAccess) { galleryOpen = false }
            MediaPermissionGate(
                state = permissionState,
                onRequestPermission = onRequestPermission,
                onDismiss = { galleryOpen = false },
                onOpenSystemSettings = onOpenSystemSettings,
                modifier = modifier,
            ) { grantedState ->
                key(cleanupRequestToken) {
                    MediaGalleryFlow(
                        access = if (grantedState is MediaPermissionState.PartialAccess) GalleryAccess.Partial else GalleryAccess.Full,
                        onRequestPermission = onRequestPermission,
                        onBackHome = {
                            cleanupLaunchPending = false
                            galleryOpen = false
                        },
                        initialCleanupOpen = cleanupLaunchPending,
                        mediaRefreshToken = mediaRefreshToken,
                        eveningReminderEnabled = eveningReminderEnabled,
                        onToggleEveningReminder = onToggleEveningReminder,
                        modifier = modifier,
                    )
                }
            }
        } else {
            Box(modifier = modifier.fillMaxSize()) {
                FirstScreenVisualPrototype()
                Column(modifier = Modifier.align(Alignment.BottomCenter).padding(bottom = 48.dp),
                    horizontalAlignment = Alignment.CenterHorizontally) {
                    Button(onClick = { galleryOpen = true }) { Text("启册 · 查看照片") }
                    Row {
                        Button(onClick = { tasksOpen = true }) { Text("工作与生活") }
                        Button(onClick = { taskSearchOpen = true; tasksOpen = true }) { Text("语音搜索") }
                    }
                }
            }
        }
    }
}
