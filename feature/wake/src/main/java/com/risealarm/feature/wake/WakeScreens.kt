package com.risealarm.feature.wake

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.risealarm.core.designsystem.*
import kotlinx.serialization.Serializable

@Serializable data object RingingRoute
@Serializable data object ActiveVisionRoute
@Serializable data object ActiveSensorRoute
@Serializable data object EmergencyEscapeRoute
@Serializable data object WakeResultRoute

sealed interface WakeAction {
    data object Start : WakeAction
    data object Help : WakeAction
    data object Retry : WakeAction
    data object UseFallback : WakeAction
    data object Emergency : WakeAction
    data object ConfirmEscape : WakeAction
    data object Finish : WakeAction
    data object Back : WakeAction
    data class SubmitAnswer(val answer: String) : WakeAction
}

@Composable
fun AlarmRingingScreen(onAction: (WakeAction) -> Unit, modifier: Modifier = Modifier) {
    Box(
        modifier.fillMaxSize().background(
            Brush.radialGradient(listOf(RisePrimary.copy(alpha = 0.24f), RiseBackground), center = Offset.Unspecified, radius = 900f),
        ).padding(24.dp),
    ) {
        IconButton(onClick = { onAction(WakeAction.Emergency) }, modifier = Modifier.align(Alignment.TopEnd)) {
            Icon(Icons.Rounded.HealthAndSafety, "Tùy chọn khẩn cấp", tint = RiseTextMuted)
        }
        Column(Modifier.align(Alignment.Center), horizontalAlignment = Alignment.CenterHorizontally) {
            Box(Modifier.size(132.dp).clip(CircleShape).background(RisePrimary.copy(alpha = 0.16f)), contentAlignment = Alignment.Center) {
                Box(Modifier.size(92.dp).clip(CircleShape).background(RisePrimary.copy(alpha = 0.18f)), contentAlignment = Alignment.Center) {
                    Icon(Icons.Rounded.Alarm, null, tint = RisePrimary, modifier = Modifier.size(48.dp))
                }
            }
            Spacer(Modifier.height(34.dp))
            Text("06:30", style = MaterialTheme.typography.displayLarge)
            Text("Đến lúc rời giường", style = MaterialTheme.typography.headlineMedium)
            Spacer(Modifier.height(12.dp))
            StatusChip("3 nhiệm vụ · khoảng 2 phút", RiseSecondary, icon = Icons.Rounded.TaskAlt)
        }
        Column(Modifier.align(Alignment.BottomCenter)) {
            RisePrimaryButton("Bắt đầu thức dậy", { onAction(WakeAction.Start) }, leadingIcon = Icons.Rounded.Bolt)
            Spacer(Modifier.height(12.dp))
            Text("Không có snooze. Alarm chỉ dừng sau khi xác minh.", color = RiseTextMuted, textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth())
        }
    }
}

enum class VisionStatus { Framing, Valid, TrackingLost, CameraBusy, Thermal, TechnicalFallback, UnlockRequired }

@Immutable
data class ActiveVisionUiState(
    val exercise: String = "Push-up",
    val count: Int = 7,
    val target: Int = 12,
    val instruction: String = "Hạ thấp người thêm một chút",
    val status: VisionStatus = VisionStatus.Valid,
    val reFireSeconds: Int = 28,
)

