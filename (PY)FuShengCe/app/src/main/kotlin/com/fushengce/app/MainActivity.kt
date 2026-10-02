package com.fushengce.app

import android.Manifest
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.content.pm.PackageManager
import android.os.Bundle
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.setValue
import androidx.core.app.ActivityCompat
import androidx.lifecycle.lifecycleScope
import com.fushengce.app.permission.MediaPermissionPolicy
import com.fushengce.app.permission.MediaPermissionState
import com.fushengce.background.MediaScanScheduler
import com.fushengce.background.EveningReminderScheduler
import com.fushengce.background.EXTRA_OPEN_CLEANUP
import com.fushengce.background.EXTRA_TASK_ID
import com.fushengce.background.TaskReminders
import kotlinx.coroutines.CancellationException
import android.util.Log
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {
    private val mediaPermissionPolicy = MediaPermissionPolicy()
    private var previouslyHadMediaAccess = false
    private var permissionState by mutableStateOf<MediaPermissionState>(
        MediaPermissionState.NotRequested,
    )
    private var hasRequestedBefore = false
    private var mediaRefreshToken by mutableIntStateOf(0)
    private var eveningReminderEnabled by mutableStateOf(false)
    private var incomingText by mutableStateOf<String?>(null)
    private var incomingTaskId by mutableStateOf<String?>(null)
    private var cleanupRequestToken by mutableIntStateOf(0)
    private val taskNotificationLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission(),
    ) { mediaRefreshToken++; reconcileTaskReminders() }

    private val permissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions(),
    ) {
        markPermissionRequested()
        refreshPermissionState()
    }

    private val notificationPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission(),
    ) { granted ->
        EveningReminderScheduler.setEnabled(applicationContext, granted)
        eveningReminderEnabled = granted
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        hasRequestedBefore = savedInstanceState?.getBoolean(HAS_REQUESTED_BEFORE_KEY)
            ?: getPreferences(MODE_PRIVATE).getBoolean(HAS_REQUESTED_BEFORE_KEY, false)
        previouslyHadMediaAccess = savedInstanceState?.getBoolean(
            PREVIOUSLY_HAD_MEDIA_ACCESS_KEY,
        ) ?: false
        refreshPermissionState()
        eveningReminderEnabled = EveningReminderScheduler.isEnabled(applicationContext)
        readTaskIntent(intent)
        enableEdgeToEdge()

        setContent {
            FuShengCeApp(
                permissionState = permissionState,
                onRequestPermission = ::requestMediaPermission,
                onDismissPermission = ::finish,
                onOpenSystemSettings = ::openSystemSettings,
                mediaRefreshToken = mediaRefreshToken,
                eveningReminderEnabled = eveningReminderEnabled,
                initialCleanupOpen = intent.getBooleanExtra(EXTRA_OPEN_CLEANUP, false),
                onToggleEveningReminder = ::toggleEveningReminder,
                incomingText = incomingText,
                incomingTaskId = incomingTaskId,
                cleanupRequestToken = cleanupRequestToken,
                onConsumeIncoming = {
                    incomingText = null
                    incomingTaskId = null
                    intent.removeExtra(Intent.EXTRA_TEXT)
                    intent.removeExtra(EXTRA_TASK_ID)
                },
                onRequestTaskNotifications = {
                    if (Build.VERSION.SDK_INT >= 33 && !hasNotificationPermission()) {
                        taskNotificationLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                    } else {
                        startActivity(Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS)
                            .putExtra(Settings.EXTRA_APP_PACKAGE, packageName))
                    }
                },
            )
        }
    }

    override fun onResume() {
        super.onResume()
        refreshPermissionState()
        mediaRefreshToken++
        reconcileTaskReminders()
        if (EveningReminderScheduler.isEnabled(applicationContext)) {
            if (permissionState.hasMediaAccess && hasNotificationPermission()) {
                EveningReminderScheduler.scheduleNext(applicationContext)
            } else {
                EveningReminderScheduler.setEnabled(applicationContext, false)
                eveningReminderEnabled = false
            }
        }
        lifecycleScope.launch {
            MediaScanScheduler.enqueueIfChanged(applicationContext, permissionState.hasMediaAccess)
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        readTaskIntent(intent)
    }

    private fun readTaskIntent(intent: Intent) {
        try {
            incomingTaskId = intent.getStringExtra(EXTRA_TASK_ID)?.takeIf { it.length <= 100 }
            incomingText = if (intent.action == Intent.ACTION_SEND && intent.type == "text/plain") {
                intent.getCharSequenceExtra(Intent.EXTRA_TEXT)?.toString()?.takeIf { it.isNotBlank() }
            } else null
            if (intent.getBooleanExtra(EXTRA_OPEN_CLEANUP, false)) {
                cleanupRequestToken++
                intent.removeExtra(EXTRA_OPEN_CLEANUP)
            }
        } catch (_: RuntimeException) {
            incomingTaskId = null
            incomingText = null
        }
    }

    private fun reconcileTaskReminders() {
        lifecycleScope.launch {
            try { TaskReminders.reconcile(applicationContext) }
            catch (cancelled: CancellationException) { throw cancelled }
            catch (error: Exception) { Log.e("TaskReminder", "Unable to restore reminders", error) }
        }
    }

    override fun onSaveInstanceState(outState: Bundle) {
        outState.putBoolean(PREVIOUSLY_HAD_MEDIA_ACCESS_KEY, previouslyHadMediaAccess)
        outState.putBoolean(HAS_REQUESTED_BEFORE_KEY, hasRequestedBefore)
        super.onSaveInstanceState(outState)
    }

    private fun requestMediaPermission() {
        markPermissionRequested()
        permissionLauncher.launch(mediaPermissionPolicy.permissionsToRequest().toTypedArray())
    }

    private fun markPermissionRequested() {
        hasRequestedBefore = true
        getPreferences(MODE_PRIVATE).edit()
            .putBoolean(HAS_REQUESTED_BEFORE_KEY, true)
            .apply()
    }

    private fun refreshPermissionState() {
        permissionState = mediaPermissionPolicy.currentState(
            context = this,
            hasRequestedBefore = hasRequestedBefore,
            shouldShowRationale = { permission ->
                ActivityCompat.shouldShowRequestPermissionRationale(this, permission)
            },
        )
        val currentlyHasMediaAccess = permissionState.hasMediaAccess
        MediaScanScheduler.enqueueAfterPermissionChange(
            context = applicationContext,
            previouslyHadAccess = previouslyHadMediaAccess,
            currentlyHasAccess = currentlyHasMediaAccess,
        )
        previouslyHadMediaAccess = currentlyHasMediaAccess
    }

    private fun toggleEveningReminder() {
        if (eveningReminderEnabled) {
            EveningReminderScheduler.setEnabled(applicationContext, false)
            eveningReminderEnabled = false
        } else if (Build.VERSION.SDK_INT >= 33 && !hasNotificationPermission()) {
            notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
        } else {
            EveningReminderScheduler.setEnabled(applicationContext, true)
            eveningReminderEnabled = true
        }
    }

    private fun hasNotificationPermission(): Boolean =
        Build.VERSION.SDK_INT < 33 ||
            checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) ==
                PackageManager.PERMISSION_GRANTED

    private fun openSystemSettings() {
        startActivity(
            Intent(
                Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
                Uri.fromParts("package", packageName, null),
            ),
        )
    }

    private companion object {
        const val PREVIOUSLY_HAD_MEDIA_ACCESS_KEY = "previously-had-media-access"
        const val HAS_REQUESTED_BEFORE_KEY = "has-requested-media-permission"
    }
}
