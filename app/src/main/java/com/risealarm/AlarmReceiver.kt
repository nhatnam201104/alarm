package com.risealarm

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import androidx.core.content.ContextCompat
import com.risealarm.engine.alarm.AndroidAlarmScheduler
import com.risealarm.domain.WakeSession
import java.util.UUID

class AlarmReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != AndroidAlarmScheduler.ACTION_FIRE_ALARM) return
        val alarmId = intent.getStringExtra(AndroidAlarmScheduler.EXTRA_ALARM_ID) ?: return
        val app = context.applicationContext as RiseApplication
        val alarm = app.alarmStore.find(alarmId) ?: return
        if (!alarm.enabled) return

        val session = app.wakeSessionStore.replace(
            WakeSession(
                sessionId = UUID.randomUUID().toString(),
                alarmId = alarm.id,
                firedAtEpochMillis = System.currentTimeMillis(),
                challenge = alarm.challenge,
            ),
        )

        if (alarm.repeatDays.isEmpty()) {
            if (app.alarmStore.setEnabled(alarm.id, false) == null) return
        } else {
            app.alarmScheduler.schedule(alarm)
        }

        val ringingIntent = AlarmRingingService.startIntent(
            context = context,
            alarmId = alarm.id,
            hour = alarm.hour,
            minute = alarm.minute,
            label = alarm.label,
            vibration = alarm.vibration,
            sessionId = session.sessionId,
        )
        ContextCompat.startForegroundService(context, ringingIntent)
    }
}