@Composable
fun ActiveVisionScreen(
    state: ActiveVisionUiState,
    onAction: (WakeAction) -> Unit,
    cameraContent: @Composable BoxScope.() -> Unit,
    modifier: Modifier = Modifier,
) {
    Box(modifier.fillMaxSize().background(Color.Black)) {
        cameraContent()
        Box(Modifier.fillMaxSize().background(Brush.verticalGradient(listOf(Color.Black.copy(alpha = 0.28f), Color.Transparent, Color.Black.copy(alpha = 0.82f)))))
        Column(Modifier.fillMaxSize().padding(20.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                StatusChip(state.exercise, RiseSecondary)
                Spacer(Modifier.weight(1f))
                StatusChip("Re-fire " + state.reFireSeconds + "s", RiseWarning, icon = Icons.Rounded.Alarm)
            }
            when (state.status) {
                VisionStatus.Valid, VisionStatus.Framing -> {
                    Spacer(Modifier.weight(1f))
                    Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                        PoseGuide(valid = state.status == VisionStatus.Valid)
                    }
                    Spacer(Modifier.weight(1f))
                    Text(state.instruction, style = MaterialTheme.typography.titleLarge, textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth())
                    Spacer(Modifier.height(10.dp))
                    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.Bottom, horizontalArrangement = Arrangement.Center) {
                        Text(state.count.toString(), style = MaterialTheme.typography.displayLarge, color = RisePrimary)
                        Text(" / " + state.target, style = MaterialTheme.typography.headlineMedium, color = RiseTextMuted, modifier = Modifier.padding(bottom = 8.dp))
                    }
                    StatusChip(if (state.status == VisionStatus.Valid) "Tư thế được xác minh" else "Đưa toàn thân vào khung", if (state.status == VisionStatus.Valid) RiseVerified else RiseWarning, modifier = Modifier.align(Alignment.CenterHorizontally))
                    Spacer(Modifier.height(16.dp))
                    TextButton(onClick = { onAction(WakeAction.Help) }, modifier = Modifier.align(Alignment.CenterHorizontally)) {
                        Text("Không nhận diện được?")
                    }
                }
                else -> {
                    Spacer(Modifier.weight(1f))
                    VisionIssueCard(state.status, onAction)
                    Spacer(Modifier.weight(1f))
                }
            }
        }
    }
}

@Composable
private fun BoxScope.PoseGuide(valid: Boolean) {
    Canvas(Modifier.fillMaxWidth().height(360.dp)) {
        val c = if (valid) RiseVerified else RiseWarning
        val stroke = 7f
        val p = listOf(
            Offset(size.width * .50f, size.height * .14f),
            Offset(size.width * .42f, size.height * .28f),
            Offset(size.width * .58f, size.height * .28f),
            Offset(size.width * .40f, size.height * .48f),
            Offset(size.width * .60f, size.height * .48f),
            Offset(size.width * .43f, size.height * .68f),
            Offset(size.width * .57f, size.height * .68f),
            Offset(size.width * .37f, size.height * .90f),
            Offset(size.width * .63f, size.height * .90f),
        )
        drawCircle(c, 22f, p[0], style = Stroke(stroke))
        listOf(1 to 2, 1 to 3, 2 to 4, 3 to 5, 1 to 5, 4 to 6, 5 to 6, 6 to 7, 6 to 8, 7 to 9).forEach { (a, b) -> drawLine(c, p[a], p[b], stroke) }
        p.drop(1).forEach { drawCircle(c, 9f, it) }
        drawRoundRect(c.copy(alpha = .7f), style = Stroke(3f), cornerRadius = androidx.compose.ui.geometry.CornerRadius(36f, 36f))
    }
}

