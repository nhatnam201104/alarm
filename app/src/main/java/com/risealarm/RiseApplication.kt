package com.risealarm

import android.app.Application
import com.risealarm.data.local.AlarmDefinitionStore
import com.risealarm.data.local.WakeSessionStore
import com.risealarm.engine.alarm.AndroidAlarmScheduler

class RiseApplication : Application() {
    val alarmStore by lazy { AlarmDefinitionStore(this) }
    val wakeSessionStore by lazy { WakeSessionStore(this) }
    val alarmScheduler by lazy {
        AndroidAlarmScheduler(
            context = this,
            fireReceiverClassName = AlarmReceiver::class.java.name,
            showActivityClassName = MainActivity::class.java.name,
        )
    }
}
