package com.risealarm.feature.home

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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.risealarm.core.designsystem.*
import kotlinx.serialization.Serializable

@Serializable data object HomeRoute
@Serializable data object HistoryRoute
@Serializable data class SessionDetailRoute(val sessionId: String)
@Serializable data class AchievementDetailRoute(val achievementId: String)

sealed interface HomeAction {
    data object OpenNextAlarm : HomeAction
    data object OpenHistory : HomeAction
    data class OpenSession(val id: String) : HomeAction
    data class OpenAchievement(val id: String) : HomeAction
    data object Back : HomeAction
}

enum class SessionOutcome { Completed, Escaped, TechnicalError }

@Immutable
data class WakeSessionUi(
    val id: String,
    val day: String,
    val time: String,
    val duration: String,
    val protocol: String,
    val outcome: SessionOutcome,
)

@Immutable
data class HomeUiState(
    val nextAlarm: String = "06:30",
    val ready: Boolean = true,
    val currentStreak: Int = 12,
    val bestStreak: Int = 21,
    val successRate: Int = 86,
    val weekly: List<Int> = listOf(72, 88, 100, 65, 92, 100, 86),
    val sessions: List<WakeSessionUi> = sampleSessions(),
)

@Composable
fun HomeDashboardScreen(state: HomeUiState, onAction: (HomeAction) -> Unit, modifier: Modifier = Modifier) {
    LazyColumn(
        modifier = modifier.fillMaxSize().background(RiseBackground),
        contentPadding = PaddingValues(20.dp),
        verticalArrangement = Arrangement.spacedBy(18.dp),
    ) {
        item {
            Text("Chào buổi tối,", color = RiseTextMuted, style = MaterialTheme.typography.bodyLarge)
            Text("Sẵn sàng cho ngày mai?", style = MaterialTheme.typography.headlineLarge)
        }
        item {
            RiseCard(Modifier.fillMaxWidth().clickable { onAction(HomeAction.OpenNextAlarm) }, Color(0xFF171724)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("Báo thức tiếp theo", color = RiseTextMuted, modifier = Modifier.weight(1f))
                    StatusChip(if (state.ready) "Sẵn sàng" else "Cần kiểm tra", if (state.ready) RiseVerified else RiseWarning, icon = if (state.ready) Icons.Rounded.CheckCircle else Icons.Rounded.ErrorOutline)
                }
                Spacer(Modifier.height(14.dp))
                Row(verticalAlignment = Alignment.Bottom) {
                    Text(state.nextAlarm, style = MaterialTheme.typography.displayLarge)
                    Text(" AM", color = RiseTextMuted, modifier = Modifier.padding(bottom = 10.dp))
                    Spacer(Modifier.weight(1f))
                    Icon(Icons.Rounded.ChevronRight, "Mở báo thức", tint = RiseTextMuted)
                }
                Text("Ngày mai · Đi làm", color = RiseTextMuted)
                Spacer(Modifier.height(14.dp))
                HorizontalDivider(color = RiseOutline)
                Spacer(Modifier.height(12.dp))
                Text("▣ QR phòng tắm  ·  Squat 15  ·  Toán 3 câu", style = MaterialTheme.typography.bodyMedium)
            }
        }
        item {
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                MetricCard("Chuỗi hiện tại", state.currentStreak.toString() + " ngày", Icons.Rounded.LocalFireDepartment, RisePrimary, "Kỷ lục " + state.bestStreak + " ngày", Modifier.weight(1f))
                MetricCard("Tỷ lệ thành công", state.successRate.toString() + "%", Icons.Rounded.CheckCircle, RiseVerified, "+2% tuần này", Modifier.weight(1f))
            }
        }
        item {
            RiseCard {
                SectionHeader("Thống kê 7 ngày")
                Spacer(Modifier.height(20.dp))
                WeeklyBars(state.weekly)
            }
        }
        item {
            SectionHeader("Huy hiệu", action = "Xem tất cả")
            Spacer(Modifier.height(12.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                AchievementMini("early", "Chim sớm", Icons.Rounded.Bolt, RiseWarning, Modifier.weight(1f), onAction)
                AchievementMini("streak", "Chuỗi 7", Icons.Rounded.LocalFireDepartment, RisePrimary, Modifier.weight(1f), onAction)
                AchievementMini("steady", "Bền bỉ", Icons.Rounded.EmojiEvents, RiseSecondary, Modifier.weight(1f), onAction)
            }
        }
        item { SectionHeader("Phiên gần đây", action = "Xem tất cả") }
        items(state.sessions.take(5), key = { it.id }) { session ->
            SessionRow(session) { onAction(HomeAction.OpenSession(session.id)) }
        }
        item { Spacer(Modifier.height(24.dp)) }
    }
}

@Composable
private fun WeeklyBars(values: List<Int>) {
    Row(Modifier.fillMaxWidth().height(128.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.Bottom) {
        values.forEachIndexed { index, value ->
            Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Bottom, modifier = Modifier.height(128.dp)) {
                Box(
                    Modifier.size(24.dp, (value * 0.85f).dp).clip(CircleShape)
                        .background(if (index == values.lastIndex) RisePrimary else RiseSecondary.copy(alpha = 0.55f)),
                )
                Spacer(Modifier.height(8.dp))
                Text(listOf("T2", "T3", "T4", "T5", "T6", "T7", "CN")[index], color = RiseTextMuted, style = MaterialTheme.typography.labelMedium)
            }
        }
    }
}

