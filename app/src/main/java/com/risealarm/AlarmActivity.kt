package com.risealarm

import android.os.Bundle
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.risealarm.core.designsystem.RiseTheme
import com.risealarm.feature.wake.ManualDismissAlarmScreen

class AlarmActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (android.os.Build.VERSION.SDK_INT >= 27) {
            setShowWhenLocked(true)
            setTurnScreenOn(true)
        } else {
            @Suppress("DEPRECATION")
            window.addFlags(
                WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED or
                    WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON,
            )
        }
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        enableEdgeToEdge()

        val alarmId = intent.getStringExtra(AlarmRingingService.EXTRA_ID)
            ?: intent.data?.pathSegments?.firstOrNull()
        val alarm = (application as RiseApplication).alarmStore.find(alarmId.orEmpty())
        val time = alarm?.let { "%02d:%02d".format(it.hour, it.minute) } ?: "BÁO THỨC"
        val label = alarm?.label.orEmpty()

        setContent {
            RiseTheme {
                BackHandler(enabled = true) { }
                ManualDismissAlarmScreen(
                    time = time,
                    label = label,
                    onDismiss = {
                        stopService(AlarmRingingService.stopIntent(this))
                        finishAndRemoveTask()
                    },
                )
            }
        }
    }
}
