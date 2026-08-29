package com.risealarm

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

class AlarmReconciliationReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action !in SUPPORTED_ACTIONS) return
        val app = context.applicationContext as RiseApplication
        app.alarmScheduler.reconcile(app.alarmStore.all())
    }

    private companion object {
        val SUPPORTED_ACTIONS = setOf(
            Intent.ACTION_LOCKED_BOOT_COMPLETED,
            Intent.ACTION_BOOT_COMPLETED,
            Intent.ACTION_TIME_CHANGED,
            Intent.ACTION_TIMEZONE_CHANGED,
            Intent.ACTION_MY_PACKAGE_REPLACED,
        )
    }
}
