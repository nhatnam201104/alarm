package com.risealarm.feature.alarms

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.risealarm.core.designsystem.*
import kotlinx.serialization.Serializable

@Serializable data object AlarmListRoute
@Serializable data class AlarmEditorRoute(val alarmId: String? = null)
@Serializable data object AlarmReviewRoute
@Serializable data object ReadinessDiagnosticsRoute

sealed interface AlarmAction {
    data object Add : AlarmAction
    data class Edit(val id: String) : AlarmAction
    data class Toggle(val id: String, val enabled: Boolean) : AlarmAction
    data class Delete(val id: String) : AlarmAction
    data object Continue : AlarmAction
    data object OpenProtocol : AlarmAction
    data object OpenDiagnostics : AlarmAction
    data object OpenSettings : AlarmAction
    data object TestAlarm : AlarmAction
    data object Save : AlarmAction
    data object Back : AlarmAction
}

@Immutable
data class AlarmItemUi(
    val id: String,
    val time: String,
    val label: String,
    val days: String,
    val protocol: String,
    val enabled: Boolean,
    val ready: Boolean,
)

@Immutable
data class AlarmListUiState(
    val alarms: List<AlarmItemUi> = sampleAlarms(),
    val isLoading: Boolean = false,
    val showEmpty: Boolean = false,
)

@Composable
fun AlarmListScreen(state: AlarmListUiState, onAction: (AlarmAction) -> Unit, modifier: Modifier = Modifier) {
    Box(modifier.fillMaxSize().background(RiseBackground)) {
        when {
            state.isLoading -> LoadingAlarms()
            state.showEmpty || state.alarms.isEmpty() -> EmptyAlarms { onAction(AlarmAction.Add) }
            else -> LazyColumn(
                Modifier.fillMaxSize(),
                contentPadding = PaddingValues(start = 20.dp, end = 20.dp, top = 20.dp, bottom = 110.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp),
            ) {
                item {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Column(Modifier.weight(1f)) {
                            Text("Báo thức", style = MaterialTheme.typography.headlineLarge)
                            Text("3 alarm · 2 đang bật", color = RiseTextMuted)
                        }
                        IconButton(onClick = { onAction(AlarmAction.OpenDiagnostics) }) {
                            Icon(Icons.Rounded.HealthAndSafety, "Kiểm tra độ tin cậy", tint = RiseWarning)
                        }
                    }
                }
                item {
                    RiseCard(containerColor = RiseVerified.copy(alpha = 0.08f)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Rounded.Verified, null, tint = RiseVerified)
                            Column(Modifier.weight(1f).padding(start = 12.dp)) {
                                Text("Sẵn sàng cho 06:30", color = RiseVerified, style = MaterialTheme.typography.titleMedium)
                                Text("Quyền và protocol đã được kiểm tra", color = RiseTextMuted)
                            }
                        }
                    }
                }
                items(state.alarms, key = { it.id }) { alarm ->
                    AlarmCard(alarm, onAction)
                }
            }
        }
        FloatingActionButton(
            onClick = { onAction(AlarmAction.Add) },
            modifier = Modifier.align(Alignment.BottomEnd).padding(24.dp),
            containerColor = RisePrimary,
            contentColor = Color(0xFF261007),
            shape = CircleShape,
        ) { Icon(Icons.Rounded.Add, "Thêm báo thức") }
    }
}

@Composable
private fun AlarmCard(alarm: AlarmItemUi, onAction: (AlarmAction) -> Unit) {
    RiseCard(Modifier.fillMaxWidth().clickable { onAction(AlarmAction.Edit(alarm.id)) }) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text(alarm.time, style = MaterialTheme.typography.displayMedium, color = if (alarm.enabled) MaterialTheme.colorScheme.onSurface else RiseTextMuted)
                Text(alarm.label, style = MaterialTheme.typography.titleMedium)
            }
            Switch(checked = alarm.enabled, onCheckedChange = { onAction(AlarmAction.Toggle(alarm.id, it)) })
        }
        Spacer(Modifier.height(12.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(alarm.days, color = RiseTextMuted, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.weight(1f))
            StatusChip(if (alarm.ready) "Sẵn sàng" else "Cần kiểm tra", if (alarm.ready) RiseVerified else RiseWarning)
        }
        Spacer(Modifier.height(12.dp))
        HorizontalDivider(color = RiseOutline)
        Spacer(Modifier.height(12.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Rounded.AccountTree, null, tint = RiseSecondary, modifier = Modifier.size(18.dp))
            Text(alarm.protocol, modifier = Modifier.padding(start = 8.dp), color = RiseTextMuted, style = MaterialTheme.typography.bodyMedium)
        }
    }
}

