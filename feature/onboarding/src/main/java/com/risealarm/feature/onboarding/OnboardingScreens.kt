package com.risealarm.feature.onboarding

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AccessibilityNew
import androidx.compose.material.icons.rounded.Alarm
import androidx.compose.material.icons.rounded.ArrowBack
import androidx.compose.material.icons.rounded.BatteryChargingFull
import androidx.compose.material.icons.rounded.CameraAlt
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.ChevronRight
import androidx.compose.material.icons.rounded.Fullscreen
import androidx.compose.material.icons.rounded.HealthAndSafety
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material.icons.rounded.NotificationsActive
import androidx.compose.material.icons.rounded.QrCode2
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.SelfImprovement
import androidx.compose.material.icons.rounded.Shield
import androidx.compose.material.icons.rounded.Smartphone
import androidx.compose.material.icons.rounded.Timer
import androidx.compose.material3.Checkbox
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.risealarm.core.designsystem.IconBadge
import com.risealarm.core.designsystem.RiseBackground
import com.risealarm.core.designsystem.RiseCard
import com.risealarm.core.designsystem.RiseError
import com.risealarm.core.designsystem.RiseOutline
import com.risealarm.core.designsystem.RisePrimary
import com.risealarm.core.designsystem.RisePrimaryButton
import com.risealarm.core.designsystem.RiseSecondary
import com.risealarm.core.designsystem.RiseSpacing
import com.risealarm.core.designsystem.RiseSurfaceHigh
import com.risealarm.core.designsystem.RiseSurface
import com.risealarm.core.designsystem.RiseTextMuted
import com.risealarm.core.designsystem.RiseVerified
import com.risealarm.core.designsystem.RiseWarning
import com.risealarm.core.designsystem.StatusChip
import kotlinx.serialization.Serializable

@Serializable data object WelcomeRoute
@Serializable data object SafetyRoute
@Serializable data object PermissionRoute
@Serializable data object ReliabilityRoute
@Serializable data object ExerciseSetupRoute
@Serializable data object QrSetupRoute

sealed interface OnboardingAction {
    data object Continue : OnboardingAction
    data object Back : OnboardingAction
    data class Toggle(val key: String, val enabled: Boolean) : OnboardingAction
    data class Select(val key: String) : OnboardingAction
    data object OpenSettings : OnboardingAction
    data object RunTest : OnboardingAction
}

