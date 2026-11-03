package org.openyaske.app.notify

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
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.Worker
import androidx.work.WorkerParameters
import java.util.concurrent.TimeUnit
import org.openyaske.app.MainActivity
import org.openyaske.app.ui.nowMinutes
import org.openyaske.app.ui.todayIso
import org.openyaske.core.api.CoreApi
import org.openyaske.core.data.AndroidDatabaseDriverFactory
import org.openyaske.core.data.DatabaseRepository
import org.openyaske.core.data.openDatabase
import org.openyaske.core.logic.PeriodTimes
import org.openyaske.core.model.SessionStatus
import org.openyaske.core.model.Weekday
import org.openyaske.core.validation.IsoDate

/**
 * 授業前のローカル通知。
 *
 * サーバも Push も使わない。15 分ごとの Worker が端末内の DB を見て、
 * 「もうすぐ始まる授業」だけを通知する。同じ授業回は 1 回しか出さない。
 * 設定の `quietDuringClass` が有効なら、授業中は出さない。
 */
object ClassReminder {
    const val CHANNEL_ID = "class_reminder"
    private const val WORK_NAME = "class-reminder"
    private const val PREFS = "class_reminder"
    private const val KEY_NOTIFIED = "notified"

    /** アプリ起動時に呼ぶ。設定が OFF でも Worker 側で判断するので、常に 1 本だけ積む。 */
    fun ensureScheduled(context: Context) {
        createChannel(context)
        val request = PeriodicWorkRequestBuilder<ClassReminderWorker>(15, TimeUnit.MINUTES)
            .setInitialDelay(1, TimeUnit.MINUTES)
            .build()
        WorkManager.getInstance(context).enqueueUniquePeriodicWork(
            WORK_NAME,
            ExistingPeriodicWorkPolicy.UPDATE,
            request
        )
    }

    fun canNotify(context: Context): Boolean {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) return true
        return ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.POST_NOTIFICATIONS
        ) == PackageManager.PERMISSION_GRANTED
    }

    internal fun createChannel(context: Context) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
        val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        if (manager.getNotificationChannel(CHANNEL_ID) != null) return
        manager.createNotificationChannel(
            NotificationChannel(
                CHANNEL_ID,
                "Class reminders",
                NotificationManager.IMPORTANCE_DEFAULT
            ).apply { description = "Start of the next class" }
        )
    }

    internal fun show(context: Context, sessionId: String, title: String, body: String) {
        val intent = Intent(context, MainActivity::class.java)
        val pending = PendingIntent.getActivity(
            context,
            sessionId.hashCode(),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_menu_agenda)
            .setContentTitle(title)
            .setContentText(body)
            .setContentIntent(pending)
            .setAutoCancel(true)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .build()
        runCatching {
            NotificationManagerCompat.from(context).notify(sessionId.hashCode(), notification)
        }
    }

    internal fun alreadyNotified(context: Context, sessionId: String): Boolean =
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .getStringSet(KEY_NOTIFIED, emptySet())
            ?.contains(sessionId) == true

    internal fun markNotified(context: Context, sessionId: String) {
        val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        val current = prefs.getStringSet(KEY_NOTIFIED, emptySet()).orEmpty().toMutableSet()
        current.add(sessionId)
        // 翌日には消えるよう、日付を一緒に持たず、件数だけ抑える。
        if (current.size > 200) current.clear()
        prefs.edit().putStringSet(KEY_NOTIFIED, current).apply()
    }
}

/** 15 分ごとに今日の授業を見て、開始が近いものだけ通知する。 */
class ClassReminderWorker(
    appContext: Context,
    params: WorkerParameters
) : Worker(appContext, params) {

    override fun doWork(): Result {
        val context = applicationContext
        val api = runCatching {
            CoreApi(
                DatabaseRepository(
                    openDatabase(AndroidDatabaseDriverFactory(context))
                )
            )
        }.getOrNull() ?: return Result.success()

        val settings = api.settings.value
        if (!settings.notifications.enabled) return Result.success()
        if (!ClassReminder.canNotify(context)) return Result.success()

        val now = nowMinutes()
        val today = todayIso()
        val weekday = Weekday.entries.firstOrNull { it.isoNumber == IsoDate.dayOfWeekIso(today) }
            ?: return Result.success()
        val snapshot = api.snapshot
        val courses = snapshot.courses.filter { it.deletedAtMillis == null }.associateBy { it.id }

        fun startOf(session: org.openyaske.core.model.CourseSession): Int? =
            session.startTime?.let { PeriodTimes.parseTimeToMinutes(it) }
                ?: session.startPeriod?.let { settings.periodStartMinutes[it] ?: PeriodTimes.uecStartMinutes[it] }

        fun endOf(session: org.openyaske.core.model.CourseSession): Int? {
            session.endTime?.let { return PeriodTimes.parseTimeToMinutes(it) }
            val start = startOf(session) ?: return null
            return start + 90
        }

        val todaySessions = snapshot.sessions.filter {
            it.deletedAtMillis == null && it.date == today && it.status != SessionStatus.CANCELLED
        }

        // 授業中は出さない。
        if (settings.notifications.quietDuringClass) {
            val inClass = todaySessions.any { session ->
                val start = startOf(session)
                val end = endOf(session)
                start != null && end != null && now in start until end
            }
            if (inClass) return Result.success()
        }

        val horizon = now + maxOf(settings.notifications.leadMinutes, 15)
        todaySessions.forEach { session ->
            val start = startOf(session) ?: return@forEach
            if (start < now || start > horizon) return@forEach
            if (ClassReminder.alreadyNotified(context, session.id)) return@forEach
            val course = courses[session.courseId] ?: return@forEach
            val time = PeriodTimes.formatMinutes(start)
            val room = session.room ?: course.room
            val title = if (settings.locale.startsWith("ja")) "まもなく授業" else "Class soon"
            val body = listOfNotNull(
                "$time ${course.name}",
                room
            ).joinToString(" · ")
            ClassReminder.show(context, session.id, title, body)
            ClassReminder.markNotified(context, session.id)
        }
        // 課題の締切前日 20:00 の知らせ。20:00〜20:15 の間に 1 回だけ出す。
        if (settings.notifications.taskReminderEnabled && now in 20 * 60 until 20 * 60 + 15) {
            val tomorrow = IsoDate.plusDays(today, 1)
            val dueTomorrow = snapshot.courseTasks.filter {
                it.deletedAtMillis == null &&
                    it.status == org.openyaske.core.model.TaskStatus.OPEN &&
                    it.dueDate == tomorrow
            }
            dueTomorrow.forEach { task ->
                val key = "task-${task.id}"
                if (ClassReminder.alreadyNotified(context, key)) return@forEach
                val course = courses[task.courseId]
                val title = if (settings.locale.startsWith("ja")) "明日が締切の課題" else "Due tomorrow"
                val body = listOfNotNull(task.title, course?.name, task.dueTime).joinToString(" · ")
                ClassReminder.show(context, key, title, body)
                ClassReminder.markNotified(context, key)
            }
        }
        return Result.success()
    }
}
