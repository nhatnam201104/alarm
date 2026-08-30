package com.risealarm.catalog

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import com.risealarm.core.designsystem.*
import com.risealarm.feature.alarms.*
import com.risealarm.feature.home.*
import com.risealarm.feature.onboarding.*
import com.risealarm.feature.protocol.*
import com.risealarm.feature.settings.*
import com.risealarm.feature.settings.SettingsScreen as RiseSettingsScreen
import com.risealarm.feature.wake.*

class CatalogActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            RiseTheme {
                Surface(Modifier.fillMaxSize(), color = RiseBackground) {
                    CatalogApp()
                }
            }
        }
    }
}

private enum class CatalogScreen(val title: String, val group: String) {
    Index("UI Catalog", "Catalog"),
    AppDemo("Ứng dụng 3 tab", "Catalog"),
    Welcome("Welcome & Promise", "Onboarding"),
    Safety("Safety & Mobility", "Onboarding"),
    Permissions("Permission Wizard", "Onboarding"),
    Reliability("Reliability Test", "Onboarding"),
    ExerciseSetup("Exercise Setup", "Onboarding"),
    QrSetup("QR Location Setup", "Onboarding"),
    Home("Home Dashboard", "Main"),
    AlarmList("Alarm List", "Main"),
    Settings("Settings", "Main"),
    AlarmEditor("Add/Edit Alarm", "Alarm"),
    ProtocolBuilder("Wake Protocol Builder", "Alarm"),
    MissionPicker("Mission Picker", "Alarm"),
    MissionConfig("Mission Configuration", "Alarm"),
    AlarmReview("Alarm Review", "Alarm"),
    Diagnostics("Readiness Diagnostics", "Alarm"),
    Ringing("Alarm Ringing", "Wake"),
    VisionPushup("Active Push-up", "Wake"),
    VisionSitup("Active Sit-up", "Wake"),
    HoldSquat("Squat Hold", "Wake"),
    VisionPlank("Plank Hold", "Wake"),
    SensorQr("QR Mission", "Wake"),
    SensorSteps("Steps Mission", "Wake"),
    SensorShake("Shake Mission", "Wake"),
    SensorMath("Math Mission", "Wake"),
    Result("Wake Result", "Wake"),
    Privacy("Privacy & Local Data", "Settings"),
    History("Full History", "P1"),
    SessionDetail("Session Detail", "P1"),
    Achievement("Achievement Detail", "P1"),
    ExerciseLibrary("Exercise Library", "P1"),
    OemHelp("OEM Troubleshooting", "P1"),
    EmptyAlarms("Alarm Empty State", "States"),
    LoadingAlarms("Alarm Loading State", "States"),
    VisionFraming("Vision Framing", "States"),
    TrackingLost("Tracking Lost", "States"),
    CameraBusy("Camera Busy", "States"),
    Thermal("Thermal Fallback", "States"),
    UnlockGate("Direct Boot Unlock", "States"),
}

