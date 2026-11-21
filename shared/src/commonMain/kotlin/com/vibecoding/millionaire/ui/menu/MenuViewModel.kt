package com.vibecoding.millionaire.ui.menu

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.vibecoding.millionaire.domain.model.Player
import com.vibecoding.millionaire.domain.model.PlayerStats
import com.vibecoding.millionaire.domain.repository.GameResultRepository
import com.vibecoding.millionaire.domain.repository.PlayerRepository
import com.vibecoding.millionaire.domain.repository.QuestionBankRepository
import com.vibecoding.millionaire.domain.usecase.CheckGameAvailabilityUseCase
import com.vibecoding.millionaire.domain.usecase.ResetGameHistoryUseCase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class MenuUiState(
    val player: Player? = null,
    val stats: PlayerStats = PlayerStats(),
    val totalQuestions: Int = 0,
    val availableQuestions: Int = 0,
    val canStart: Boolean = false,
    val bankError: Boolean = false,
    val showNotEnoughDialog: Boolean = false,
    val navigateToGame: Boolean = false,
)

class MenuViewModel(
    private val playerId: Long,
    private val playerRepository: PlayerRepository,
    private val gameResultRepository: GameResultRepository,
    private val questionBankRepository: QuestionBankRepository,
    private val checkGameAvailability: CheckGameAvailabilityUseCase,
    private val resetGameHistory: ResetGameHistoryUseCase,
) : ViewModel() {

    private val _state = MutableStateFlow(MenuUiState())
    val state: StateFlow<MenuUiState> = _state.asStateFlow()

    init {
        viewModelScope.launch {
            playerRepository.observePlayer(playerId).collect { player ->
                if (player != null) {
                    _state.update { it.copy(player = player) }
                    refresh(player)
                }
            }
        }
        viewModelScope.launch {
            gameResultRepository.observeStats(playerId).collect { stats ->
                _state.update { it.copy(stats = stats) }
            }
        }
    }

    private suspend fun refresh(player: Player) {
        val bank = runCatching { questionBankRepository.getBank() }.getOrNull()
        if (bank == null) {
            _state.update { it.copy(bankError = true) }
            return
        }
        val seen = playerRepository.seenQuestionIds(playerId)
        _state.update {
            it.copy(
                totalQuestions = bank.countFor(player.ageGroup),
                availableQuestions = bank.availableFor(player.ageGroup, seen),
                canStart = checkGameAvailability(playerId),
            )
        }
    }

    /** FR-3.4: refuse to start a game that would repeat questions, unless the history is reset. */
    fun startGame() {
        viewModelScope.launch {
            if (checkGameAvailability(playerId)) {
                _state.update { it.copy(navigateToGame = true) }
            } else {
                _state.update { it.copy(showNotEnoughDialog = true) }
            }
        }
    }

    fun resetHistoryAndPlay() {
        viewModelScope.launch {
            resetGameHistory(playerId)
            _state.update { it.copy(showNotEnoughDialog = false, navigateToGame = true) }
        }
    }

    /**
     * Clears the one-shot navigation flag once the game screen has been opened. Without this the
     * flag stays `true`, so coming back from the game immediately starts a brand new game.
     */
    fun onGameNavigated() = _state.update { it.copy(navigateToGame = false) }

    fun dismissNotEnoughDialog() {
        _state.update { it.copy(showNotEnoughDialog = false) }
    }
}