@Composable
private fun VisionIssueCard(status: VisionStatus, onAction: (WakeAction) -> Unit) {
    data class Issue(val icon: androidx.compose.ui.graphics.vector.ImageVector, val title: String, val detail: String, val color: Color)
    val issue = when (status) {
        VisionStatus.TrackingLost -> Issue(Icons.Rounded.VisibilityOff, "Mất khung hình", "Đứng lại trong khung và bảo đảm đủ ánh sáng.", RiseWarning)
        VisionStatus.CameraBusy -> Issue(Icons.Rounded.NoPhotography, "Camera đang được sử dụng", "Đóng ứng dụng camera khác rồi thử lại.", RiseWarning)
        VisionStatus.Thermal -> Issue(Icons.Rounded.DeviceThermostat, "Thiết bị đang quá nóng", "RISE đã dừng camera để bảo vệ thiết bị.", RiseError)
        VisionStatus.TechnicalFallback -> Issue(Icons.Rounded.BuildCircle, "Không thể tiếp tục camera", "Đây là lỗi kỹ thuật, không phải thất bại của bạn.", RiseError)
        VisionStatus.UnlockRequired -> Issue(Icons.Rounded.Lock, "Mở khóa để tiếp tục", "Cấu hình vision chỉ khả dụng sau khi mở khóa điện thoại.", RiseSecondary)
        else -> Issue(Icons.Rounded.CenterFocusStrong, "Căn lại khung", "Đưa toàn thân vào vùng hướng dẫn.", RiseWarning)
    }
    RiseCard(containerColor = issue.color.copy(alpha = .12f)) {
        Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
            IconBadge(issue.icon, issue.color, size = 72.dp)
            Spacer(Modifier.height(18.dp))
            Text(issue.title, style = MaterialTheme.typography.headlineMedium, textAlign = TextAlign.Center)
            Text(issue.detail, color = RiseTextMuted, textAlign = TextAlign.Center)
            Spacer(Modifier.height(22.dp))
            RisePrimaryButton(if (status == VisionStatus.TechnicalFallback || status == VisionStatus.Thermal) "Dùng fallback tương đương" else "Thử lại", { onAction(if (status == VisionStatus.TechnicalFallback || status == VisionStatus.Thermal) WakeAction.UseFallback else WakeAction.Retry) }, color = issue.color)
        }
    }
}

@Immutable
data class HoldUiState(
    val exercise: String = "Plank",
    val elapsed: Int = 14,
    val target: Int = 20,
    val isValid: Boolean = true,
)

@Composable
fun PlankHoldScreen(state: HoldUiState, onAction: (WakeAction) -> Unit, cameraContent: @Composable BoxScope.() -> Unit) {
    Box(Modifier.fillMaxSize().background(Color.Black)) {
        cameraContent()
        Box(Modifier.fillMaxSize().background(Brush.verticalGradient(listOf(Color.Black.copy(alpha = .35f), Color.Transparent, Color.Black.copy(alpha = .88f)))))
        Column(Modifier.fillMaxSize().padding(20.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                StatusChip(state.exercise, RiseSecondary)
                Spacer(Modifier.weight(1f))
                Text("Bộ 1/1", color = RiseTextMuted)
            }
            Spacer(Modifier.weight(1f))
            Box(Modifier.size(230.dp), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(progress = { state.elapsed.toFloat() / state.target }, modifier = Modifier.fillMaxSize(), strokeWidth = 14.dp, color = if (state.isValid) RiseVerified else RiseWarning, trackColor = RiseSurfaceHigh)
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("00:" + state.elapsed.toString().padStart(2, '0'), style = MaterialTheme.typography.displayMedium)
                    Text("/ 00:" + state.target, color = RiseTextMuted)
                }
            }
            Spacer(Modifier.height(22.dp))
            StatusChip(if (state.isValid) "Tư thế hợp lệ" else "Mất tư thế — đang tạm dừng", if (state.isValid) RiseVerified else RiseWarning, icon = if (state.isValid) Icons.Rounded.CheckCircle else Icons.Rounded.PauseCircle)
            Spacer(Modifier.height(12.dp))
            Text(if (state.isValid) "Giữ hông thẳng" else "Trở lại tư thế plank", style = MaterialTheme.typography.titleLarge)
            Spacer(Modifier.weight(1f))
            TextButton(onClick = { onAction(WakeAction.Help) }) { Text("Cần trợ giúp?") }
        }
    }
}

enum class SensorMissionType { Qr, Steps, Math, Shake }

