package com.risealarm

import android.Manifest
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.risealarm.core.designsystem.RiseMainScaffold
import com.risealarm.core.designsystem.RiseTheme
import com.risealarm.core.designsystem.RiseTopLevel
import com.risealarm.domain.AlarmDefinition
import com.risealarm.domain.ExerciseType
import com.risealarm.feature.alarms.AlarmAction
import com.risealarm.feature.alarms.AlarmEditorScreen
import com.risealarm.feature.alarms.AlarmItemUi
import com.risealarm.feature.alarms.AlarmListScreen
import com.risealarm.feature.alarms.AlarmListUiState
import com.risealarm.feature.alarms.displayName
import com.risealarm.feature.home.HomeAction
import com.risealarm.feature.home.HomeDashboardScreen
import com.risealarm.feature.home.HomeUiState
import com.risealarm.feature.settings.SettingsAction
import com.risealarm.feature.settings.SettingsScreen
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter
import java.util.Locale

class MainActivity : ComponentActivity() {
    private val alarmViewModel by viewModels<AlarmViewModel>()
    private var selectedTab by mutableStateOf(RiseTopLevel.Home)
    private var deepLinkedAlarmId by mutableStateOf<String?>(null)
    private var capabilities by mutableStateOf(AlarmCapabilities(false, false, false))
    private var pendingCalibrationExercise: ExerciseType? = null
    private val calibrationLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == RESULT_OK) {
            alarmViewModel.markCalibrated()
            requestActivityRecognitionIfNeeded()
        }
    }
    private val activityRecognitionPermission = registerForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        if (!granted) Toast.makeText(this, "Nếu camera lỗi, RISE sẽ dùng lắc máy hoặc toán thay cho bước chân.", Toast.LENGTH_LONG).show()
    }
    private val cameraPermission = registerForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        val exercise = pendingCalibrationExercise
        pendingCalibrationExercise = null
        if (granted && exercise != null) {
            calibrationLauncher.launch(CalibrationActivity.intent(this, exercise))
        } else if (!granted) {
            Toast.makeText(this, "Cần quyền camera để hiệu chuẩn bài tập.", Toast.LENGTH_LONG).show()
        }
    }
    private val notificationPermission = registerForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        capabilities = readAlarmCapabilities()
        if (granted) {
            ensureAlarmCapabilities()
        } else {
            Toast.makeText(this, "Báo thức vẫn được lưu, nhưng cần quyền thông báo để hiển thị trên màn hình khóa.", Toast.LENGTH_LONG).show()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        handleIntent(intent)
        capabilities = readAlarmCapabilities()
        enableEdgeToEdge()
        setContent {
            RiseTheme {
                val alarms by alarmViewModel.alarms.collectAsStateWithLifecycle()
                val editor by alarmViewModel.editor.collectAsStateWithLifecycle()

                LaunchedEffect(deepLinkedAlarmId) {
                    deepLinkedAlarmId?.let {
                        alarmViewModel.openEdit(it)
                        selectedTab = RiseTopLevel.Alarms
                        deepLinkedAlarmId = null
                    }
                }

                if (editor != null) {
                    AlarmEditorScreen(editor!!, onAction = { action -> handleEditorAction(action, alarmViewModel) })
                } else {
                    val next = alarmViewModel.nextAlarm()
                    RiseMainScaffold(selected = selectedTab, onSelected = { selectedTab = it }) { padding ->
                        when (selectedTab) {
                            RiseTopLevel.Home -> HomeDashboardScreen(
                                state = next.toHomeState(capabilities),
                                onAction = { action ->
                                    if (action == HomeAction.OpenNextAlarm) {
                                        if (next == null) alarmViewModel.openNew() else alarmViewModel.openEdit(next.first.id)
                                    }
                                },
                                modifier = Modifier.padding(padding),
                            )
                            RiseTopLevel.Alarms -> AlarmListScreen(
                                state = alarms.toAlarmListState(capabilities),
                                onAction = { action -> handleListAction(action, alarmViewModel) },
                                modifier = Modifier.padding(padding),
                            )
                            RiseTopLevel.Settings -> SettingsScreen(
                                onAction = { action -> handleSettingsAction(action) },
                                modifier = Modifier.padding(padding),
                            )
                        }
                    }
                }
            }
        }
    }

    override fun onResume() {
        super.onResume()
        capabilities = readAlarmCapabilities()
        alarmViewModel.refresh()
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        handleIntent(intent)
    }

    private fun handleIntent(intent: Intent?) {
        if (intent?.data?.scheme == "rise") {
            deepLinkedAlarmId = intent.data?.pathSegments?.firstOrNull()
            selectedTab = RiseTopLevel.Alarms
        }
    }

    private fun handleListAction(action: AlarmAction, viewModel: AlarmViewModel) {
        when (action) {
            AlarmAction.Add -> viewModel.openNew()
            is AlarmAction.Edit -> viewModel.openEdit(action.id)
            is AlarmAction.Toggle -> {
                viewModel.toggle(action.id, action.enabled)
                if (action.enabled) ensureAlarmCapabilities()
            }
            is AlarmAction.Delete -> viewModel.delete(action.id)
            AlarmAction.OpenDiagnostics -> openRelevantSystemSetting()
            AlarmAction.TestAlarm -> {
                viewModel.openNew()
                Toast.makeText(this, "Chọn thời gian sau hiện tại 1–2 phút để test.", Toast.LENGTH_LONG).show()
            }
            else -> Unit
        }
    }

    private fun handleEditorAction(action: AlarmAction, viewModel: AlarmViewModel) {
        when (action) {
            AlarmAction.Back -> viewModel.closeEditor()
            is AlarmAction.SetTime -> viewModel.setTime(action.hour, action.minute)
            is AlarmAction.SetLabel -> viewModel.setLabel(action.value)
            is AlarmAction.ToggleDay -> viewModel.toggleDay(action.isoDay)
            is AlarmAction.SetVibration -> viewModel.setVibration(action.enabled)
            is AlarmAction.SelectExercise -> viewModel.selectExercise(action.exercise)
            is AlarmAction.ChangeExerciseTarget -> viewModel.changeExerciseTarget(action.delta)
            AlarmAction.CalibrateExercise -> startCalibration(viewModel)
            AlarmAction.Save -> if (viewModel.save()) {
                ensureAlarmCapabilities()
                Toast.makeText(this, "Đã lưu báo thức", Toast.LENGTH_SHORT).show()
            }
            is AlarmAction.Delete -> viewModel.delete(action.id)
            else -> Unit
        }
    }

    private fun startCalibration(viewModel: AlarmViewModel) {
        val exercise = viewModel.editor.value?.exercise ?: return
        if (androidx.core.content.ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) == android.content.pm.PackageManager.PERMISSION_GRANTED) {
            calibrationLauncher.launch(CalibrationActivity.intent(this, exercise))
        } else {
            pendingCalibrationExercise = exercise
            cameraPermission.launch(Manifest.permission.CAMERA)
        }
    }

    private fun requestActivityRecognitionIfNeeded() {
        if (Build.VERSION.SDK_INT >= 29 && androidx.core.content.ContextCompat.checkSelfPermission(this, Manifest.permission.ACTIVITY_RECOGNITION) != android.content.pm.PackageManager.PERMISSION_GRANTED) {
            activityRecognitionPermission.launch(Manifest.permission.ACTIVITY_RECOGNITION)
        }
    }

    private fun ensureAlarmCapabilities() {
        capabilities = readAlarmCapabilities()
        when {
            !capabilities.notifications && Build.VERSION.SDK_INT >= 33 -> notificationPermission.launch(Manifest.permission.POST_NOTIFICATIONS)
            !capabilities.exactAlarm && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> startActivity(Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM, Uri.parse("package:$packageName")))
            !capabilities.fullScreenIntent && Build.VERSION.SDK_INT >= 34 -> startActivity(Intent(Settings.ACTION_MANAGE_APP_USE_FULL_SCREEN_INTENT, Uri.parse("package:$packageName")))
        }
    }

    private fun openRelevantSystemSetting() {
        capabilities = readAlarmCapabilities()
        when {
            !capabilities.exactAlarm && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> startActivity(Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM, Uri.parse("package:$packageName")))
            !capabilities.fullScreenIntent && Build.VERSION.SDK_INT >= 34 -> startActivity(Intent(Settings.ACTION_MANAGE_APP_USE_FULL_SCREEN_INTENT, Uri.parse("package:$packageName")))
            else -> startActivity(Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS).putExtra(Settings.EXTRA_APP_PACKAGE, packageName))
        }
    }

    private fun handleSettingsAction(action: SettingsAction) {
        if ((action is SettingsAction.Open && action.destination == "permissions") || action == SettingsAction.OpenSystemSettings) {
            openRelevantSystemSetting()
        }
    }
}

