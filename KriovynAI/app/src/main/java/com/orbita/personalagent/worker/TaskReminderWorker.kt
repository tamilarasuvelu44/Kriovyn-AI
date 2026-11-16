package com.orbita.personalagent.worker

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import androidx.work.CoroutineWorker
import androidx.work.Data
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import com.orbita.personalagent.MainActivity
import com.orbita.personalagent.R
import java.util.UUID
import java.util.concurrent.TimeUnit

object NotificationHelper {
    const val CHANNEL_ID = "kriovyn_task_reminders"

    fun ensureChannel(context: Context) {
        val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        val existing = manager.getNotificationChannel(CHANNEL_ID)
        if (existing == null) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                context.getString(R.string.reminder_channel_name),
                NotificationManager.IMPORTANCE_DEFAULT
            ).apply {
                description = context.getString(R.string.reminder_channel_description)
            }
            manager.createNotificationChannel(channel)
        }
    }

    fun hasNotificationPermission(context: Context): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED
        } else {
            true
        }
    }
}

class TaskReminderWorker(
    appContext: Context,
    params: WorkerParameters
) : CoroutineWorker(appContext, params) {

    override suspend fun doWork(): Result {
        val taskId = inputData.getLong(KEY_TASK_ID, -1L)
        val taskTitle = inputData.getString(KEY_TASK_TITLE) ?: "Scheduled Kriovyn Task"
        val taskDesc = inputData.getString(KEY_TASK_DESCRIPTION).orEmpty()

        NotificationHelper.ensureChannel(applicationContext)
        if (!NotificationHelper.hasNotificationPermission(applicationContext)) {
            return Result.success()
        }

        val launchIntent = Intent(applicationContext, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val pendingIntent = PendingIntent.getActivity(
            applicationContext,
            taskId.toInt(),
            launchIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val body = taskDesc.ifBlank { "Tap to open Kriovyn AI and review your task." }
        val notification = NotificationCompat.Builder(applicationContext, NotificationHelper.CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_notification_kriovyn)
            .setContentTitle("Task Reminder: $taskTitle")
            .setContentText(body)
            .setStyle(NotificationCompat.BigTextStyle().bigText(body))
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
            .build()

        NotificationManagerCompat.from(applicationContext).notify(
            if (taskId > 0) taskId.toInt() else System.currentTimeMillis().toInt(),
            notification
        )
        return Result.success()
    }

    companion object {
        const val KEY_TASK_ID = "task_id"
        const val KEY_TASK_TITLE = "task_title"
        const val KEY_TASK_DESCRIPTION = "task_description"
    }
}

class TaskReminderScheduler(private val context: Context) {

    private val workManager: WorkManager by lazy { WorkManager.getInstance(context) }

    fun scheduleReminder(
        taskId: Long,
        title: String,
        description: String,
        triggerAtMillis: Long
    ): String {
        NotificationHelper.ensureChannel(context)
        val delayMillis = (triggerAtMillis - System.currentTimeMillis()).coerceAtLeast(5_000L)
        val inputData = Data.Builder()
            .putLong(TaskReminderWorker.KEY_TASK_ID, taskId)
            .putString(TaskReminderWorker.KEY_TASK_TITLE, title)
            .putString(TaskReminderWorker.KEY_TASK_DESCRIPTION, description)
            .build()

        val request = OneTimeWorkRequestBuilder<TaskReminderWorker>()
            .setInitialDelay(delayMillis, TimeUnit.MILLISECONDS)
            .setInputData(inputData)
            .addTag("task_reminder_$taskId")
            .build()

        workManager.enqueueUniqueWork(
            uniqueWorkName(taskId),
            ExistingWorkPolicy.REPLACE,
            request
        )
        return request.id.toString()
    }

    fun cancelReminder(taskId: Long, workRequestId: String?) {
        workManager.cancelUniqueWork(uniqueWorkName(taskId))
        if (!workRequestId.isNullOrBlank()) {
            runCatching { UUID.fromString(workRequestId) }.getOrNull()?.let { uuid ->
                workManager.cancelWorkById(uuid)
            }
        }
    }

    private fun uniqueWorkName(taskId: Long): String = "kriovyn_task_reminder_$taskId"
}