@Composable
fun ActiveSensorMissionScreen(type: SensorMissionType, onAction: (WakeAction) -> Unit) {
    when (type) {
        SensorMissionType.Qr -> QrMission(onAction)
        SensorMissionType.Steps -> ProgressMission("Đi bộ", "14 / 30 bước", 14f / 30f, Icons.Rounded.DirectionsWalk, "Cầm điện thoại và đi khỏi giường")
        SensorMissionType.Shake -> ProgressMission("Lắc máy", "18 / 30", 18f / 30f, Icons.Rounded.Vibration, "Lắc đều theo nhịp")
        SensorMissionType.Math -> MathMission(onAction)
    }
}

@Composable
private fun QrMission(onAction: (WakeAction) -> Unit) {
    Box(Modifier.fillMaxSize().background(RiseBackground).padding(24.dp)) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Row(Modifier.fillMaxWidth()) { StatusChip("Nhiệm vụ 1/3", RiseSecondary); Spacer(Modifier.weight(1f)); StatusChip("Re-fire 22s", RiseWarning) }
            Spacer(Modifier.height(30.dp))
            Text("Quét QR phòng tắm", style = MaterialTheme.typography.headlineMedium)
            Text("Đưa mã vào chính giữa khung", color = RiseTextMuted)
            Spacer(Modifier.height(28.dp))
            Box(Modifier.fillMaxWidth().weight(1f).clip(RoundedCornerShape(28.dp)).background(Brush.linearGradient(listOf(Color(0xFF1C2634), Color(0xFF0D121B)))), contentAlignment = Alignment.Center) {
                Box(Modifier.size(230.dp).clip(RoundedCornerShape(24.dp)).background(RisePrimary.copy(alpha = .08f)), contentAlignment = Alignment.Center) {
                    Icon(Icons.Rounded.QrCodeScanner, null, tint = RisePrimary, modifier = Modifier.size(130.dp))
                }
            }
            TextButton(onClick = { onAction(WakeAction.Help) }) { Text("Không tìm thấy mã QR?") }
        }
    }
}

@Composable
private fun ProgressMission(title: String, value: String, progress: Float, icon: androidx.compose.ui.graphics.vector.ImageVector, instruction: String) {
    Column(Modifier.fillMaxSize().background(RiseBackground).padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
        IconBadge(icon, RisePrimary, size = 112.dp)
        Spacer(Modifier.height(28.dp))
        Text(title, style = MaterialTheme.typography.headlineLarge)
        Text(instruction, color = RiseTextMuted, textAlign = TextAlign.Center)
        Spacer(Modifier.height(36.dp))
        Text(value, style = MaterialTheme.typography.displayMedium)
        LinearProgressIndicator(progress = { progress }, modifier = Modifier.fillMaxWidth().height(12.dp).clip(CircleShape), color = RisePrimary, trackColor = RiseSurfaceHigh)
    }
}

