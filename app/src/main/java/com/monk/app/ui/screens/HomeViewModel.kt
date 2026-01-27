package com.monk.app.ui.screens

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.monk.app.data.datastore.PreferencesManager
import com.monk.app.domain.model.CustomDuration
import com.monk.app.domain.model.FocusDuration
import com.monk.app.domain.model.FocusState
import com.monk.app.service.FocusService
import com.monk.app.service.NotificationListener
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import java.time.Duration
import java.time.Instant
import javax.inject.Inject

@HiltViewModel
class HomeViewModel @Inject constructor(
    @ApplicationContext private val context: Context
) : ViewModel() {

    private val _focusState = MutableStateFlow(FocusState())
    val focusState: StateFlow<FocusState> = _focusState.asStateFlow()

    private val _selectedDuration = MutableStateFlow(FocusDuration.MINUTES_30)
    val selectedDuration: StateFlow<FocusDuration> = _selectedDuration.asStateFlow()
    
    private val preferencesManager = PreferencesManager(context)

    private var tick: Long = 0

    init {
        // Load focus preferences on startup
        viewModelScope.launch {
            val deepFocusEnabled = preferencesManager.deepFocusEnabled.first()
            val dndEnabled = preferencesManager.dndEnabled.first()
            FocusService.deepFocusEnabled = deepFocusEnabled
            FocusService.dndEnabled = dndEnabled
        }
        
        // Poll for service state changes
        viewModelScope.launch {
            while (true) {
                delay(1000) // Update every second
                updateFocusState()
            }
        }
    }

    private fun updateFocusState() {
        val isActive = FocusService.isRunning
        
        // Use the service's persisted start time and duration
        val startTime = if (FocusService.focusStartTimeMs > 0) {
            Instant.ofEpochMilli(FocusService.focusStartTimeMs)
        } else {
            null
        }
        
        val scheduledEnd = if (isActive && FocusService.focusDurationMs > 0 && startTime != null) {
            startTime.plusMillis(FocusService.focusDurationMs)
        } else {
            null
        }
        
        // Increment tick to force UI recomposition
        tick++
        
        _focusState.value = FocusState(
            isActive = isActive,
            startedAt = startTime,
            scheduledEndAt = scheduledEnd,
            notificationsSilenced = NotificationListener.notificationsSilenced,
            repliesSent = NotificationListener.repliesSent,
            tick = tick
        )
        
        // Note: Auto-stop is now handled by FocusService itself
    }

    fun setDuration(duration: FocusDuration) {
        _selectedDuration.value = duration
    }

    fun startFocus() {
        val duration = if (_selectedDuration.value.isCustom) {
            CustomDuration.getDuration()
        } else {
            _selectedDuration.value.duration
        }
        
        val durationMs = duration?.toMillis() ?: 0
        
        // Start service with duration
        FocusService.start(context, durationMs)
        
        // Optimistically update UI
        tick = 0
        val startTime = Instant.now()
        val scheduledEnd = if (durationMs > 0) startTime.plusMillis(durationMs) else null
        
        _focusState.value = FocusState(
            isActive = true,
            startedAt = startTime,
            scheduledEndAt = scheduledEnd,
            notificationsSilenced = 0,
            repliesSent = 0,
            tick = tick
        )
    }

    fun stopFocus() {
        FocusService.stop(context)
        
        _focusState.value = FocusState(
            isActive = false,
            startedAt = null,
            scheduledEndAt = null,
            notificationsSilenced = _focusState.value.notificationsSilenced,
            repliesSent = _focusState.value.repliesSent,
            tick = 0
        )
    }
}
