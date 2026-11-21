package com.vibecoding.millionaire.ui.settings

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.vibecoding.millionaire.domain.repository.QuestionBankRepository
import com.vibecoding.millionaire.domain.repository.SettingsRepository
import com.vibecoding.millionaire.domain.usecase.ResetGameHistoryUseCase
import com.vibecoding.millionaire.ui.navigation.NavArgs
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

data class SettingsUiState(
    val soundEnabled: Boolean = true,
    val vibrationEnabled: Boolean = true,
    val questionCount: Int = 0,
    val showResetDialog: Boolean = false,
)

@HiltViewModel
class SettingsViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val settingsRepository: SettingsRepository,
    private val resetGameHistory: ResetGameHistoryUseCase,
    questionBankRepository: QuestionBankRepository,
) : ViewModel() {

    private val playerId: Long = savedStateHandle[NavArgs.PLAYER_ID] ?: -1L
    private val resetDialog = MutableStateFlow(false)
    private val questionCount = MutableStateFlow(0)

    val state: StateFlow<SettingsUiState> = combine(
        settingsRepository.settings,
        resetDialog,
        questionCount,
    ) { settings, showReset, count ->
        SettingsUiState(
            soundEnabled = settings.soundEnabled,
            vibrationEnabled = settings.vibrationEnabled,
            questionCount = count,
            showResetDialog = showReset,
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), SettingsUiState())

    init {
        viewModelScope.launch {
            questionCount.value = runCatching { questionBankRepository.getBank() }.getOrNull()?.size ?: 0
        }
    }

    fun setSoundEnabled(enabled: Boolean) {
        viewModelScope.launch { settingsRepository.setSoundEnabled(enabled) }
    }

    fun setVibrationEnabled(enabled: Boolean) {
        viewModelScope.launch { settingsRepository.setVibrationEnabled(enabled) }
    }

    fun askResetHistory() {
        resetDialog.value = true
    }

    fun dismissResetDialog() {
        resetDialog.value = false
    }

    /** FR-3.4: forgets the "already seen" marks so the whole bank becomes available again. */
    fun confirmResetHistory() {
        viewModelScope.launch {
            resetGameHistory(playerId)
            resetDialog.value = false
        }
    }
}