@Composable
private fun MathMission(onAction: (WakeAction) -> Unit) {
    Column(Modifier.fillMaxSize().background(RiseBackground).padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally) {
        Row(Modifier.fillMaxWidth()) { StatusChip("Câu 2/3", RiseSecondary); Spacer(Modifier.weight(1f)); StatusChip("Khó", RiseWarning) }
        Spacer(Modifier.weight(1f))
        Text("47 + 28", style = MaterialTheme.typography.displayLarge)
        Text("Đánh thức trí óc của bạn", color = RiseTextMuted)
        Spacer(Modifier.height(32.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            listOf("65", "75", "85").forEach { answer ->
                OutlinedButton(onClick = { onAction(WakeAction.SubmitAnswer(answer)) }, modifier = Modifier.weight(1f).height(64.dp), shape = MaterialTheme.shapes.medium) {
                    Text(answer, style = MaterialTheme.typography.titleLarge)
                }
            }
        }
        Spacer(Modifier.weight(1f))
    }
}

@Composable
fun EmergencyEscapeScreen(holdProgress: Float, onAction: (WakeAction) -> Unit) {
    Column(Modifier.fillMaxSize().background(RiseBackground).padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally) {
        IconButton(onClick = { onAction(WakeAction.Back) }, modifier = Modifier.align(Alignment.Start)) { Icon(Icons.Rounded.ArrowBack, "Quay lại") }
        Spacer(Modifier.weight(1f))
        IconBadge(Icons.Rounded.HealthAndSafety, RiseError, size = 96.dp)
        Spacer(Modifier.height(24.dp))
        Text("Dừng khẩn cấp", style = MaterialTheme.typography.headlineLarge)
        Text("Chỉ dùng khi bạn không thể hoàn thành nhiệm vụ một cách an toàn. Sự kiện sẽ được ghi vào lịch sử.", color = RiseTextMuted, textAlign = TextAlign.Center)
        Spacer(Modifier.height(30.dp))
        Box(Modifier.size(190.dp), contentAlignment = Alignment.Center) {
            CircularProgressIndicator(progress = { holdProgress }, modifier = Modifier.fillMaxSize(), strokeWidth = 12.dp, color = RiseError, trackColor = RiseSurfaceHigh)
            Column(horizontalAlignment = Alignment.CenterHorizontally) { Icon(Icons.Rounded.TouchApp, null, tint = RiseError); Text("Giữ 10 giây", fontWeight = FontWeight.Bold) }
        }
        Spacer(Modifier.height(24.dp))
        RisePrimaryButton("Giữ để dừng báo thức", { onAction(WakeAction.ConfirmEscape) }, color = RiseError)
        Spacer(Modifier.weight(1f))
    }
}

@Composable
fun WakeResultScreen(escaped: Boolean, onAction: (WakeAction) -> Unit) {
    Box(Modifier.fillMaxSize().background(Brush.verticalGradient(listOf(if (escaped) RiseError.copy(alpha = .17f) else RisePrimary.copy(alpha = .16f), RiseBackground))).padding(24.dp)) {
        Column(Modifier.fillMaxSize(), horizontalAlignment = Alignment.CenterHorizontally) {
            Spacer(Modifier.weight(1f))
            IconBadge(if (escaped) Icons.Rounded.HealthAndSafety else Icons.Rounded.CheckCircle, if (escaped) RiseWarning else RiseVerified, size = 110.dp)
            Spacer(Modifier.height(24.dp))
            Text(if (escaped) "Báo thức đã dừng an toàn" else "Bạn đã thức dậy!", style = MaterialTheme.typography.headlineLarge, textAlign = TextAlign.Center)
            Text(if (escaped) "Phiên được đánh dấu Emergency Escape" else "Tuyệt vời — bắt đầu ngày mới thôi.", color = RiseTextMuted, textAlign = TextAlign.Center)
            Spacer(Modifier.height(26.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                MetricCard("Thời gian", "1p 34s", Icons.Rounded.Timer, RisePrimary, "Từ lần reo đầu", Modifier.weight(1f))
                MetricCard("Chuỗi", if (escaped) "0 ngày" else "13 ngày", Icons.Rounded.LocalFireDepartment, if (escaped) RiseTextMuted else RiseVerified, if (escaped) "Đã đặt lại" else "Kỷ lục mới", Modifier.weight(1f))
            }
            Spacer(Modifier.height(18.dp))
            RiseCard {
                listOf("Quét QR phòng tắm", "Squat · 15/15", "Toán nhanh · 3/3").forEach { item ->
                    Row(Modifier.fillMaxWidth().padding(vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Rounded.CheckCircle, null, tint = if (escaped) RiseTextMuted else RiseVerified)
                        Text(item, modifier = Modifier.padding(start = 12.dp))
                    }
                }
            }
            Spacer(Modifier.weight(1f))
            RisePrimaryButton("Bắt đầu ngày mới", { onAction(WakeAction.Finish) }, leadingIcon = Icons.Rounded.WbSunny)
        }
    }
}

@Composable
fun MockCameraContent() {
    Box(Modifier.fillMaxSize().background(Brush.linearGradient(listOf(Color(0xFF243244), Color(0xFF080B10), Color(0xFF201625)))))
}
