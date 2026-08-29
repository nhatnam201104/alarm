package com.risealarm

import android.Manifest
import android.app.NotificationManager
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.content.ContextCompat

data class AlarmCapabilities(
    val exactAlarm: Boolean,
    val notifications: Boolean,
    val fullScreenIntent: Boolean,
) {
    val ready: Boolean get() = exactAlarm && notifications && fullScreenIntent
}

fun Context.readAlarmCapabilities(): AlarmCapabilities {
    val app = applicationContext as RiseApplication
    val notificationManager = getSystemService(NotificationManager::class.java)
    val notificationsAllowed = Build.VERSION.SDK_INT < 33 ||
        ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED
    val fullScreenAllowed = Build.VERSION.SDK_INT < 34 || notificationManager.canUseFullScreenIntent()
    return AlarmCapabilities(
        exactAlarm = app.alarmScheduler.canScheduleExactAlarms(),
        notifications = notificationsAllowed,
        fullScreenIntent = fullScreenAllowed,
    )
}
