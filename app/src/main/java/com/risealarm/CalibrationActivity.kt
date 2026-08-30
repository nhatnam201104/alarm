package com.risealarm

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.CenterFocusStrong
import androidx.compose.material.icons.rounded.Verified
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.risealarm.core.designsystem.*
import com.risealarm.domain.ExerciseChallenge
import com.risealarm.domain.ExerciseType
import com.risealarm.engine.vision.TrackingQuality
import com.risealarm.engine.vision.VisionCameraPreview
import com.risealarm.engine.vision.evaluatorFor
import com.risealarm.feature.alarms.displayName

class CalibrationActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val exercise = intent.getStringExtra(EXTRA_EXERCISE)?.let { runCatching { ExerciseType.valueOf(it) }.getOrNull() }
            ?: run { finish(); return }
        enableEdgeToEdge()
        setContent {
            RiseTheme {
                val challenge = remember(exercise) { ExerciseChallenge(exercise, if (exercise == ExerciseType.Plank || exercise == ExerciseType.SquatHold) 20 else 10) }
                val evaluator = remember(challenge) { evaluatorFor(challenge) }
                var stableSince by remember { mutableLongStateOf(0L) }
                var stableSeconds by remember { mutableIntStateOf(0) }
                var instruction by remember { mutableStateOf("Đưa toàn thân vào khung và đứng nghiêng") }
                var error by remember { mutableStateOf<String?>(null) }
                val ready = stableSeconds >= 3

                Box(Modifier.fillMaxSize().background(Color.Black)) {
                    VisionCameraPreview(
                        onObservation = { observation ->
                            val result = evaluator.evaluate(observation)
                            runOnUiThread {
                                instruction = result.instruction
                                if (result.quality == TrackingQuality.Valid) {
                                    if (stableSince == 0L) stableSince = observation.timestampMillis
                                    stableSeconds = ((observation.timestampMillis - stableSince) / 1_000L).toInt().coerceAtMost(3)
                                } else {
                                    stableSince = 0L
                                    stableSeconds = 0
                                }
                            }
                        },
                        onError = { throwable ->
                            runOnUiThread {
                                error = if (throwable is UnsatisfiedLinkError) {
                                    "Thiết bị này không hỗ trợ MediaPipe Pose. Hãy hiệu chuẩn trên điện thoại ARM."
                                } else {
                                    throwable.message ?: "Không thể khởi động camera"
                                }
                            }
                        },
                        modifier = Modifier.fillMaxSize(),
                    )
                    Box(Modifier.fillMaxSize().background(Brush.verticalGradient(listOf(Color.Black.copy(.45f), Color.Transparent, Color.Black.copy(.9f)))))
                    Column(
                        Modifier.fillMaxSize().padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                    ) {
                        Spacer(Modifier.height(30.dp))
                        Text("Hiệu chuẩn ${exercise.displayName()}", style = MaterialTheme.typography.headlineMedium, color = RiseText, textAlign = TextAlign.Center)
                        Text("Đặt điện thoại chắc chắn và để toàn thân xuất hiện trong khung", color = RiseTextMuted, textAlign = TextAlign.Center)
                        Spacer(Modifier.weight(1f))
                        Icon(if (ready) Icons.Rounded.Verified else Icons.Rounded.CenterFocusStrong, null, tint = if (ready) RiseVerified else RiseWarning, modifier = Modifier.size(72.dp))
                        Spacer(Modifier.height(16.dp))
                        Text(error ?: if (ready) "Camera và tư thế đã sẵn sàng" else instruction, style = MaterialTheme.typography.titleLarge, color = RiseText, textAlign = TextAlign.Center)
                        Text(if (ready) "Hoàn tất" else "Giữ ổn định ${stableSeconds}/3 giây", color = RiseTextMuted)
                        Spacer(Modifier.height(24.dp))
                        RisePrimaryButton(
                            text = if (ready) "Lưu hiệu chuẩn" else "Đang kiểm tra…",
                            onClick = {
                                setResult(Activity.RESULT_OK, Intent())
                                finish()
                            },
                            enabled = ready,
                        )
                    }
                }
            }
        }
    }

    companion object {
        private const val EXTRA_EXERCISE = "exercise"
        fun intent(activity: Activity, exercise: ExerciseType) = Intent(activity, CalibrationActivity::class.java)
            .putExtra(EXTRA_EXERCISE, exercise.name)
    }
}
