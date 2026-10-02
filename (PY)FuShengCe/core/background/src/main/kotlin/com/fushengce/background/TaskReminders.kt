package com.fushengce.background

import android.Manifest
import android.app.AlarmManager
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.util.Log
import com.fushengce.database.FuShengCeDatabase
import com.fushengce.database.TaskRecord
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

const val EXTRA_TASK_ID = "com.fushengce.extra.TASK_ID"
private const val REVISION = "task-reminder-revision"
private const val CHANNEL = "task-reminders"

object TaskReminders {
    // ponytail: one process-wide lock; split per task only if reminder traffic warrants it.
    private val mutex = Mutex()

    suspend fun save(context: Context, value: TaskRecord) = mutex.withLock {
        val dao = FuShengCeDatabase.get(context).taskDao()
        val saved = value.prepareForSave(dao.find(value.id), System.currentTimeMillis())
        dao.upsert(saved)
        cancel(context, saved.id)
        if (!saved.completed && saved.remindAtMillis != null &&
            saved.notifiedRevision != saved.reminderRevision) schedule(context, saved)
    }

    suspend fun delete(context: Context, id: String) = mutex.withLock {
        FuShengCeDatabase.get(context).taskDao().delete(id)
        cancel(context, id)
    }

    suspend fun reconcile(context: Context) = mutex.withLock {
        FuShengCeDatabase.get(context).taskDao().pendingReminders().forEach { schedule(context, it) }
    }

    fun canNotify(context: Context): Boolean {
        val manager = context.getSystemService(NotificationManager::class.java)
        return (Build.VERSION.SDK_INT < 33 ||
            context.checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED) &&
            manager.areNotificationsEnabled() &&
            manager.getNotificationChannel(CHANNEL)?.importance != NotificationManager.IMPORTANCE_NONE
    }

    fun canSchedulePrecisely(context: Context): Boolean = Build.VERSION.SDK_INT < 31 ||
        context.getSystemService(AlarmManager::class.java).canScheduleExactAlarms()

    private fun alarmIntent(context: Context, id: String, revision: String = ""): PendingIntent {
        val intent = Intent(context, TaskReminderReceiver::class.java)
            .setAction("com.fushengce.TASK_REMINDER")
            .setData(Uri.Builder().scheme("fushengce").authority("reminder").appendPath(id).build())
            .putExtra(EXTRA_TASK_ID, id).putExtra(REVISION, revision)
        return PendingIntent.getBroadcast(context, 0, intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
    }

    private fun cancel(context: Context, id: String) {
        context.getSystemService(AlarmManager::class.java).cancel(alarmIntent(context, id))
        context.getSystemService(NotificationManager::class.java).cancel(id, 1)
    }

    private fun schedule(context: Context, task: TaskRecord) {
        val at = task.remindAtMillis ?: return
        val alarms = context.getSystemService(AlarmManager::class.java)
        val pending = alarmIntent(context, task.id, task.reminderRevision)
        val trigger = at.coerceAtLeast(System.currentTimeMillis() + 1000)
        if (canSchedulePrecisely(context)) {
            try {
                alarms.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, trigger, pending)
                return
            } catch (_: SecurityException) {
                // The permission may be revoked between the check and the system call.
            }
        }
        alarms.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, trigger, pending)
    }

    internal suspend fun deliver(context: Context, id: String, revision: String) = mutex.withLock {
        val dao = FuShengCeDatabase.get(context).taskDao()
        val task = dao.find(id) ?: return@withLock
        if (!task.reminderDue(System.currentTimeMillis(), revision) || !canNotify(context)) return@withLock
        val manager = context.getSystemService(NotificationManager::class.java)
        manager.createNotificationChannel(NotificationChannel(CHANNEL, "工作与生活事项", NotificationManager.IMPORTANCE_HIGH))
        val launch = context.packageManager.getLaunchIntentForPackage(context.packageName) ?: return@withLock
        launch.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP)
            .setData(Uri.Builder().scheme("fushengce").authority("task").appendPath(id).build())
            .putExtra(EXTRA_TASK_ID, id)
        val content = PendingIntent.getActivity(context, 0, launch,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
        val area = if (task.realm == "work") "工作" else "生活"
        val notification = Notification.Builder(context, CHANNEL)
            .setSmallIcon(android.R.drawable.ic_popup_reminder)
            .setContentTitle("${if (task.emergency) "应急" else area} · ${task.title}")
            .setContentText("事项已到提醒时间，点此查看")
            .setVisibility(Notification.VISIBILITY_PRIVATE)
            .setContentIntent(content).setAutoCancel(true).setOnlyAlertOnce(true).build()
        try {
            manager.notify(id, 1, notification)
            dao.upsert(task.copy(notifiedRevision = revision))
        } catch (_: SecurityException) {
            // Leave it pending so granting notifications and reopening can recover delivery.
        }
    }
}

class TaskReminderReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val pending = goAsync()
        CoroutineScope(Dispatchers.IO).launch {
            try {
                if (intent.action == "com.fushengce.TASK_REMINDER") {
                    val id = intent.getStringExtra(EXTRA_TASK_ID) ?: return@launch
                    val revision = intent.getStringExtra(REVISION) ?: return@launch
                    TaskReminders.deliver(context.applicationContext, id, revision)
                } else {
                    TaskReminders.reconcile(context.applicationContext)
                }
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (error: Exception) {
                Log.e("TaskReminder", "Reminder failed; reconcile on next app resume", error)
            } finally {
                pending.finish()
            }
        }
    }
}