@Composable
private fun CatalogApp() {
    var currentName by rememberSaveable { mutableStateOf(CatalogScreen.Index.name) }
    var lastName by rememberSaveable { mutableStateOf(CatalogScreen.Index.name) }
    var mainTabName by rememberSaveable { mutableStateOf(RiseTopLevel.Home.name) }
    var reliabilityPassed by rememberSaveable { mutableStateOf(false) }
    var qrPassed by rememberSaveable { mutableStateOf(false) }
    var selectedExercise by rememberSaveable { mutableStateOf("Plank") }
    var selectedCategoryName by rememberSaveable { mutableStateOf(MissionCategory.Move.name) }
    var analyticsEnabled by rememberSaveable { mutableStateOf(false) }

    val current = CatalogScreen.valueOf(currentName)
    fun navigate(target: CatalogScreen) {
        lastName = currentName
        currentName = target.name
    }
    fun back() {
        currentName = if (current == CatalogScreen.Index) CatalogScreen.Index.name else lastName
        if (currentName == current.name) currentName = CatalogScreen.Index.name
    }

    BackHandler(enabled = current != CatalogScreen.Index) { navigate(CatalogScreen.Index) }

    when (current) {
        CatalogScreen.Index -> CatalogIndex(onOpen = ::navigate)
        CatalogScreen.AppDemo -> MainDemo(RiseTopLevel.valueOf(mainTabName), { mainTabName = it.name }, ::navigate)
        CatalogScreen.Welcome -> WelcomeScreen(onAction = { if (it == OnboardingAction.Continue) navigate(CatalogScreen.Safety) })
        CatalogScreen.Safety -> SafetyProfileScreen(SafetyUiState(accepted = true)) { action ->
            when (action) { OnboardingAction.Continue -> navigate(CatalogScreen.Permissions); OnboardingAction.Back -> navigate(CatalogScreen.Welcome); else -> Unit }
        }
        CatalogScreen.Permissions -> PermissionWizardScreen(defaultPermissions()) { action ->
            when (action) { OnboardingAction.Continue -> navigate(CatalogScreen.ProtocolBuilder); OnboardingAction.OpenSettings -> navigate(CatalogScreen.Diagnostics); OnboardingAction.Back -> navigate(CatalogScreen.Safety); else -> Unit }
        }
        CatalogScreen.Reliability -> ReliabilityTestScreen(false, reliabilityPassed) { action ->
            when (action) { OnboardingAction.RunTest -> reliabilityPassed = true; OnboardingAction.Continue -> navigate(CatalogScreen.Home); OnboardingAction.Back -> navigate(CatalogScreen.QrSetup); else -> Unit }
        }
        CatalogScreen.ExerciseSetup -> ExerciseSetupScreen(selectedExercise) { action ->
            when (action) { is OnboardingAction.Select -> selectedExercise = action.key; OnboardingAction.Continue -> navigate(CatalogScreen.Reliability); OnboardingAction.Back -> navigate(CatalogScreen.MissionPicker); else -> Unit }
        }
        CatalogScreen.QrSetup -> QrSetupScreen(qrPassed) { action ->
            when (action) { OnboardingAction.RunTest -> qrPassed = true; OnboardingAction.Continue -> navigate(CatalogScreen.Reliability); OnboardingAction.Back -> navigate(CatalogScreen.ProtocolBuilder); else -> Unit }
        }
        CatalogScreen.Home -> NormalPreviewBar(RiseTopLevel.Home, ::navigate) { HomeDashboardScreen(HomeUiState(), homeActions(::navigate)) }
        CatalogScreen.AlarmList -> NormalPreviewBar(RiseTopLevel.Alarms, ::navigate) { AlarmListScreen(AlarmListUiState(), alarmActions(::navigate)) }
        CatalogScreen.EmptyAlarms -> AlarmListScreen(AlarmListUiState(alarms = emptyList(), showEmpty = true), alarmActions(::navigate))
        CatalogScreen.LoadingAlarms -> AlarmListScreen(AlarmListUiState(isLoading = true), alarmActions(::navigate))
        CatalogScreen.Settings -> NormalPreviewBar(RiseTopLevel.Settings, ::navigate) { RiseSettingsScreen(settingsActions(::navigate)) }
        CatalogScreen.AlarmEditor -> AlarmEditorScreen(AlarmEditorUiState(), alarmActions(::navigate))
        CatalogScreen.ProtocolBuilder -> ProtocolBuilderScreen(sampleMissionSteps(), protocolActions(::navigate) { selectedCategoryName = it.name })
        CatalogScreen.MissionPicker -> MissionPickerScreen(MissionCategory.valueOf(selectedCategoryName), sampleMissionChoices(), protocolActions(::navigate) { selectedCategoryName = it.name })
        CatalogScreen.MissionConfig -> MissionConfigurationScreen(MissionConfigUiState(exercise = selectedExercise), protocolActions(::navigate) { selectedCategoryName = it.name })
        CatalogScreen.AlarmReview -> AlarmReviewScreen(sampleReadinessChecks(), alarmActions(::navigate))
        CatalogScreen.Diagnostics -> ReadinessDiagnosticsScreen(sampleReadinessChecks(), alarmActions(::navigate))
        CatalogScreen.Ringing -> AlarmRingingScreen(wakeActions(::navigate))
        CatalogScreen.VisionPushup -> ActiveVisionScreen(ActiveVisionUiState(), wakeActions(::navigate), cameraContent = { MockCameraContent() })
        CatalogScreen.VisionSitup -> ActiveVisionScreen(ActiveVisionUiState(exercise = "Gập bụng", count = 5, target = 10, instruction = "Chạm khuỷu tay vào gần đầu gối"), wakeActions(::navigate), cameraContent = { MockCameraContent() })
        CatalogScreen.HoldSquat -> PlankHoldScreen(HoldUiState(exercise = "Giữ squat", elapsed = 18, target = 30), wakeActions(::navigate), cameraContent = { MockCameraContent() })
        CatalogScreen.VisionPlank -> PlankHoldScreen(HoldUiState(), wakeActions(::navigate), cameraContent = { MockCameraContent() })
        CatalogScreen.SensorQr -> ActiveSensorMissionScreen(SensorMissionType.Qr, wakeActions(::navigate))
        CatalogScreen.SensorSteps -> ActiveSensorMissionScreen(SensorMissionType.Steps, wakeActions(::navigate))
        CatalogScreen.SensorShake -> ActiveSensorMissionScreen(SensorMissionType.Shake, wakeActions(::navigate))
        CatalogScreen.SensorMath -> ActiveSensorMissionScreen(SensorMissionType.Math, wakeActions(::navigate))
        CatalogScreen.Result -> WakeResultScreen(wakeActions(::navigate))
        CatalogScreen.Privacy -> PrivacyScreen(analyticsEnabled) { action ->
            when (action) { is SettingsAction.Toggle -> analyticsEnabled = action.enabled; SettingsAction.Back -> navigate(CatalogScreen.Settings); else -> Unit }
        }
        CatalogScreen.History -> FullHistoryScreen(sampleSessions(), homeActions(::navigate))
        CatalogScreen.SessionDetail -> SessionDetailScreen(sampleSessions().first(), homeActions(::navigate))
        CatalogScreen.Achievement -> AchievementDetailScreen(homeActions(::navigate))
        CatalogScreen.ExerciseLibrary -> ExerciseLibraryScreen(settingsActions(::navigate))
        CatalogScreen.OemHelp -> OemHelpScreen(settingsActions(::navigate))
        CatalogScreen.VisionFraming -> ActiveVisionScreen(ActiveVisionUiState(status = VisionStatus.Framing, instruction = "Lùi điện thoại để thấy toàn thân"), wakeActions(::navigate), cameraContent = { MockCameraContent() })
        CatalogScreen.TrackingLost -> ActiveVisionScreen(ActiveVisionUiState(status = VisionStatus.TrackingLost), wakeActions(::navigate), cameraContent = { MockCameraContent() })
        CatalogScreen.CameraBusy -> ActiveVisionScreen(ActiveVisionUiState(status = VisionStatus.CameraBusy), wakeActions(::navigate), cameraContent = { MockCameraContent() })
        CatalogScreen.Thermal -> ActiveVisionScreen(ActiveVisionUiState(status = VisionStatus.Thermal), wakeActions(::navigate), cameraContent = { MockCameraContent() })
        CatalogScreen.UnlockGate -> ActiveVisionScreen(ActiveVisionUiState(status = VisionStatus.UnlockRequired), wakeActions(::navigate), cameraContent = { MockCameraContent() })
    }
}

