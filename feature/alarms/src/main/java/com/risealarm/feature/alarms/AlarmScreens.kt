package com.risealarm.feature.alarms

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.pager.PageSize
import androidx.compose.foundation.pager.VerticalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.risealarm.core.designsystem.*
import kotlinx.serialization.Serializable
import kotlin.math.absoluteValue

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
    data class SetTime(val hour: Int, val minute: Int) : AlarmAction
    data class SetLabel(val value: String) : AlarmAction
    data class ToggleDay(val isoDay: Int) : AlarmAction
    data class SetVibration(val enabled: Boolean) : AlarmAction
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
    val readinessReady: Boolean = true,
    val readinessTitle: String = "Sẵn sàng cho 06:30",
    val readinessDetail: String = "Quyền và hệ thống báo thức đã được kiểm tra",
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
                            Text("${state.alarms.size} báo thức · ${state.alarms.count { it.enabled }} đang bật", color = RiseTextMuted)
                        }
                        IconButton(onClick = { onAction(AlarmAction.OpenDiagnostics) }) {
                            Icon(Icons.Rounded.HealthAndSafety, "Kiểm tra độ tin cậy", tint = RiseWarning)
                        }
                    }
                }
                item {
                    val readinessColor = if (state.readinessReady) RiseVerified else RiseWarning
                    RiseCard(containerColor = readinessColor.copy(alpha = 0.08f)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(if (state.readinessReady) Icons.Rounded.Verified else Icons.Rounded.Warning, null, tint = readinessColor)
                            Column(Modifier.weight(1f).padding(start = 12.dp)) {
                                Text(state.readinessTitle, color = readinessColor, style = MaterialTheme.typography.titleMedium)
                                Text(state.readinessDetail, color = RiseTextMuted)
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
    val id: String? = null,
    val hour: Int = 6,
    val minute: Int = 30,
    val label: String = "Thức dậy đi làm",
    val selectedDays: Set<Int> = setOf(1, 2, 3, 4, 5),
    val vibration: Boolean = true,
    val enabled: Boolean = true,
    val isSaving: Boolean = false,
    val error: String? = null,
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AlarmEditorScreen(state: AlarmEditorUiState, onAction: (AlarmAction) -> Unit) {
    Scaffold(
        containerColor = RiseBackground,
        topBar = { CenterAlignedTopAppBar(title = { Text(if (state.id == null) "Thêm báo thức" else "Sửa báo thức") }, navigationIcon = { IconButton({ onAction(AlarmAction.Back) }) { Icon(Icons.Rounded.Close, "Đóng") } }) },
        bottomBar = { Box(Modifier.padding(20.dp)) { RisePrimaryButton(if (state.isSaving) "Đang lưu…" else "Lưu báo thức", { onAction(AlarmAction.Save) }, enabled = !state.isSaving && state.label.isNotBlank()) } },
    ) { padding ->
        LazyColumn(Modifier.fillMaxSize().padding(padding), contentPadding = PaddingValues(horizontal = 20.dp, vertical = 16.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
            item {
                RiseCard(containerColor = Color(0xFF171724)) {
                    Text("Thời gian", style = MaterialTheme.typography.titleMedium)
                    Text("Cuộn từng cột để chọn", color = RiseTextMuted, style = MaterialTheme.typography.bodyMedium)
                    Spacer(Modifier.height(12.dp))
                    TimeWheelPicker(
                        hour = state.hour,
                        minute = state.minute,
                        onTimeChange = { hour, minute -> onAction(AlarmAction.SetTime(hour, minute)) },
                    )
                }
            }
            item {
                Text("Lặp lại", style = MaterialTheme.typography.titleMedium)
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    listOf("T2", "T3", "T4", "T5", "T6", "T7", "CN").forEachIndexed { index, day ->
                        FilterChip(selected = index + 1 in state.selectedDays, onClick = { onAction(AlarmAction.ToggleDay(index + 1)) }, label = { Text(day) })
                    }
                }
            }
            item {
                OutlinedTextField(
                    value = state.label,
                    onValueChange = { onAction(AlarmAction.SetLabel(it)) },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("Nhãn") },
                    leadingIcon = { Icon(Icons.Rounded.Label, null) },
                    singleLine = true,
                )
            }
            item {
                RiseCard {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        IconBadge(Icons.Rounded.Vibration, RiseSecondary)
                        Column(Modifier.weight(1f).padding(start = 12.dp)) { Text("Rung", style = MaterialTheme.typography.titleMedium); Text("Nhịp tăng dần", color = RiseTextMuted) }
                        Switch(state.vibration, onCheckedChange = { onAction(AlarmAction.SetVibration(it)) })
                    }
                }
            }
            item {
                RiseCard(containerColor = RiseSecondary.copy(alpha = 0.10f)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        IconBadge(Icons.Rounded.TouchApp, RiseSecondary)
                        Column(Modifier.weight(1f).padding(horizontal = 12.dp)) {
                            Text("Cách tắt trong MVP", style = MaterialTheme.typography.titleMedium)
                            Text("Giữ nút 3 giây. Vision sẽ được thêm ở phiên bản sau.", color = RiseTextMuted)
                        }
                    }
                }
            }
            state.error?.let { message -> item { Text(message, color = RiseError) } }
            if (state.id != null) {
                item {
                    OutlinedButton(
                        onClick = { onAction(AlarmAction.Delete(state.id)) },
                        modifier = Modifier.fillMaxWidth().height(54.dp),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = RiseError),
                    ) { Icon(Icons.Rounded.Delete, null); Text("  Xóa báo thức") }
                }
            }
            item { Spacer(Modifier.height(90.dp)) }
        }
    }
}

