package com.fushengce.background

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import androidx.work.CoroutineWorker
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import com.fushengce.media.AndroidMediaRepository
import com.fushengce.media.MediaQueryResult
import java.time.LocalDate
import java.time.ZonedDateTime
import java.util.concurrent.TimeUnit

/** An opted-in, best-effort evening reminder. WorkManager may run after 22:00. */
class EveningReminderWorker(
    context: Context,
    parameters: WorkerParameters,
) : CoroutineWorker(context, parameters) {
    override suspend fun doWork(): Result {
        val context = applicationContext
        if (!EveningReminderScheduler.isEnabled(context)) return Result.success()
        val now = ZonedDateTime.now()
        if (now.hour != 22 || !hasMediaAccess(context) || !canNotify(context)) {
            EveningReminderScheduler.scheduleAfterWork(context)
            return Result.success()
        }

        val today = now.toLocalDate()
        val preferences = context.getSharedPreferences(PREFERENCES, Context.MODE_PRIVATE)
        val lastNotified = preferences.getString(LAST_NOTIFIED_DATE, null)
            ?.let { runCatching { LocalDate.parse(it) }.getOrNull() }
        if (lastNotified == today) {
            EveningReminderScheduler.scheduleAfterWork(context)
            return Result.success()
        }

        val count = CleanupCandidateCounter(
            AndroidMediaRepository(context.contentResolver),
        ).count(today, now.zone)
        if (count is MediaQueryResult.Success &&
            EveningReminderScheduler.isEnabled(context) &&
            canNotify(context) &&
            ZonedDateTime.now().let { it.toLocalDate() == today && it.hour == 22 } &&
            EveningReminderPolicy.shouldNotify(count.value, today, lastNotified)
        ) {
            if (postNotification(context, count.value)) {
                preferences.edit().putString(LAST_NOTIFIED_DATE, today.toString()).apply()
            }
        }
        // Failed or interrupted media queries never send a notification from a partial count.
        EveningReminderScheduler.scheduleAfterWork(context)
        return Result.success()
    }

    private fun postNotification(context: Context, count: Int): Boolean {
        if (!canNotify(context)) return false
        val manager = context.getSystemService(NotificationManager::class.java)
        manager.createNotificationChannel(
            NotificationChannel(CHANNEL_ID, "晚间整理提醒", NotificationManager.IMPORTANCE_DEFAULT),
        )
        val launch = context.packageManager.getLaunchIntentForPackage(context.packageName)
            ?: return false
        launch.putExtra(EXTRA_OPEN_CLEANUP, true)
        val pending = PendingIntent.getActivity(
            context, 0, launch,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
        val notification = android.app.Notification.Builder(context, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_menu_gallery)
            .setContentTitle("浮生册 · 今晚可整理影像")
            .setContentText("有 ${count} 项待整理候选，打开浮生册查看")
            .setContentIntent(pending)
            .setAutoCancel(true)
            .build()
        return try {
            manager.notify(NOTIFICATION_ID, notification)
            true
        } catch (_: SecurityException) {
            false
        }
    }
}

object EveningReminderScheduler {
    private const val UNIQUE_WORK = "fushengce-evening-reminder"
    private const val ENABLED = "enabled"

    fun isEnabled(context: Context): Boolean =
        context.getSharedPreferences(PREFERENCES, Context.MODE_PRIVATE)
            .getBoolean(ENABLED, false)

    fun setEnabled(context: Context, enabled: Boolean) {
        context.getSharedPreferences(PREFERENCES, Context.MODE_PRIVATE)
            .edit().putBoolean(ENABLED, enabled).apply()
        if (enabled) scheduleNext(context)
        else WorkManager.getInstance(context).cancelUniqueWork(UNIQUE_WORK)
    }

    fun scheduleNext(context: Context) = enqueue(context, ExistingWorkPolicy.KEEP)

    internal fun scheduleAfterWork(context: Context) =
        enqueue(context, ExistingWorkPolicy.APPEND_OR_REPLACE)

    private fun enqueue(context: Context, policy: ExistingWorkPolicy) {
        if (!isEnabled(context)) return
        val now = ZonedDateTime.now()
        // A finished 22:00 run always queues tomorrow, including the exact second 22:00:00.
        val reference = if (policy == ExistingWorkPolicy.APPEND_OR_REPLACE) now.plusSeconds(1) else now
        val next = EveningReminderPolicy.nextRunAt(reference)
        val delay = java.time.Duration.between(now, next)
            .toMillis().coerceAtLeast(0L)
        val request = OneTimeWorkRequestBuilder<EveningReminderWorker>()
            .setInitialDelay(delay, TimeUnit.MILLISECONDS)
            .build()
        WorkManager.getInstance(context).enqueueUniqueWork(
            UNIQUE_WORK, policy, request,
        )
    }
}

const val EXTRA_OPEN_CLEANUP = "com.fushengce.extra.OPEN_CLEANUP"
private const val PREFERENCES = "evening-reminder"
private const val LAST_NOTIFIED_DATE = "last-notified-date"
private const val CHANNEL_ID = "evening-cleanup"
private const val NOTIFICATION_ID = 2200

private fun canNotify(context: Context): Boolean =
    Build.VERSION.SDK_INT < 33 ||
        context.checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) ==
        PackageManager.PERMISSION_GRANTED

private fun hasMediaAccess(context: Context): Boolean =
    if (Build.VERSION.SDK_INT >= 34) {
        context.checkSelfPermission(Manifest.permission.READ_MEDIA_IMAGES) ==
            PackageManager.PERMISSION_GRANTED ||
            context.checkSelfPermission(Manifest.permission.READ_MEDIA_VIDEO) ==
            PackageManager.PERMISSION_GRANTED ||
            context.checkSelfPermission(Manifest.permission.READ_MEDIA_VISUAL_USER_SELECTED) ==
            PackageManager.PERMISSION_GRANTED
    } else if (Build.VERSION.SDK_INT >= 33) {
        context.checkSelfPermission(Manifest.permission.READ_MEDIA_IMAGES) ==
            PackageManager.PERMISSION_GRANTED ||
            context.checkSelfPermission(Manifest.permission.READ_MEDIA_VIDEO) ==
            PackageManager.PERMISSION_GRANTED
    } else {
        context.checkSelfPermission(Manifest.permission.READ_EXTERNAL_STORAGE) ==
            PackageManager.PERMISSION_GRANTED
    }