@Composable
private fun MainDemo(selected: RiseTopLevel, onSelected: (RiseTopLevel) -> Unit, navigate: (CatalogScreen) -> Unit) {
    RiseMainScaffold(selected, onSelected) { padding ->
        Box(Modifier.padding(padding)) {
            when (selected) {
                RiseTopLevel.Home -> HomeDashboardScreen(HomeUiState(), homeActions(navigate))
                RiseTopLevel.Alarms -> AlarmListScreen(AlarmListUiState(), alarmActions(navigate))
                RiseTopLevel.Settings -> RiseSettingsScreen(settingsActions(navigate))
            }
        }
    }
}

@Composable
private fun NormalPreviewBar(selected: RiseTopLevel, navigate: (CatalogScreen) -> Unit, content: @Composable () -> Unit) {
    RiseMainScaffold(selected, {
        navigate(when (it) { RiseTopLevel.Home -> CatalogScreen.Home; RiseTopLevel.Alarms -> CatalogScreen.AlarmList; RiseTopLevel.Settings -> CatalogScreen.Settings })
    }) { padding -> Box(Modifier.padding(padding)) { content() } }
}

@Composable
private fun CatalogIndex(onOpen: (CatalogScreen) -> Unit) {
    LazyColumn(
        Modifier.fillMaxSize().background(RiseBackground),
        contentPadding = PaddingValues(20.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        item {
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconBadge(Icons.Rounded.Palette, RisePrimary, size = 58.dp)
                Column(Modifier.padding(start = 14.dp)) {
                    Text("RISE UI Catalog", style = MaterialTheme.typography.headlineLarge)
                    Text("UI-only · ${CatalogScreen.entries.size - 1} screens & states", color = RiseTextMuted)
                }
            }
            Spacer(Modifier.height(14.dp))
            RiseCard(containerColor = RiseSecondary.copy(alpha = .10f)) {
                Text("Midnight Performance", color = RiseSecondary, style = MaterialTheme.typography.titleMedium)
                Text("Chạm một mục để mở toàn màn hình. Dùng nút Back của Android để quay lại catalog.", color = RiseTextMuted)
            }
        }
        CatalogScreen.entries.filter { it != CatalogScreen.Index }.groupBy { it.group }.forEach { (group, screens) ->
            item { Text(group, color = RiseTextMuted, style = MaterialTheme.typography.labelLarge, modifier = Modifier.padding(top = 14.dp)) }
            items(screens, key = { it.name }) { screen ->
                RiseCard(Modifier.fillMaxWidth().clickable { onOpen(screen) }, RiseSurface) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        IconBadge(groupIcon(group), groupColor(group), size = 40.dp)
                        Text(screen.title, modifier = Modifier.weight(1f).padding(horizontal = 12.dp), style = MaterialTheme.typography.titleMedium)
                        Icon(Icons.Rounded.ChevronRight, "Mở", tint = RiseTextMuted)
                    }
                }
            }
        }
        item { Spacer(Modifier.height(28.dp)) }
    }
}

