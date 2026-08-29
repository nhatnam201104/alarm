package com.risealarm

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.media.AudioAttributes
import android.media.AudioFocusRequest
import android.media.AudioManager
import android.media.MediaPlayer
import android.media.RingtoneManager
import android.media.ToneGenerator
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.os.PowerManager
import android.os.VibrationEffect
import android.os.Vibrator
import androidx.core.app.NotificationCompat

class AlarmRingingService : Service() {
    private var mediaPlayer: MediaPlayer? = null
    private var toneGenerator: ToneGenerator? = null
    private var vibrator: Vibrator? = null
    private var wakeLock: PowerManager.WakeLock? = null
    private var audioFocusRequest: AudioFocusRequest? = null
    private val fallbackHandler = Handler(Looper.getMainLooper())
    private val fallbackTone = object : Runnable {
        override fun run() {
            toneGenerator?.startTone(ToneGenerator.TONE_CDMA_ALERT_CALL_GUARD, 4_000)
            fallbackHandler.postDelayed(this, 4_500)
        }
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent?.action == ACTION_STOP) {
            stopSelf()
            return START_NOT_STICKY
        }
        if (intent?.action != ACTION_START) return START_NOT_STICKY

        val alarmId = intent.getStringExtra(EXTRA_ID) ?: return START_NOT_STICKY
        val hour = intent.getIntExtra(EXTRA_HOUR, 0)
        val minute = intent.getIntExtra(EXTRA_MINUTE, 0)
        val label = intent.getStringExtra(EXTRA_LABEL).orEmpty()
        val vibrationEnabled = intent.getBooleanExtra(EXTRA_VIBRATION, true)

        createChannel()
        startForeground(NOTIFICATION_ID, buildNotification(alarmId, hour, minute, label))
        acquireWakeLock()
        startAudio()
        if (vibrationEnabled) startVibration()
        return START_NOT_STICKY
    }

    private fun buildNotification(alarmId: String, hour: Int, minute: Int, label: String) =
        NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_alarm_notification)
            .setContentTitle("%02d:%02d · %s".format(hour, minute, label.ifBlank { "Báo thức RISE" }))
            .setContentText("Giữ nút 3 giây để tắt báo thức")
            .setCategory(NotificationCompat.CATEGORY_ALARM)
            .setPriority(NotificationCompat.PRIORITY_MAX)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setOngoing(true)
            .setAutoCancel(false)
            .setContentIntent(activityPendingIntent(alarmId))
            .setFullScreenIntent(activityPendingIntent(alarmId), true)
            .build()

    private fun activityPendingIntent(alarmId: String): PendingIntent {
        val intent = Intent(this, AlarmActivity::class.java)
            .putExtra(EXTRA_ID, alarmId)
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
        return PendingIntent.getActivity(
            this,
            alarmId.hashCode(),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
    }

    private fun createChannel() {
        val manager = getSystemService(NotificationManager::class.java)
        val channel = NotificationChannel(CHANNEL_ID, "Báo thức đang reo", NotificationManager.IMPORTANCE_HIGH).apply {
            description = "Hiển thị báo thức RISE trên màn hình khóa"
            setSound(null, null)
            enableVibration(false)
            lockscreenVisibility = android.app.Notification.VISIBILITY_PUBLIC
        }
        manager.createNotificationChannel(channel)
    }

    private fun acquireWakeLock() {
        val powerManager = getSystemService(PowerManager::class.java)
        wakeLock = powerManager.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "RISE:AlarmRinging").apply {
            setReferenceCounted(false)
            acquire(10 * 60 * 1_000L)
        }
    }

    private fun requestAudioFocus() {
        val audioManager = getSystemService(AudioManager::class.java)
        val attributes = AudioAttributes.Builder()
            .setUsage(AudioAttributes.USAGE_ALARM)
            .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
            .build()
        audioFocusRequest = AudioFocusRequest.Builder(AudioManager.AUDIOFOCUS_GAIN_TRANSIENT_EXCLUSIVE)
            .setAudioAttributes(attributes)
            .setOnAudioFocusChangeListener { }
            .build()
        audioManager.requestAudioFocus(audioFocusRequest!!)
    }

    private fun startAudio() {
        requestAudioFocus()
        val attributes = AudioAttributes.Builder()
            .setUsage(AudioAttributes.USAGE_ALARM)
            .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
            .build()
        val candidates = listOfNotNull(
            RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM),
            RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION),
        ).distinct()
        mediaPlayer = candidates.firstNotNullOfOrNull { uri ->
            var candidate: MediaPlayer? = null
            try {
                MediaPlayer().also { player ->
                    candidate = player
                    player.apply {
                    setAudioAttributes(attributes)
                    setDataSource(this@AlarmRingingService, uri)
                    isLooping = true
                    prepare()
                    start()
                    }
                }
            } catch (_: Exception) {
                candidate?.release()
                null
            }
        }
        if (mediaPlayer == null) {
            toneGenerator = ToneGenerator(AudioManager.STREAM_ALARM, 100)
            fallbackHandler.post(fallbackTone)
        }
    }

    private fun startVibration() {
        vibrator = getSystemService(Vibrator::class.java)
        vibrator?.vibrate(VibrationEffect.createWaveform(longArrayOf(0, 700, 300, 700, 600), 0))
    }

    override fun onDestroy() {
        fallbackHandler.removeCallbacksAndMessages(null)
        runCatching { mediaPlayer?.stop() }
        mediaPlayer?.release()
        mediaPlayer = null
        toneGenerator?.release()
        toneGenerator = null
        vibrator?.cancel()
        vibrator = null
        audioFocusRequest?.let { getSystemService(AudioManager::class.java).abandonAudioFocusRequest(it) }
        audioFocusRequest = null
        wakeLock?.takeIf { it.isHeld }?.release()
        wakeLock = null
        stopForeground(STOP_FOREGROUND_REMOVE)
        super.onDestroy()
    }

    companion object {
        private const val CHANNEL_ID = "rise_alarm_ringing_v1"
        private const val NOTIFICATION_ID = 4_201
        private const val ACTION_START = "com.risealarm.action.START_RINGING"
        const val ACTION_STOP = "com.risealarm.action.STOP_RINGING"
        const val EXTRA_ID = "alarm_id"
        private const val EXTRA_HOUR = "alarm_hour"
        private const val EXTRA_MINUTE = "alarm_minute"
        private const val EXTRA_LABEL = "alarm_label"
        private const val EXTRA_VIBRATION = "alarm_vibration"

        fun startIntent(context: Context, alarmId: String, hour: Int, minute: Int, label: String, vibration: Boolean) =
            Intent(context, AlarmRingingService::class.java)
                .setAction(ACTION_START)
                .putExtra(EXTRA_ID, alarmId)
                .putExtra(EXTRA_HOUR, hour)
                .putExtra(EXTRA_MINUTE, minute)
                .putExtra(EXTRA_LABEL, label)
                .putExtra(EXTRA_VIBRATION, vibration)

        fun stopIntent(context: Context) = Intent(context, AlarmRingingService::class.java).setAction(ACTION_STOP)
    }
}
