package com.monk.app.ui.screens

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.monk.app.domain.model.FocusDuration
import com.monk.app.domain.model.FocusState
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.time.Instant
import javax.inject.Inject

@HiltViewModel
class HomeViewModel @Inject constructor(
    // TODO: Inject repositories
) : ViewModel() {

    private val _focusState = MutableStateFlow(FocusState.INACTIVE)
    val focusState: StateFlow<FocusState> = _focusState.asStateFlow()

    private val _selectedDuration = MutableStateFlow(FocusDuration.MINUTES_30)
    val selectedDuration: StateFlow<FocusDuration> = _selectedDuration.asStateFlow()

    fun setDuration(duration: FocusDuration) {
        _selectedDuration.value = duration
    }

    fun startFocus() {
        val now = Instant.now()
        val endTime = _selectedDuration.value.duration?.let { now.plus(it) }

        _focusState.value = FocusState(
            isActive = true,
            startedAt = now,
            scheduledEndAt = endTime,
            repliesSent = 0,
            notificationsSilenced = 0
        )

        // Start the focus service
        // TODO: FocusService.start(context)

        // Start timer updates
        if (endTime != null) {
            startTimerUpdates()
        }
    }

    fun stopFocus() {
        _focusState.value = FocusState.INACTIVE

        // Stop the focus service
        // TODO: FocusService.stop(context)
    }

    private fun startTimerUpdates() {
        viewModelScope.launch {
            while (_focusState.value.isActive) {
                delay(1000) // Update every second

                val current = _focusState.value
                if (current.shouldAutoEnd) {
                    stopFocus()
                    break
                }

                // Trigger recomposition by updating the state
                _focusState.value = current.copy()
            }
        }
    }

    fun incrementRepliesSent() {
        _focusState.value = _focusState.value.copy(
            repliesSent = _focusState.value.repliesSent + 1
        )
    }

    fun incrementNotificationsSilenced() {
        _focusState.value = _focusState.value.copy(
            notificationsSilenced = _focusState.value.notificationsSilenced + 1
        )
    }
}