@Composable
private fun EmptyAlarms(onAdd: () -> Unit) {
    Column(Modifier.fillMaxSize().padding(32.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
        IconBadge(Icons.Rounded.AlarmAdd, RisePrimary, size = 84.dp)
        Spacer(Modifier.height(22.dp))
        Text("Chưa có báo thức", style = MaterialTheme.typography.headlineMedium)
        Text("Tạo alarm đầu tiên và chọn cách RISE xác minh rằng bạn đã thức dậy.", color = RiseTextMuted, textAlign = TextAlign.Center)
        Spacer(Modifier.height(24.dp))
        RisePrimaryButton("Tạo báo thức đầu tiên", onAdd, leadingIcon = Icons.Rounded.AddAlarm)
    }
}

@Composable
private fun LoadingAlarms() {
    Column(Modifier.fillMaxSize().padding(20.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
        Text("Báo thức", style = MaterialTheme.typography.headlineLarge)
        repeat(3) {
            Box(Modifier.fillMaxWidth().height(150.dp).clip(MaterialTheme.shapes.large).background(RiseSurfaceHigh))
        }
    }
}

@Immutable
data class AlarmEditorUiState(
    val time: String = "06:30",
    val label: String = "Thức dậy đi làm",
    val selectedDays: Set<String> = setOf("T2", "T3", "T4", "T5", "T6"),
    val sound: String = "Morning Pulse",
    val vibration: Boolean = true,
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AlarmEditorScreen(state: AlarmEditorUiState, onAction: (AlarmAction) -> Unit) {
    Scaffold(
        containerColor = RiseBackground,
        topBar = { CenterAlignedTopAppBar(title = { Text("Thêm báo thức") }, navigationIcon = { IconButton({ onAction(AlarmAction.Back) }) { Icon(Icons.Rounded.Close, "Đóng") } }) },
        bottomBar = { Box(Modifier.padding(20.dp)) { RisePrimaryButton("Tiếp tục", { onAction(AlarmAction.Continue) }) } },
    ) { padding ->
        LazyColumn(Modifier.fillMaxSize().padding(padding), contentPadding = PaddingValues(horizontal = 20.dp, vertical = 16.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
            item {
                RiseCard(containerColor = Color(0xFF171724)) {
                    Text("Thời gian", color = RiseTextMuted)
                    Box(Modifier.fillMaxWidth().padding(vertical = 26.dp), contentAlignment = Alignment.Center) {
                        Text(state.time, style = MaterialTheme.typography.displayLarge)
                    }
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Center) {
                        StatusChip("AM", RisePrimary)
                    }
                }
            }
            item {
                Text("Lặp lại", style = MaterialTheme.typography.titleMedium)
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    listOf("T2", "T3", "T4", "T5", "T6", "T7", "CN").forEach { day ->
                        FilterChip(selected = day in state.selectedDays, onClick = {}, label = { Text(day) })
                    }
                }
            }
            item { EditorField("Nhãn", state.label, Icons.Rounded.Label) }
            item { EditorField("Âm thanh", state.sound, Icons.Rounded.MusicNote) }
            item {
                RiseCard {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        IconBadge(Icons.Rounded.Vibration, RiseSecondary)
                        Column(Modifier.weight(1f).padding(start = 12.dp)) { Text("Rung", style = MaterialTheme.typography.titleMedium); Text("Nhịp tăng dần", color = RiseTextMuted) }
                        Switch(state.vibration, onCheckedChange = {})
                    }
                }
            }
            item {
                RiseCard(Modifier.clickable { onAction(AlarmAction.OpenProtocol) }, containerColor = RiseSecondary.copy(alpha = 0.10f)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        IconBadge(Icons.Rounded.AccountTree, RiseSecondary)
                        Column(Modifier.weight(1f).padding(horizontal = 12.dp)) {
                            Text("Wake Protocol", style = MaterialTheme.typography.titleMedium)
                            Text("QR phòng tắm · Squat 15 · Toán 3 câu", color = RiseTextMuted)
                        }
                        Icon(Icons.Rounded.ChevronRight, "Chỉnh protocol")
                    }
                }
            }
            item { Spacer(Modifier.height(90.dp)) }
        }
    }
}

@Composable
private fun EditorField(label: String, value: String, icon: ImageVector) {
    RiseCard {
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconBadge(icon, RisePrimary)
            Column(Modifier.weight(1f).padding(start = 12.dp)) { Text(label, color = RiseTextMuted); Text(value, style = MaterialTheme.typography.titleMedium) }
            Icon(Icons.Rounded.ChevronRight, "Chỉnh " + label, tint = RiseTextMuted)
        }
    }
}