@Composable
private fun OnboardingScaffold(
    step: Int,
    title: String,
    subtitle: String,
    onBack: (() -> Unit)? = null,
    footer: @Composable () -> Unit,
    content: @Composable ColumnScope.() -> Unit,
) {
    Scaffold(
        containerColor = RiseBackground,
        topBar = {
            Column(Modifier.padding(horizontal = 20.dp, vertical = 12.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    if (onBack != null) {
                        IconButton(onClick = onBack) { Icon(Icons.Rounded.ArrowBack, "Quay lại") }
                    }
                    Text("RISE", color = RisePrimary, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
                    Text("$step/6", color = RiseTextMuted, style = MaterialTheme.typography.labelLarge)
                }
                LinearProgressIndicator(
                    progress = { step / 6f },
                    modifier = Modifier.fillMaxWidth().height(4.dp).clip(CircleShape),
                    color = RisePrimary,
                    trackColor = RiseSurfaceHigh,
                )
            }
        },
        bottomBar = { Box(Modifier.padding(20.dp)) { footer() } },
    ) { padding ->
        Column(
            modifier = Modifier.fillMaxSize().padding(padding).padding(horizontal = 20.dp).verticalScroll(rememberScrollState()),
        ) {
            Spacer(Modifier.height(16.dp))
            Text(title, style = MaterialTheme.typography.headlineLarge)
            Spacer(Modifier.height(8.dp))
            Text(subtitle, color = RiseTextMuted, style = MaterialTheme.typography.bodyLarge)
            Spacer(Modifier.height(24.dp))
            content()
            Spacer(Modifier.height(120.dp))
        }
    }
}

@Composable
fun WelcomeScreen(onAction: (OnboardingAction) -> Unit, modifier: Modifier = Modifier) {
    Box(
        modifier.fillMaxSize().background(
            Brush.radialGradient(listOf(RisePrimary.copy(alpha = 0.20f), RiseBackground), radius = 1000f),
        ),
    ) {
        Column(
            Modifier.fillMaxSize().padding(24.dp),
            verticalArrangement = Arrangement.SpaceBetween,
        ) {
            Text("RISE", color = RisePrimary, style = MaterialTheme.typography.titleLarge)
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Box(
                    Modifier.size(116.dp).clip(CircleShape).background(RisePrimary.copy(alpha = 0.16f)),
                    contentAlignment = Alignment.Center,
                ) { Icon(Icons.Rounded.Alarm, null, tint = RisePrimary, modifier = Modifier.size(58.dp)) }
                Spacer(Modifier.height(32.dp))
                Text("Thắng buổi sáng\ncủa bạn", style = MaterialTheme.typography.displayMedium, textAlign = TextAlign.Center)
                Spacer(Modifier.height(16.dp))
                Text(
                    "Báo thức chỉ dừng khi bạn hoàn thành bằng chứng rằng mình đã thực sự thức dậy.",
                    color = RiseTextMuted,
                    style = MaterialTheme.typography.bodyLarge,
                    textAlign = TextAlign.Center,
                )
                Spacer(Modifier.height(24.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    StatusChip("Không snooze", RisePrimary)
                    StatusChip("Xử lý trên máy", RiseVerified)
                }
            }
            Column {
                RisePrimaryButton("Thiết lập RISE", { onAction(OnboardingAction.Continue) })
                Spacer(Modifier.height(12.dp))
                Text("Bạn luôn có lối thoát khẩn cấp an toàn.", color = RiseTextMuted, style = MaterialTheme.typography.bodyMedium, textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth())
            }
        }
    }
}

@Immutable
data class SafetyUiState(
    val mobilityEnabled: Boolean = true,
    val selectedLimitations: Set<String> = emptySet(),
    val accepted: Boolean = false,
)

@Composable
fun SafetyProfileScreen(state: SafetyUiState, onAction: (OnboardingAction) -> Unit) {
    OnboardingScaffold(
        step = 2,
        title = "An toàn trước tiên",
        subtitle = "Chúng tôi dùng thông tin này để chỉ đề xuất nhiệm vụ phù hợp. Đây không phải đánh giá y tế.",
        onBack = { onAction(OnboardingAction.Back) },
        footer = { RisePrimaryButton("Tiếp tục", { onAction(OnboardingAction.Continue) }, enabled = state.accepted) },
    ) {
        RiseCard {
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconBadge(Icons.Rounded.AccessibilityNew, RiseSecondary)
                Column(Modifier.weight(1f).padding(start = 14.dp)) {
                    Text("Tôi muốn dùng nhiệm vụ vận động", style = MaterialTheme.typography.titleMedium)
                    Text("Có thể thay đổi trước khi kích hoạt alarm", color = RiseTextMuted)
                }
                Switch(checked = state.mobilityEnabled, onCheckedChange = { onAction(OnboardingAction.Toggle("mobility", it)) })
            }
        }
        Spacer(Modifier.height(16.dp))
        Text("Bạn có vùng nào cần tránh vận động?", style = MaterialTheme.typography.titleMedium)
        Spacer(Modifier.height(10.dp))
        listOf("Cổ hoặc lưng", "Cổ tay hoặc vai", "Đầu gối", "Mất thăng bằng").forEach { item ->
            RiseCard(modifier = Modifier.padding(vertical = 5.dp), containerColor = RiseSurfaceHigh) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Checkbox(checked = item in state.selectedLimitations, onCheckedChange = { onAction(OnboardingAction.Toggle(item, it)) })
                    Text(item, modifier = Modifier.padding(start = 8.dp))
                }
            }
        }
        Spacer(Modifier.height(16.dp))
        RiseCard(containerColor = RiseVerified.copy(alpha = 0.08f)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Rounded.HealthAndSafety, null, tint = RiseVerified)
                Text("Luôn có protocol không vận động và Emergency Escape.", modifier = Modifier.padding(start = 12.dp), color = RiseVerified)
            }
        }
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(top = 16.dp)) {
            Checkbox(checked = state.accepted, onCheckedChange = { onAction(OnboardingAction.Toggle("accepted", it)) })
            Text("Tôi hiểu và sẽ dừng lại nếu thấy đau hoặc chóng mặt.", modifier = Modifier.padding(start = 8.dp))
        }
    }
}

data class PermissionItemUi(val key: String, val title: String, val description: String, val granted: Boolean, val icon: ImageVector)

