package com.risealarm

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import com.risealarm.domain.AlarmDefinition
import com.risealarm.engine.alarm.NextOccurrenceCalculator
import com.risealarm.feature.alarms.AlarmEditorUiState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.time.ZonedDateTime
import java.util.UUID

class AlarmViewModel(application: Application) : AndroidViewModel(application) {
    private val app = application as RiseApplication
    private val calculator = NextOccurrenceCalculator()
    private val _alarms = MutableStateFlow(app.alarmStore.all())
    val alarms = _alarms.asStateFlow()
    private val _editor = MutableStateFlow<AlarmEditorUiState?>(null)
    val editor = _editor.asStateFlow()

    fun refresh() {
        _alarms.value = app.alarmStore.all()
        app.alarmScheduler.reconcile(_alarms.value)
    }

    fun openNew() {
        val now = ZonedDateTime.now().plusMinutes(2)
        _editor.value = AlarmEditorUiState(
            hour = now.hour,
            minute = now.minute,
            label = "Thức dậy",
            selectedDays = emptySet(),
        )
    }

    fun openEdit(id: String) {
        val alarm = app.alarmStore.find(id) ?: return
        _editor.value = alarm.toEditorState()
    }

    fun closeEditor() {
        _editor.value = null
    }

    fun setTime(hour: Int, minute: Int) = updateEditor { copy(hour = hour, minute = minute, error = null) }
    fun setLabel(value: String) = updateEditor { copy(label = value.take(60), error = null) }
    fun setVibration(enabled: Boolean) = updateEditor { copy(vibration = enabled) }
    fun toggleDay(day: Int) = updateEditor {
        copy(selectedDays = if (day in selectedDays) selectedDays - day else selectedDays + day)
    }

    fun save(): Boolean {
        val state = _editor.value ?: return false
        if (state.label.isBlank()) {
            _editor.value = state.copy(error = "Vui lòng nhập nhãn báo thức.")
            return false
        }
        val alarm = AlarmDefinition(
            id = state.id ?: UUID.randomUUID().toString(),
            hour = state.hour,
            minute = state.minute,
            label = state.label.trim(),
            repeatDays = state.selectedDays,
            vibration = state.vibration,
            enabled = state.enabled,
        )
        if (!app.alarmStore.upsert(alarm)) {
            _editor.value = state.copy(error = "Không thể lưu báo thức trên thiết bị.")
            return false
        }
        if (alarm.enabled) app.alarmScheduler.schedule(alarm) else app.alarmScheduler.cancel(alarm.id)
        _alarms.value = app.alarmStore.all()
        _editor.value = null
        return true
    }

    fun toggle(id: String, enabled: Boolean) {
        val alarm = app.alarmStore.setEnabled(id, enabled) ?: return
        if (enabled) app.alarmScheduler.schedule(alarm) else app.alarmScheduler.cancel(id)
        _alarms.value = app.alarmStore.all()
    }

    fun delete(id: String) {
        app.alarmScheduler.cancel(id)
        app.alarmStore.delete(id)
        _alarms.value = app.alarmStore.all()
        if (_editor.value?.id == id) _editor.value = null
    }

    fun nextAlarm(now: ZonedDateTime = ZonedDateTime.now()): Pair<AlarmDefinition, ZonedDateTime>? =
        _alarms.value.mapNotNull { alarm -> calculator.next(alarm, now)?.let { alarm to it } }
            .minByOrNull { it.second.toInstant() }

    private fun updateEditor(transform: AlarmEditorUiState.() -> AlarmEditorUiState) {
        _editor.value = _editor.value?.transform()
    }

    private fun AlarmDefinition.toEditorState() = AlarmEditorUiState(
        id = id,
        hour = hour,
        minute = minute,
        label = label,
        selectedDays = repeatDays,
        vibration = vibration,
        enabled = enabled,
    )
}