private fun groupIcon(group: String): ImageVector = when (group) {
    "Onboarding" -> Icons.Rounded.RocketLaunch
    "Main" -> Icons.Rounded.Dashboard
    "Alarm" -> Icons.Rounded.Alarm
    "Wake" -> Icons.Rounded.Bolt
    "Settings" -> Icons.Rounded.Settings
    "P1" -> Icons.Rounded.AutoAwesome
    "States" -> Icons.Rounded.Layers
    else -> Icons.Rounded.Apps
}

private fun groupColor(group: String): Color = when (group) {
    "Wake" -> RisePrimary
    "Onboarding" -> RiseSecondary
    "States" -> RiseWarning
    "P1" -> RiseVerified
    else -> RiseTextMuted
}

private fun homeActions(navigate: (CatalogScreen) -> Unit): (HomeAction) -> Unit = { action ->
    when (action) {
        HomeAction.OpenNextAlarm -> navigate(CatalogScreen.AlarmEditor)
        HomeAction.OpenHistory -> navigate(CatalogScreen.History)
        is HomeAction.OpenSession -> navigate(CatalogScreen.SessionDetail)
        is HomeAction.OpenAchievement -> navigate(CatalogScreen.Achievement)
        HomeAction.Back -> navigate(CatalogScreen.Index)
    }
}

private fun alarmActions(navigate: (CatalogScreen) -> Unit): (AlarmAction) -> Unit = { action ->
    when (action) {
        AlarmAction.Add -> navigate(CatalogScreen.AlarmEditor)
        is AlarmAction.Edit -> navigate(CatalogScreen.AlarmEditor)
        AlarmAction.Continue -> navigate(CatalogScreen.ProtocolBuilder)
        AlarmAction.OpenProtocol -> navigate(CatalogScreen.ProtocolBuilder)
        AlarmAction.OpenDiagnostics -> navigate(CatalogScreen.Diagnostics)
        AlarmAction.TestAlarm -> navigate(CatalogScreen.Reliability)
        AlarmAction.Save -> navigate(CatalogScreen.AlarmList)
        AlarmAction.Back -> navigate(CatalogScreen.Index)
        else -> Unit
    }
}

private fun protocolActions(navigate: (CatalogScreen) -> Unit, selectCategory: (MissionCategory) -> Unit): (ProtocolAction) -> Unit = { action ->
    when (action) {
        ProtocolAction.AddMission -> navigate(CatalogScreen.MissionPicker)
        is ProtocolAction.EditMission -> navigate(CatalogScreen.MissionConfig)
        is ProtocolAction.SelectMission -> {
            if (action.type.startsWith("category:")) {
                selectCategory(MissionCategory.valueOf(action.type.substringAfter("category:")))
            } else {
                navigate(CatalogScreen.MissionConfig)
            }
        }
        ProtocolAction.SaveMission -> navigate(CatalogScreen.ProtocolBuilder)
        ProtocolAction.Review -> navigate(CatalogScreen.AlarmReview)
        ProtocolAction.Back -> navigate(CatalogScreen.Index)
        else -> Unit
    }
}

private fun wakeActions(navigate: (CatalogScreen) -> Unit): (WakeAction) -> Unit = { action ->
    when (action) {
        WakeAction.Start -> navigate(CatalogScreen.VisionPushup)
        WakeAction.Help -> navigate(CatalogScreen.TrackingLost)
        WakeAction.Retry -> navigate(CatalogScreen.VisionPushup)
        WakeAction.UseFallback -> navigate(CatalogScreen.SensorQr)
        WakeAction.Finish -> navigate(CatalogScreen.AppDemo)
        WakeAction.Back -> navigate(CatalogScreen.Ringing)
        is WakeAction.SubmitAnswer -> navigate(CatalogScreen.Result)
    }
}

private fun settingsActions(navigate: (CatalogScreen) -> Unit): (SettingsAction) -> Unit = { action ->
    when (action) {
        is SettingsAction.Open -> when {
            action.destination == "privacy" -> navigate(CatalogScreen.Privacy)
            action.destination == "exercise" -> navigate(CatalogScreen.ExerciseLibrary)
            action.destination == "oem" -> navigate(CatalogScreen.OemHelp)
            action.destination == "permissions" -> navigate(CatalogScreen.Diagnostics)
            else -> Unit
        }
        SettingsAction.Back -> navigate(CatalogScreen.Index)
        else -> Unit
    }
}