@Composable
fun PermissionWizardScreen(items: List<PermissionItemUi>, onAction: (OnboardingAction) -> Unit) {
    val ready = items.take(3).all { it.granted }
    OnboardingScaffold(
        step = 3,
        title = "Cho phép RISE đánh thức bạn",
        subtitle = "Chỉ cấp quyền khi cần. Camera và nhận diện hoạt động sẽ được hỏi theo nhiệm vụ.",
        onBack = { onAction(OnboardingAction.Back) },
        footer = { RisePrimaryButton(if (ready) "Chọn protocol" else "Cấp quyền bắt buộc", { onAction(if (ready) OnboardingAction.Continue else OnboardingAction.OpenSettings) }) },
    ) {
        items.forEach { item ->
            RiseCard(modifier = Modifier.padding(bottom = 12.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconBadge(item.icon, if (item.granted) RiseVerified else RiseWarning)
                    Column(Modifier.weight(1f).padding(horizontal = 14.dp)) {
                        Text(item.title, style = MaterialTheme.typography.titleMedium)
                        Text(item.description, color = RiseTextMuted, style = MaterialTheme.typography.bodyMedium)
                    }
                    if (item.granted) Icon(Icons.Rounded.CheckCircle, "Đã cấp", tint = RiseVerified)
                    else Icon(Icons.Rounded.ChevronRight, "Mở cài đặt", tint = RiseWarning)
                }
            }
        }
        RiseCard(containerColor = RiseSurfaceHigh) {
            Text("Giới hạn hệ thống", style = MaterialTheme.typography.titleMedium)
            Spacer(Modifier.height(6.dp))
            Text("Force Stop, tắt nguồn hoặc thu hồi quyền vẫn có thể ngăn alarm hoạt động. RISE sẽ hiển thị trạng thái trung thực.", color = RiseTextMuted)
        }
    }
}

@Composable
fun ReliabilityTestScreen(isRunning: Boolean, passed: Boolean, onAction: (OnboardingAction) -> Unit) {
    OnboardingScaffold(
        step = 4,
        title = if (passed) "Alarm test đã thành công" else "Kiểm tra trên thiết bị này",
        subtitle = "RISE sẽ reo sau 30 giây. Hãy khóa màn hình và hoàn thành protocol thử.",
        onBack = { onAction(OnboardingAction.Back) },
        footer = {
            RisePrimaryButton(
                text = when { passed -> "Hoàn tất thiết lập"; isRunning -> "Đang chờ alarm…"; else -> "Bắt đầu kiểm tra 30 giây" },
                onClick = { onAction(if (passed) OnboardingAction.Continue else OnboardingAction.RunTest) },
                enabled = !isRunning,
                leadingIcon = if (passed) Icons.Rounded.Check else Icons.Rounded.Timer,
            )
        },
    ) {
        RiseCard(containerColor = if (passed) RiseVerified.copy(alpha = 0.10f) else RiseSurface) {
            Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                Box(
                    Modifier.size(150.dp).clip(CircleShape).background((if (passed) RiseVerified else RisePrimary).copy(alpha = 0.14f)),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(if (passed) Icons.Rounded.CheckCircle else Icons.Rounded.Alarm, null, tint = if (passed) RiseVerified else RisePrimary, modifier = Modifier.size(72.dp))
                }
            }
            Spacer(Modifier.height(24.dp))
            listOf(
                Triple("Exact alarm", Icons.Rounded.Alarm, true),
                Triple("Mở trên màn hình khóa", Icons.Rounded.Fullscreen, passed),
                Triple("Âm thanh và rung", Icons.Rounded.NotificationsActive, passed),
                Triple("Hoàn thành protocol", Icons.Rounded.Shield, passed),
            ).forEach { (label, icon, ok) ->
                Row(Modifier.fillMaxWidth().padding(vertical = 9.dp), verticalAlignment = Alignment.CenterVertically) {
                    Icon(icon, null, tint = if (ok) RiseVerified else RiseTextMuted)
                    Text(label, modifier = Modifier.weight(1f).padding(start = 12.dp))
                    Text(if (ok) "Đạt" else "Chờ", color = if (ok) RiseVerified else RiseTextMuted)
                }
            }
        }
    }
}

data class ExerciseChoiceUi(val name: String, val detail: String, val icon: ImageVector, val available: Boolean = true)

