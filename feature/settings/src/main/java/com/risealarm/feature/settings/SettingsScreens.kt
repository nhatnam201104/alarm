package com.risealarm.feature.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import com.risealarm.core.designsystem.*
import kotlinx.serialization.Serializable

@Serializable data object SettingsRoute
@Serializable data object PrivacyRoute
@Serializable data object ExerciseLibraryRoute
@Serializable data object OemHelpRoute

sealed interface SettingsAction {
    data class Open(val destination: String) : SettingsAction
    data class Toggle(val key: String, val enabled: Boolean) : SettingsAction
    data object Back : SettingsAction
    data object ExportData : SettingsAction
    data object DeleteData : SettingsAction
    data object OpenSystemSettings : SettingsAction
}

data class SettingsRowUi(val key: String, val title: String, val detail: String, val icon: ImageVector, val color: Color, val warning: Boolean = false)

@Composable
fun SettingsScreen(onAction: (SettingsAction) -> Unit, modifier: Modifier = Modifier) {
    val sections = listOf(
        "Alarm" to listOf(
            SettingsRowUi("sound", "Âm thanh & rung", "Morning Pulse · tăng dần", Icons.Rounded.VolumeUp, RisePrimary),
            SettingsRowUi("re-fire", "Nhịp re-fire", "30 giây cho tới khi hoàn thành", Icons.Rounded.Refresh, RiseWarning),
        ),
        "Độ tin cậy" to listOf(
            SettingsRowUi("permissions", "Quyền và độ tin cậy", "Thiếu quyền toàn màn hình", Icons.Rounded.Security, RiseWarning, true),
            SettingsRowUi("oem", "Trợ giúp thiết bị", "Pin, tự khởi động và OEM", Icons.Rounded.Smartphone, RiseSecondary),
        ),
        "Cá nhân hóa" to listOf(
            SettingsRowUi("exercise", "Bài tập & hiệu chuẩn", "4/5 bài sẵn sàng", Icons.Rounded.FitnessCenter, RiseVerified),
            SettingsRowUi("appearance", "Giao diện", "Tối · Midnight Performance", Icons.Rounded.DarkMode, RiseSecondary),
            SettingsRowUi("language", "Ngôn ngữ", "Tiếng Việt", Icons.Rounded.Language, RiseSecondary),
        ),
        "Dữ liệu" to listOf(
            SettingsRowUi("privacy", "Quyền riêng tư", "Xử lý camera trên thiết bị", Icons.Rounded.PrivacyTip, RiseVerified),
            SettingsRowUi("about", "Giới thiệu RISE", "UI prototype 0.1.0", Icons.Rounded.Info, RiseTextMuted),
        ),
    )
    LazyColumn(
        modifier = modifier.fillMaxSize().background(RiseBackground),
        contentPadding = PaddingValues(20.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        item {
            Text("Cài đặt", style = MaterialTheme.typography.headlineLarge)
            Text("Ứng dụng và độ tin cậy thiết bị", color = RiseTextMuted)
        }
        sections.forEach { (title, rows) ->
            item { Text(title, color = RiseTextMuted, style = MaterialTheme.typography.labelLarge, modifier = Modifier.padding(top = 10.dp)) }
            items(rows, key = { it.key }) { row ->
                SettingRow(row) { onAction(SettingsAction.Open(row.key)) }
            }
        }
        item { Spacer(Modifier.height(28.dp)) }
    }
}

@Composable
private fun SettingRow(row: SettingsRowUi, onClick: () -> Unit) {
    RiseCard(Modifier.fillMaxWidth().clickable(onClick = onClick)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconBadge(row.icon, row.color)
            Column(Modifier.weight(1f).padding(horizontal = 12.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(row.title, style = MaterialTheme.typography.titleMedium)
                    if (row.warning) {
                        Spacer(Modifier.width(8.dp))
                        Box(Modifier.size(8.dp).background(RiseWarning, androidx.compose.foundation.shape.CircleShape))
                    }
                }
                Text(row.detail, color = if (row.warning) RiseWarning else RiseTextMuted, style = MaterialTheme.typography.bodyMedium)
            }
            Icon(Icons.Rounded.ChevronRight, "Mở", tint = RiseTextMuted)
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PrivacyScreen(analyticsEnabled: Boolean, onAction: (SettingsAction) -> Unit) {
    Scaffold(
        containerColor = RiseBackground,
        topBar = { CenterAlignedTopAppBar(title = { Text("Quyền riêng tư") }, navigationIcon = { IconButton({ onAction(SettingsAction.Back) }) { Icon(Icons.Rounded.ArrowBack, "Quay lại") } }) },
    ) { padding ->
        LazyColumn(Modifier.fillMaxSize().padding(padding), contentPadding = PaddingValues(20.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
            item {
                RiseCard(containerColor = RiseVerified.copy(alpha = .08f)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        IconBadge(Icons.Rounded.Shield, RiseVerified)
                        Column(Modifier.padding(start = 12.dp)) {
                            Text("Camera luôn xử lý trên thiết bị", color = RiseVerified, style = MaterialTheme.typography.titleMedium)
                            Text("Không tải ảnh, video hoặc landmark lên máy chủ.", color = RiseTextMuted)
                        }
                    }
                }
            }
            item { PrivacyPoint(Icons.Rounded.CameraAlt, "Camera & pose", "Frame chỉ tồn tại trong bộ nhớ khi nhiệm vụ đang chạy.") }
            item { PrivacyPoint(Icons.Rounded.History, "Lịch sử cục bộ", "Lưu thời gian, kết quả và reason code; không lưu hình ảnh.") }
            item {
                RiseCard {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        IconBadge(Icons.Rounded.Analytics, RiseSecondary)
                        Column(Modifier.weight(1f).padding(start = 12.dp)) { Text("Analytics ẩn danh"); Text("Giúp cải thiện độ tin cậy", color = RiseTextMuted) }
                        Switch(analyticsEnabled, onCheckedChange = { onAction(SettingsAction.Toggle("analytics", it)) })
                    }
                }
            }
            item { OutlinedButton({ onAction(SettingsAction.ExportData) }, Modifier.fillMaxWidth().height(54.dp)) { Icon(Icons.Rounded.Download, null); Text("  Xuất dữ liệu cục bộ") } }
            item { OutlinedButton({ onAction(SettingsAction.DeleteData) }, Modifier.fillMaxWidth().height(54.dp), colors = ButtonDefaults.outlinedButtonColors(contentColor = RiseError)) { Icon(Icons.Rounded.DeleteForever, null); Text("  Xóa toàn bộ dữ liệu") } }
        }
    }
}

@Composable
private fun PrivacyPoint(icon: ImageVector, title: String, detail: String) {
    RiseCard {
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconBadge(icon, RiseSecondary)
            Column(Modifier.padding(start = 12.dp)) { Text(title, style = MaterialTheme.typography.titleMedium); Text(detail, color = RiseTextMuted) }
        }
    }
}

data class ExerciseLibraryItemUi(val name: String, val detail: String, val ready: Boolean, val icon: ImageVector)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ExerciseLibraryScreen(onAction: (SettingsAction) -> Unit) {
    val items = listOf(
        ExerciseLibraryItemUi("Push-up", "Đã hiệu chuẩn · 24 ngày", true, Icons.Rounded.FitnessCenter),
        ExerciseLibraryItemUi("Squat", "Đã hiệu chuẩn · 24 ngày", true, Icons.Rounded.AccessibilityNew),
        ExerciseLibraryItemUi("Gập bụng", "Cần hiệu chuẩn", false, Icons.Rounded.SelfImprovement),
        ExerciseLibraryItemUi("Giữ squat", "Đã hiệu chuẩn · 18 ngày", true, Icons.Rounded.Timer),
        ExerciseLibraryItemUi("Plank", "Đã hiệu chuẩn · 18 ngày", true, Icons.Rounded.SelfImprovement),
    )
    Scaffold(
        containerColor = RiseBackground,
        topBar = { CenterAlignedTopAppBar(title = { Text("Bài tập & hiệu chuẩn") }, navigationIcon = { IconButton({ onAction(SettingsAction.Back) }) { Icon(Icons.Rounded.ArrowBack, "Quay lại") } }) },
    ) { padding ->
        LazyColumn(Modifier.fillMaxSize().padding(padding), contentPadding = PaddingValues(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            item { Text("Vision missions", color = RiseTextMuted) }
            items(items) { item ->
                RiseCard(Modifier.clickable { onAction(SettingsAction.Open("calibrate:" + item.name)) }) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        IconBadge(item.icon, if (item.ready) RiseVerified else RiseWarning)
                        Column(Modifier.weight(1f).padding(horizontal = 12.dp)) {
                            Text(item.name, style = MaterialTheme.typography.titleMedium)
                            Text(item.detail, color = if (item.ready) RiseVerified else RiseWarning)
                        }
                        Icon(Icons.Rounded.ChevronRight, "Hiệu chuẩn")
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OemHelpScreen(onAction: (SettingsAction) -> Unit) {
    Scaffold(
        containerColor = RiseBackground,
        topBar = { CenterAlignedTopAppBar(title = { Text("Trợ giúp thiết bị") }, navigationIcon = { IconButton({ onAction(SettingsAction.Back) }) { Icon(Icons.Rounded.ArrowBack, "Quay lại") } }) },
    ) { padding ->
        LazyColumn(Modifier.fillMaxSize().padding(padding), contentPadding = PaddingValues(20.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
            item {
                RiseCard(containerColor = RiseWarning.copy(alpha = .08f)) {
                    Text("Samsung · Android 15", color = RiseWarning, style = MaterialTheme.typography.titleMedium)
                    Text("RISE phát hiện thiết bị có thể giới hạn ứng dụng nền.", color = RiseTextMuted)
                }
            }
            items(listOf(
                Triple("Cho phép chạy nền", "Pin → Không hạn chế", Icons.Rounded.BatteryChargingFull),
                Triple("Cho phép toàn màn hình", "Quyền đặc biệt → Alarm toàn màn hình", Icons.Rounded.Fullscreen),
                Triple("Giữ kênh alarm", "Thông báo → RISE Alarm → Bật", Icons.Rounded.NotificationsActive),
                Triple("Không Force Stop", "Android chặn mọi alarm sau Force Stop", Icons.Rounded.StopCircle),
            )) { (title, detail, icon) ->
                RiseCard {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        IconBadge(icon, RiseSecondary)
                        Column(Modifier.weight(1f).padding(start = 12.dp)) { Text(title, style = MaterialTheme.typography.titleMedium); Text(detail, color = RiseTextMuted) }
                    }
                }
            }
            item { RisePrimaryButton("Mở cài đặt hệ thống", { onAction(SettingsAction.OpenSystemSettings) }, leadingIcon = Icons.Rounded.Settings) }
        }
    }
}

