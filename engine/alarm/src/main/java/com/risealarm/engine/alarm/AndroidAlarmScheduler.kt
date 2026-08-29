package com.risealarm.engine.alarm

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import com.risealarm.domain.AlarmDefinition
import java.time.ZonedDateTime

class AndroidAlarmScheduler(
    context: Context,
    private val fireReceiverClassName: String,
    private val showActivityClassName: String,
    private val calculator: NextOccurrenceCalculator = NextOccurrenceCalculator(),
) {
    private val appContext = context.applicationContext
    private val alarmManager = appContext.getSystemService(AlarmManager::class.java)

    fun canScheduleExactAlarms(): Boolean =
        Build.VERSION.SDK_INT < Build.VERSION_CODES.S || alarmManager.canScheduleExactAlarms()

    fun schedule(alarm: AlarmDefinition, now: ZonedDateTime = ZonedDateTime.now()): Long? {
        cancel(alarm.id)
        val occurrence = calculator.next(alarm, now) ?: return null
        if (!canScheduleExactAlarms()) return null

        val fireIntent = Intent(ACTION_FIRE_ALARM)
            .setClassName(appContext, fireReceiverClassName)
            .setData(Uri.parse("rise://alarm/${alarm.id}"))
            .putExtra(EXTRA_ALARM_ID, alarm.id)
        val firePendingIntent = PendingIntent.getBroadcast(
            appContext,
            fireRequestCode(alarm.id),
            fireIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )

        val showIntent = Intent(Intent.ACTION_VIEW, Uri.parse("rise://alarm/${alarm.id}/show"))
            .setClassName(appContext, showActivityClassName)
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
        val showPendingIntent = PendingIntent.getActivity(
            appContext,
            showRequestCode(alarm.id),
            showIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )

        return try {
            val triggerAtMillis = occurrence.toInstant().toEpochMilli()
            alarmManager.setAlarmClock(
                AlarmManager.AlarmClockInfo(triggerAtMillis, showPendingIntent),
                firePendingIntent,
            )
            triggerAtMillis
        } catch (_: SecurityException) {
            null
        }
    }

    fun cancel(alarmId: String) {
        val intent = Intent(ACTION_FIRE_ALARM)
            .setClassName(appContext, fireReceiverClassName)
            .setData(Uri.parse("rise://alarm/$alarmId"))
        val pendingIntent = PendingIntent.getBroadcast(
            appContext,
            fireRequestCode(alarmId),
            intent,
            PendingIntent.FLAG_NO_CREATE or PendingIntent.FLAG_IMMUTABLE,
        )
        if (pendingIntent != null) {
            alarmManager.cancel(pendingIntent)
            pendingIntent.cancel()
        }
    }

    fun reconcile(alarms: List<AlarmDefinition>, now: ZonedDateTime = ZonedDateTime.now()) {
        alarms.forEach { alarm ->
            if (alarm.enabled) schedule(alarm, now) else cancel(alarm.id)
        }
    }

    private fun fireRequestCode(id: String): Int = id.hashCode()
    private fun showRequestCode(id: String): Int = id.hashCode() xor SHOW_REQUEST_MASK

    companion object {
        const val ACTION_FIRE_ALARM = "com.risealarm.action.FIRE_ALARM"
        const val EXTRA_ALARM_ID = "alarm_id"
        private const val SHOW_REQUEST_MASK = 0x51A7C10C
    }
}