@Composable
private fun AchievementMini(id: String, title: String, icon: androidx.compose.ui.graphics.vector.ImageVector, color: Color, modifier: Modifier, onAction: (HomeAction) -> Unit) {
    RiseCard(modifier.clickable { onAction(HomeAction.OpenAchievement(id)) }, RiseSurfaceHigh) {
        IconBadge(icon, color, size = 38.dp)
        Spacer(Modifier.height(8.dp))
        Text(title, style = MaterialTheme.typography.labelLarge, maxLines = 1)
    }
}

@Composable
private fun SessionRow(session: WakeSessionUi, onClick: () -> Unit) {
    RiseCard(Modifier.fillMaxWidth().clickable(onClick = onClick)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            val color = when (session.outcome) { SessionOutcome.Completed -> RiseVerified; SessionOutcome.Escaped -> RiseWarning; SessionOutcome.TechnicalError -> RiseTextMuted }
            val icon = when (session.outcome) { SessionOutcome.Completed -> Icons.Rounded.CheckCircle; SessionOutcome.Escaped -> Icons.Rounded.ErrorOutline; SessionOutcome.TechnicalError -> Icons.Rounded.Timer }
            IconBadge(icon, color)
            Column(Modifier.weight(1f).padding(horizontal = 12.dp)) {
                Text(session.day + " · " + session.time, style = MaterialTheme.typography.titleMedium)
                Text(session.protocol + " · " + session.duration, color = RiseTextMuted, style = MaterialTheme.typography.bodyMedium)
            }
            Icon(Icons.Rounded.ChevronRight, "Chi tiết", tint = RiseTextMuted)
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FullHistoryScreen(sessions: List<WakeSessionUi>, onAction: (HomeAction) -> Unit) {
    Scaffold(
        containerColor = RiseBackground,
        topBar = { CenterAlignedTopAppBar(title = { Text("Lịch sử thức dậy") }, navigationIcon = { IconButton({ onAction(HomeAction.Back) }) { Icon(Icons.Rounded.ArrowBack, "Quay lại") } }) },
    ) { padding ->
        LazyColumn(Modifier.fillMaxSize().padding(padding), contentPadding = PaddingValues(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            item { Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) { StatusChip("30 ngày", RisePrimary); StatusChip("Tất cả kết quả", RiseSecondary) } }
            items(sessions, key = { it.id }) { SessionRow(it) { onAction(HomeAction.OpenSession(it.id)) } }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SessionDetailScreen(session: WakeSessionUi, onAction: (HomeAction) -> Unit) {
    Scaffold(
        containerColor = RiseBackground,
        topBar = { CenterAlignedTopAppBar(title = { Text("Chi tiết phiên") }, navigationIcon = { IconButton({ onAction(HomeAction.Back) }) { Icon(Icons.Rounded.ArrowBack, "Quay lại") } }) },
    ) { padding ->
        LazyColumn(Modifier.fillMaxSize().padding(padding), contentPadding = PaddingValues(20.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
            item {
                RiseCard {
                    Text(session.day, color = RiseTextMuted)
                    Text(session.time, style = MaterialTheme.typography.displayMedium)
                    StatusChip(if (session.outcome == SessionOutcome.Completed) "Hoàn thành" else "Có sự cố", if (session.outcome == SessionOutcome.Completed) RiseVerified else RiseWarning)
                }
            }
            item { SectionHeader("Dòng thời gian") }
            items(listOf("06:30:00  Alarm bắt đầu reo", "06:30:18  Quét QR phòng tắm", "06:31:05  Squat · 15/15", "06:31:34  Protocol được xác minh")) { label ->
                RiseCard { Text(label) }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AchievementDetailScreen(onAction: (HomeAction) -> Unit) {
    Scaffold(
        containerColor = RiseBackground,
        topBar = { CenterAlignedTopAppBar(title = { Text("Thành tựu") }, navigationIcon = { IconButton({ onAction(HomeAction.Back) }) { Icon(Icons.Rounded.ArrowBack, "Quay lại") } }) },
    ) { padding ->
        Column(Modifier.fillMaxSize().padding(padding).padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Spacer(Modifier.height(36.dp))
            Box(Modifier.size(160.dp).clip(CircleShape).background(RisePrimary.copy(alpha = 0.15f)), contentAlignment = Alignment.Center) {
                Icon(Icons.Rounded.LocalFireDepartment, null, tint = RisePrimary, modifier = Modifier.size(82.dp))
            }
            Spacer(Modifier.height(24.dp))
            Text("Chuỗi 7 ngày", style = MaterialTheme.typography.headlineLarge)
            Text("Đã mở khóa · 24/08/2026", color = RiseVerified)
            Spacer(Modifier.height(24.dp))
            RiseCard {
                Text("Thức dậy thành công trong 7 wake day liên tiếp mà không dùng Emergency Escape.", textAlign = TextAlign.Center)
                Spacer(Modifier.height(16.dp))
                Text("Kỷ lục hiện tại: 12 ngày", color = RisePrimary, style = MaterialTheme.typography.titleMedium)
            }
        }
    }
}

fun sampleSessions() = listOf(
    WakeSessionUi("s1", "Hôm nay", "06:28", "1p 34s", "QR · Squat · Toán", SessionOutcome.Completed),
    WakeSessionUi("s2", "Hôm qua", "06:31", "2p 08s", "QR · Plank", SessionOutcome.Completed),
    WakeSessionUi("s3", "Thứ Tư", "06:36", "4p 10s", "QR · Squat", SessionOutcome.Escaped),
    WakeSessionUi("s4", "Thứ Ba", "06:29", "1p 46s", "QR · Push-up", SessionOutcome.Completed),
    WakeSessionUi("s5", "Thứ Hai", "06:30", "—", "Camera không khả dụng", SessionOutcome.TechnicalError),
    WakeSessionUi("s6", "Chủ Nhật", "07:12", "1p 20s", "QR · Toán", SessionOutcome.Completed),
)

