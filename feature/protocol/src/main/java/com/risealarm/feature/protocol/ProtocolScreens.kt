package com.risealarm.feature.protocol

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
import androidx.compose.ui.unit.dp
import com.risealarm.core.designsystem.*
import kotlinx.serialization.Serializable

@Serializable data object ProtocolBuilderRoute
@Serializable data object MissionPickerRoute
@Serializable data class MissionConfigRoute(val missionType: String)

sealed interface ProtocolAction {
    data object Back : ProtocolAction
    data object AddMission : ProtocolAction
    data class EditMission(val id: String) : ProtocolAction
    data class RemoveMission(val id: String) : ProtocolAction
    data class SelectMission(val type: String) : ProtocolAction
    data object SaveMission : ProtocolAction
    data object Review : ProtocolAction
    data class ChangeValue(val key: String, val delta: Int) : ProtocolAction
}

@Immutable
data class MissionStepUi(val id: String, val title: String, val detail: String, val estimate: String, val icon: ImageVector, val color: Color)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProtocolBuilderScreen(missions: List<MissionStepUi>, onAction: (ProtocolAction) -> Unit) {
    Scaffold(
        containerColor = RiseBackground,
        topBar = { CenterAlignedTopAppBar(title = { Text("Nhiệm vụ đánh thức") }, navigationIcon = { IconButton({ onAction(ProtocolAction.Back) }) { Icon(Icons.Rounded.ArrowBack, "Quay lại") } }) },
        bottomBar = { Box(Modifier.padding(20.dp)) { RisePrimaryButton("Kiểm tra & kích hoạt", { onAction(ProtocolAction.Review) }, leadingIcon = Icons.Rounded.Verified) } },
    ) { padding ->
        LazyColumn(Modifier.fillMaxSize().padding(padding), contentPadding = PaddingValues(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            item {
                RiseCard(containerColor = RiseSecondary.copy(alpha = 0.10f)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        IconBadge(Icons.Rounded.Timer, RiseSecondary)
                        Column(Modifier.padding(start = 12.dp)) {
                            Text("Khoảng 1 phút 40 giây", style = MaterialTheme.typography.titleMedium)
                            Text("3 nhiệm vụ · độ khó cao", color = RiseTextMuted)
                        }
                    }
                }
            }
            items(missions, key = { it.id }) { mission ->
                MissionStepCard(mission, missions.indexOf(mission) + 1, onAction)
            }
            item {
                OutlinedButton(
                    onClick = { onAction(ProtocolAction.AddMission) },
                    modifier = Modifier.fillMaxWidth().height(56.dp),
                    shape = MaterialTheme.shapes.medium,
                ) { Icon(Icons.Rounded.Add, null); Spacer(Modifier.width(8.dp)); Text("Thêm nhiệm vụ") }
            }
            item { Spacer(Modifier.height(90.dp)) }
        }
    }
}

@Composable
private fun MissionStepCard(step: MissionStepUi, index: Int, onAction: (ProtocolAction) -> Unit) {
    Row {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Box(Modifier.size(32.dp).clip(CircleShape).background(step.color), contentAlignment = Alignment.Center) {
                Text(index.toString(), color = Color.White, fontWeight = FontWeight.Bold)
            }
            Box(Modifier.width(2.dp).height(100.dp).background(RiseOutline))
        }
        RiseCard(Modifier.weight(1f).padding(start = 12.dp), RiseSurfaceHigh) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Rounded.DragIndicator, "Kéo để sắp xếp", tint = RiseTextMuted)
                IconBadge(step.icon, step.color, modifier = Modifier.padding(start = 8.dp))
                Column(Modifier.weight(1f).padding(horizontal = 12.dp)) {
                    Text(step.title, style = MaterialTheme.typography.titleMedium)
                    Text(step.detail + " · " + step.estimate, color = RiseTextMuted, style = MaterialTheme.typography.bodyMedium)
                }
                IconButton({ onAction(ProtocolAction.EditMission(step.id)) }) { Icon(Icons.Rounded.Edit, "Chỉnh") }
            }
        }
    }
}