@Composable
fun ExerciseSetupScreen(selected: String, onAction: (OnboardingAction) -> Unit) {
    val choices = listOf(
        ExerciseChoiceUi("Push-up", "Đếm chu kỳ đầy đủ", Icons.Rounded.AccessibilityNew),
        ExerciseChoiceUi("Squat", "Camera thấy toàn thân", Icons.Rounded.AccessibilityNew),
        ExerciseChoiceUi("Gập bụng", "Đặt máy ngang sàn", Icons.Rounded.SelfImprovement),
        ExerciseChoiceUi("Giữ squat", "10–60 giây", Icons.Rounded.Timer),
        ExerciseChoiceUi("Plank", "Cẳng tay · 10–60 giây", Icons.Rounded.SelfImprovement),
    )
    OnboardingScaffold(
        step = 5,
        title = "Hiệu chuẩn bài tập",
        subtitle = "Chọn một bài để thử camera, ánh sáng và vùng đặt điện thoại.",
        onBack = { onAction(OnboardingAction.Back) },
        footer = { RisePrimaryButton("Mở camera thử", { onAction(OnboardingAction.Continue) }, enabled = selected.isNotBlank(), leadingIcon = Icons.Rounded.CameraAlt) },
    ) {
        choices.forEach { item ->
            RiseCard(modifier = Modifier.padding(bottom = 10.dp), containerColor = if (item.name == selected) RiseSecondary.copy(alpha = 0.14f) else RiseSurface) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    RadioButton(selected = item.name == selected, onClick = { onAction(OnboardingAction.Select(item.name)) })
                    IconBadge(item.icon, if (item.name == selected) RiseSecondary else RiseTextMuted)
                    Column(Modifier.padding(start = 12.dp)) {
                        Text(item.name, style = MaterialTheme.typography.titleMedium)
                        Text(item.detail, color = RiseTextMuted)
                    }
                }
            }
        }
        RiseCard(containerColor = RiseWarning.copy(alpha = 0.08f)) {
            Text("Camera chỉ xử lý trực tiếp trên thiết bị. Không lưu ảnh hoặc video.", color = RiseWarning)
        }
    }
}

@Composable
fun QrSetupScreen(testPassed: Boolean, onAction: (OnboardingAction) -> Unit) {
    OnboardingScaffold(
        step = 6,
        title = "Đặt QR xa giường",
        subtitle = "Đặt mã ở phòng tắm hoặc bếp để buộc bạn rời khỏi giường.",
        onBack = { onAction(OnboardingAction.Back) },
        footer = { RisePrimaryButton(if (testPassed) "Tiếp tục" else "Quét thử QR", { onAction(if (testPassed) OnboardingAction.Continue else OnboardingAction.RunTest) }, leadingIcon = if (testPassed) Icons.Rounded.Check else Icons.Rounded.QrCode2) },
    ) {
        RiseCard {
            Box(
                Modifier.fillMaxWidth().height(260.dp).clip(MaterialTheme.shapes.large).background(Color.White),
                contentAlignment = Alignment.Center,
            ) {
                Icon(Icons.Rounded.QrCode2, "Mã QR của RISE", tint = Color.Black, modifier = Modifier.size(190.dp))
            }
            Spacer(Modifier.height(16.dp))
            Text("RISE-BATHROOM-01", style = MaterialTheme.typography.titleMedium)
            Text("Secret chỉ lưu cục bộ trên điện thoại.", color = RiseTextMuted)
        }
        Spacer(Modifier.height(16.dp))
        listOf(
            "Chụp màn hình hoặc in mã QR này",
            "Đặt mã ở vị trí cần đứng dậy để tới",
            "Quét thử trước khi kích hoạt báo thức",
        ).forEachIndexed { index, text ->
            Row(Modifier.padding(vertical = 9.dp), verticalAlignment = Alignment.CenterVertically) {
                Box(Modifier.size(30.dp).clip(CircleShape).background(RisePrimary.copy(alpha = 0.14f)), contentAlignment = Alignment.Center) {
                    Text("${index + 1}", color = RisePrimary, fontWeight = FontWeight.Bold)
                }
                Text(text, modifier = Modifier.padding(start = 12.dp))
            }
        }
        if (testPassed) {
            StatusChip("Đã quét thử thành công", RiseVerified, icon = Icons.Rounded.CheckCircle)
        }
    }
}

fun defaultPermissions() = listOf(
    PermissionItemUi("exact", "Báo thức chính xác", "Bắt buộc để reo đúng giờ", true, Icons.Rounded.Alarm),
    PermissionItemUi("notification", "Thông báo báo thức", "Phát âm thanh và hiển thị alarm", true, Icons.Rounded.NotificationsActive),
    PermissionItemUi("fullscreen", "Mở toàn màn hình", "Hiển thị khi máy đang khóa", false, Icons.Rounded.Fullscreen),
    PermissionItemUi("battery", "Tối ưu pin", "Hướng dẫn riêng theo hãng máy", false, Icons.Rounded.BatteryChargingFull),
    PermissionItemUi("camera", "Camera", "Chỉ xin khi chọn bài tập vision", false, Icons.Rounded.CameraAlt),
)