data class ReadinessCheckUi(val label: String, val detail: String, val status: CheckStatus, val icon: ImageVector)
enum class CheckStatus { Ready, Warning, Blocked }

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AlarmReviewScreen(checks: List<ReadinessCheckUi>, onAction: (AlarmAction) -> Unit) {
    val canArm = checks.none { it.status == CheckStatus.Blocked }
    Scaffold(
        containerColor = RiseBackground,
        topBar = { CenterAlignedTopAppBar(title = { Text("Kiểm tra báo thức") }, navigationIcon = { IconButton({ onAction(AlarmAction.Back) }) { Icon(Icons.Rounded.ArrowBack, "Quay lại") } }) },
        bottomBar = { Box(Modifier.padding(20.dp)) { RisePrimaryButton(if (canArm) "Kích hoạt báo thức" else "Khắc phục để tiếp tục", { onAction(if (canArm) AlarmAction.Save else AlarmAction.OpenDiagnostics) }, enabled = canArm) } },
    ) { padding ->
        LazyColumn(Modifier.fillMaxSize().padding(padding), contentPadding = PaddingValues(20.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
            item {
                RiseCard {
                    Row(verticalAlignment = Alignment.Bottom) { Text("06:30", style = MaterialTheme.typography.displayMedium); Text("  T2–T6", color = RiseTextMuted, modifier = Modifier.padding(bottom = 8.dp)) }
                    Text("Thức dậy đi làm", style = MaterialTheme.typography.titleMedium)
                    Spacer(Modifier.height(12.dp))
                    Text("QR phòng tắm → Squat 15 → Toán 3 câu", color = RiseTextMuted)
                }
            }
            item { SectionHeader("Độ sẵn sàng") }
            items(checks) { check -> CheckRow(check) }
            item { Spacer(Modifier.height(90.dp)) }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReadinessDiagnosticsScreen(checks: List<ReadinessCheckUi>, onAction: (AlarmAction) -> Unit) {
    Scaffold(
        containerColor = RiseBackground,
        topBar = { CenterAlignedTopAppBar(title = { Text("Độ tin cậy thiết bị") }, navigationIcon = { IconButton({ onAction(AlarmAction.Back) }) { Icon(Icons.Rounded.ArrowBack, "Quay lại") } }) },
    ) { padding ->
        LazyColumn(Modifier.fillMaxSize().padding(padding), contentPadding = PaddingValues(20.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
            item {
                RiseCard(containerColor = RiseWarning.copy(alpha = 0.08f)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        IconBadge(Icons.Rounded.HealthAndSafety, RiseWarning)
                        Column(Modifier.padding(start = 12.dp)) { Text("1 mục cần chú ý", color = RiseWarning, style = MaterialTheme.typography.titleMedium); Text("Hãy sửa trước giờ đi ngủ", color = RiseTextMuted) }
                    }
                }
            }
            items(checks) { check -> CheckRow(check, showDetail = true) }
            item { RisePrimaryButton("Chạy alarm test 30 giây", { onAction(AlarmAction.TestAlarm) }, leadingIcon = Icons.Rounded.AlarmOn) }
        }
    }
}

@Composable
private fun CheckRow(check: ReadinessCheckUi, showDetail: Boolean = false) {
    val color = when (check.status) { CheckStatus.Ready -> RiseVerified; CheckStatus.Warning -> RiseWarning; CheckStatus.Blocked -> RiseError }
    RiseCard(containerColor = if (check.status == CheckStatus.Ready) RiseSurface else color.copy(alpha = 0.08f)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconBadge(check.icon, color)
            Column(Modifier.weight(1f).padding(horizontal = 12.dp)) {
                Text(check.label, style = MaterialTheme.typography.titleMedium)
                Text(check.detail, color = RiseTextMuted, style = MaterialTheme.typography.bodyMedium)
            }
            Icon(if (check.status == CheckStatus.Ready) Icons.Rounded.CheckCircle else Icons.Rounded.ChevronRight, null, tint = color)
        }
        if (showDetail && check.status != CheckStatus.Ready) {
            Spacer(Modifier.height(12.dp))
            Text("Chạm để mở cài đặt hệ thống và kiểm tra lại.", color = color)
        }
    }
}

fun sampleAlarms() = listOf(
    AlarmItemUi("a1", "06:30", "Đi làm", "T2 – T6", "QR · Squat · Toán", true, true),
    AlarmItemUi("a2", "07:15", "Cuối tuần", "T7, CN", "QR · Plank", true, false),
    AlarmItemUi("a3", "05:45", "Chạy sáng", "T3, T5", "Bước chân · Toán", false, true),
)

fun sampleReadinessChecks() = listOf(
    ReadinessCheckUi("Exact alarm", "Được phép đặt báo thức chính xác", CheckStatus.Ready, Icons.Rounded.AlarmOn),
    ReadinessCheckUi("Thông báo & âm thanh", "Kênh alarm đang hoạt động", CheckStatus.Ready, Icons.Rounded.NotificationsActive),
    ReadinessCheckUi("Toàn màn hình", "Cần cấp quyền trên Android 14+", CheckStatus.Warning, Icons.Rounded.Fullscreen),
    ReadinessCheckUi("Pin & tự khởi động", "Không bị giới hạn nền", CheckStatus.Ready, Icons.Rounded.BatteryChargingFull),
    ReadinessCheckUi("Hiệu chuẩn Squat", "Còn hiệu lực 24 ngày", CheckStatus.Ready, Icons.Rounded.AccessibilityNew),
)