private fun List<AlarmDefinition>.toAlarmListState(capabilities: AlarmCapabilities): AlarmListUiState {
    val enabled = filter { it.enabled }
    val nextTime = enabled.minWithOrNull(compareBy(AlarmDefinition::hour, AlarmDefinition::minute))
        ?.let { "%02d:%02d".format(it.hour, it.minute) }
    return AlarmListUiState(
        alarms = map { alarm ->
            AlarmItemUi(
                id = alarm.id,
                time = "%02d:%02d".format(alarm.hour, alarm.minute),
                label = alarm.label,
                days = alarm.repeatDays.toDayText(),
                protocol = alarm.challenge?.let { challenge ->
                    val unit = if (challenge.isHold) "${challenge.target} giây" else "${challenge.target} rep"
                    "${challenge.exercise.displayName()} · $unit"
                } ?: "Giữ 3 giây · alarm cũ",
                enabled = alarm.enabled,
                ready = !alarm.enabled || capabilities.ready,
            )
        },
        showEmpty = isEmpty(),
        readinessReady = enabled.isEmpty() || capabilities.ready,
        readinessTitle = when {
            enabled.isEmpty() -> "Chưa có báo thức đang bật"
            capabilities.ready -> "Sẵn sàng cho $nextTime"
            else -> "Cần cấp quyền để báo thức ổn định"
        },
        readinessDetail = if (capabilities.ready) "Exact alarm, thông báo và toàn màn hình đã sẵn sàng" else "Chạm biểu tượng khiên để mở cài đặt cần thiết",
    )
}

private fun Pair<AlarmDefinition, ZonedDateTime>?.toHomeState(capabilities: AlarmCapabilities): HomeUiState {
    if (this == null) return HomeUiState(nextAlarm = null, ready = false, showInsights = false, sessions = emptyList())
    val (alarm, occurrence) = this
    return HomeUiState(
        nextAlarm = "%02d:%02d".format(alarm.hour, alarm.minute),
        nextAlarmLabel = alarm.label,
        nextAlarmDay = occurrence.format(DateTimeFormatter.ofPattern("EEEE, dd/MM", Locale("vi", "VN"))),
        ready = capabilities.ready,
        showInsights = false,
        sessions = emptyList(),
    )
}

private fun Set<Int>.toDayText(): String {
    if (isEmpty()) return "Một lần"
    val names = listOf("T2", "T3", "T4", "T5", "T6", "T7", "CN")
    return sorted().joinToString(", ") { names[it - 1] }
}