enum class MissionCategory(val label: String) { Move("Di chuyển"), Hold("Giữ tư thế"), Explore("Khám phá"), Mind("Trí óc") }
data class MissionChoiceUi(val type: String, val title: String, val detail: String, val category: MissionCategory, val icon: ImageVector, val difficulty: Int, val calibrated: Boolean)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MissionPickerScreen(selectedCategory: MissionCategory, choices: List<MissionChoiceUi>, onAction: (ProtocolAction) -> Unit) {
    Scaffold(
        containerColor = RiseBackground,
        topBar = { CenterAlignedTopAppBar(title = { Text("Chọn nhiệm vụ") }, navigationIcon = { IconButton({ onAction(ProtocolAction.Back) }) { Icon(Icons.Rounded.ArrowBack, "Quay lại") } }) },
    ) { padding ->
        Column(Modifier.fillMaxSize().padding(padding)) {
            LazyColumn(contentPadding = PaddingValues(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                item {
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        MissionCategory.entries.forEach { category ->
                            FilterChip(
                                selected = category == selectedCategory,
                                onClick = { onAction(ProtocolAction.SelectMission("category:" + category.name)) },
                                label = { Text(category.label) },
                            )
                        }
                    }
                }
                items(choices.filter { it.category == selectedCategory }) { item ->
                    RiseCard(Modifier.clickable { onAction(ProtocolAction.SelectMission(item.type)) }, if (item.type == "plank") RiseSecondary.copy(alpha = 0.13f) else RiseSurface) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            IconBadge(item.icon, if (item.type == "plank") RiseSecondary else RisePrimary)
                            Column(Modifier.weight(1f).padding(horizontal = 12.dp)) {
                                Text(item.title, style = MaterialTheme.typography.titleMedium)
                                Text(item.detail, color = RiseTextMuted)
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    repeat(item.difficulty) { Icon(Icons.Rounded.Star, null, tint = RiseWarning, modifier = Modifier.size(14.dp)) }
                                    Spacer(Modifier.width(8.dp))
                                    Text(if (item.calibrated) "Đã hiệu chuẩn" else "Cần hiệu chuẩn", color = if (item.calibrated) RiseVerified else RiseWarning, style = MaterialTheme.typography.labelMedium)
                                }
                            }
                            Icon(Icons.Rounded.ChevronRight, "Chọn", tint = RiseTextMuted)
                        }
                    }
                }
            }
        }
    }
}