@Composable
private fun TimeWheelPicker(
    hour: Int,
    minute: Int,
    onTimeChange: (hour: Int, minute: Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier.fillMaxWidth()) {
        Box(Modifier.fillMaxWidth().height(WheelViewportHeight)) {
            Box(
                Modifier
                    .align(Alignment.Center)
                    .fillMaxWidth()
                    .height(WheelItemHeight)
                    .clip(RoundedCornerShape(16.dp))
                    .background(RisePrimary.copy(alpha = 0.11f)),
            )
            Column(Modifier.align(Alignment.Center).fillMaxWidth()) {
                HorizontalDivider(color = RisePrimary.copy(alpha = 0.24f))
                Spacer(Modifier.height(WheelItemHeight))
                HorizontalDivider(color = RisePrimary.copy(alpha = 0.24f))
            }
            Row(Modifier.fillMaxSize(), verticalAlignment = Alignment.CenterVertically) {
                WheelColumn(
                    selected = hour,
                    values = 0..23,
                    label = "Giờ",
                    onValueChange = { onTimeChange(it, minute) },
                    modifier = Modifier.weight(1f),
                )
                Text(
                    ":",
                    style = MaterialTheme.typography.displaySmall,
                    fontWeight = FontWeight.SemiBold,
                    color = RisePrimary,
                    modifier = Modifier.padding(horizontal = 4.dp),
                )
                WheelColumn(
                    selected = minute,
                    values = 0..59,
                    label = "Phút",
                    onValueChange = { onTimeChange(hour, it) },
                    modifier = Modifier.weight(1f),
                )
            }
        }
        Row(Modifier.fillMaxWidth().padding(top = 4.dp)) {
            Text("GIỜ", color = RiseTextMuted, style = MaterialTheme.typography.labelSmall, textAlign = TextAlign.Center, modifier = Modifier.weight(1f))
            Spacer(Modifier.width(36.dp))
            Text("PHÚT", color = RiseTextMuted, style = MaterialTheme.typography.labelSmall, textAlign = TextAlign.Center, modifier = Modifier.weight(1f))
        }
    }
}

@Composable
private fun WheelColumn(
    selected: Int,
    values: IntRange,
    label: String,
    onValueChange: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    val valueCount = values.count()
    val selectedIndex = (selected - values.first).coerceIn(0, valueCount - 1)
    val middlePage = WheelPageCount / 2
    val initialPage = remember(values, selectedIndex) {
        middlePage - (middlePage % valueCount) + selectedIndex
    }
    val pagerState = rememberPagerState(initialPage = initialPage, pageCount = { WheelPageCount })
    val hapticFeedback = LocalHapticFeedback.current
    var readyForFeedback by remember { mutableStateOf(false) }

    LaunchedEffect(pagerState.settledPage) {
        val settledValue = values.first + (pagerState.settledPage % valueCount)
        if (readyForFeedback && settledValue != selected) {
            hapticFeedback.performHapticFeedback(HapticFeedbackType.TextHandleMove)
            onValueChange(settledValue)
        }
        readyForFeedback = true
    }

    LaunchedEffect(selected) {
        val currentIndex = pagerState.settledPage % valueCount
        var delta = selectedIndex - currentIndex
        if (delta > valueCount / 2) delta -= valueCount
        if (delta < -valueCount / 2) delta += valueCount
        if (delta != 0 && !pagerState.isScrollInProgress) {
            pagerState.scrollToPage(pagerState.settledPage + delta)
        }
    }

    VerticalPager(
        state = pagerState,
        modifier = modifier
            .fillMaxHeight()
            .semantics { contentDescription = "$label, ${selected.toString().padStart(2, '0')}" },
        pageSize = PageSize.Fixed(WheelItemHeight),
        contentPadding = PaddingValues(vertical = WheelContentPadding),
        beyondViewportPageCount = 2,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) { page ->
        val signedPageOffset = (pagerState.currentPage - page) + pagerState.currentPageOffsetFraction
        val pageOffset = signedPageOffset.absoluteValue
        val proximity = 1f - (pageOffset.coerceIn(0f, 2f) / 2f)
        val value = values.first + (page % valueCount)
        Box(
            Modifier
                .fillMaxWidth()
                .height(WheelItemHeight)
                .graphicsLayer {
                    alpha = 0.30f + (0.70f * proximity)
                    scaleX = 0.86f + (0.14f * proximity)
                    scaleY = 0.86f + (0.14f * proximity)
                    rotationX = signedPageOffset.coerceIn(-2f, 2f) * -14f
                    cameraDistance = 24f * density
                },
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text = value.toString().padStart(2, '0'),
                style = MaterialTheme.typography.displaySmall,
                fontWeight = if (pageOffset < 0.5f) FontWeight.SemiBold else FontWeight.Normal,
                color = if (pageOffset < 0.5f) RiseText else RiseTextMuted,
            )
        }
    }
}

private val WheelItemHeight = 56.dp
private val WheelViewportHeight = 280.dp
private val WheelContentPadding = 112.dp
private const val WheelPageCount = 10_000

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