@Immutable
data class MissionConfigUiState(
    val exercise: String = "Plank",
    val target: Int = 20,
    val unit: String = "giây",
    val difficulty: String = "Khó",
    val fallback: String = "Quét QR phòng tắm",
    val calibrated: Boolean = true,
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MissionConfigurationScreen(state: MissionConfigUiState, onAction: (ProtocolAction) -> Unit) {
    Scaffold(
        containerColor = RiseBackground,
        topBar = { CenterAlignedTopAppBar(title = { Text("Cấu hình " + state.exercise) }, navigationIcon = { IconButton({ onAction(ProtocolAction.Back) }) { Icon(Icons.Rounded.ArrowBack, "Quay lại") } }) },
        bottomBar = { Box(Modifier.padding(20.dp)) { RisePrimaryButton("Lưu nhiệm vụ", { onAction(ProtocolAction.SaveMission) }) } },
    ) { padding ->
        LazyColumn(Modifier.fillMaxSize().padding(padding), contentPadding = PaddingValues(20.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
            item {
                RiseCard {
                    Text("Mục tiêu", color = RiseTextMuted)
                    Spacer(Modifier.height(16.dp))
                    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceAround) {
                        FilledIconButton({ onAction(ProtocolAction.ChangeValue("target", -5)) }) { Icon(Icons.Rounded.Remove, "Giảm") }
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(state.target.toString(), style = MaterialTheme.typography.displayMedium)
                            Text(state.unit, color = RiseTextMuted)
                        }
                        FilledIconButton({ onAction(ProtocolAction.ChangeValue("target", 5)) }) { Icon(Icons.Rounded.Add, "Tăng") }
                    }
                    Spacer(Modifier.height(16.dp))
                    Text("Cho phép 10–60 giây, theo bước 5 giây.", color = RiseTextMuted, style = MaterialTheme.typography.bodyMedium)
                }
            }
            item {
                RiseCard {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        IconBadge(Icons.Rounded.CenterFocusStrong, RiseVerified)
                        Column(Modifier.weight(1f).padding(start = 12.dp)) {
                            Text("Hiệu chuẩn camera", style = MaterialTheme.typography.titleMedium)
                            Text(if (state.calibrated) "Đạt · còn hiệu lực 24 ngày" else "Cần chạy lại", color = if (state.calibrated) RiseVerified else RiseWarning)
                        }
                        Icon(Icons.Rounded.ChevronRight, "Mở hiệu chuẩn")
                    }
                }
            }
            item {
                RiseCard {
                    Text("Fallback tương đương", color = RiseTextMuted)
                    Spacer(Modifier.height(8.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Rounded.QrCodeScanner, null, tint = RiseSecondary)
                        Text(state.fallback, modifier = Modifier.weight(1f).padding(start = 12.dp), style = MaterialTheme.typography.titleMedium)
                        Icon(Icons.Rounded.ChevronRight, "Đổi fallback")
                    }
                }
            }
            item {
                RiseCard(containerColor = RiseWarning.copy(alpha = 0.08f)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Rounded.HealthAndSafety, null, tint = RiseWarning)
                        Text("Dừng lại nếu đau cổ tay, vai hoặc chóng mặt.", color = RiseWarning, modifier = Modifier.padding(start = 12.dp))
                    }
                }
            }
        }
    }
}

fun sampleMissionSteps() = listOf(
    MissionStepUi("qr", "Quét QR trong phòng tắm", "Rời khỏi giường", "30 giây", Icons.Rounded.QrCodeScanner, RisePrimary),
    MissionStepUi("squat", "Squat · 15 lần", "Camera xác minh", "40 giây", Icons.Rounded.AccessibilityNew, RiseSecondary),
    MissionStepUi("math", "Toán nhanh · 3 câu", "Đánh thức trí óc", "30 giây", Icons.Rounded.Calculate, RiseVerified),
)

fun sampleMissionChoices() = listOf(
    MissionChoiceUi("pushup", "Push-up", "Đếm chu kỳ đầy đủ", MissionCategory.Move, Icons.Rounded.FitnessCenter, 3, true),
    MissionChoiceUi("squat", "Squat", "Camera thấy toàn thân", MissionCategory.Move, Icons.Rounded.AccessibilityNew, 2, true),
    MissionChoiceUi("situp", "Gập bụng", "Đặt máy ngang sàn", MissionCategory.Move, Icons.Rounded.SelfImprovement, 3, false),
    MissionChoiceUi("squat_hold", "Giữ squat", "10–60 giây", MissionCategory.Hold, Icons.Rounded.Timer, 3, true),
    MissionChoiceUi("plank", "Plank", "Giữ cẳng tay", MissionCategory.Hold, Icons.Rounded.SelfImprovement, 3, true),
    MissionChoiceUi("qr", "Quét QR", "Rời giường để quét", MissionCategory.Explore, Icons.Rounded.QrCodeScanner, 2, true),
    MissionChoiceUi("steps", "Bước chân", "Đi đủ số bước", MissionCategory.Explore, Icons.Rounded.DirectionsWalk, 1, true),
    MissionChoiceUi("shake", "Lắc máy", "Hoàn thành chuỗi lắc", MissionCategory.Explore, Icons.Rounded.Vibration, 1, true),
    MissionChoiceUi("math", "Toán", "Giải câu hỏi nhanh", MissionCategory.Mind, Icons.Rounded.Calculate, 2, true),
)

