package com.vibecoding.millionaire.ui.game

import org.jetbrains.compose.resources.StringResource

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.vibecoding.millionaire.generated.resources.Res
import com.vibecoding.millionaire.generated.resources.*
import com.vibecoding.millionaire.domain.game.GameEngine
import com.vibecoding.millionaire.domain.game.GameState
import com.vibecoding.millionaire.domain.game.Phase
import com.vibecoding.millionaire.domain.model.AnswerOption
import com.vibecoding.millionaire.domain.model.GameOutcome
import com.vibecoding.millionaire.domain.model.GameResult
import com.vibecoding.millionaire.domain.model.Lifeline
import com.vibecoding.millionaire.domain.model.Question
import com.vibecoding.millionaire.domain.repository.PlayerRepository
import com.vibecoding.millionaire.domain.usecase.SaveGameResultUseCase
import com.vibecoding.millionaire.domain.usecase.StartGameOutcome
import com.vibecoding.millionaire.domain.usecase.StartGameUseCase
import com.vibecoding.millionaire.ui.feedback.FeedbackManager
import com.vibecoding.millionaire.ui.feedback.HapticEffect
import com.vibecoding.millionaire.ui.feedback.SoundEffect
import com.vibecoding.millionaire.util.Clock
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class GameUiState(
    val loading: Boolean = true,
    val errorRes: StringResource? = null,
    val nickname: String = "",
    val game: GameState? = null,
    val showAudience: Boolean = false,
    val showFriend: Boolean = false,
    val showExitDialog: Boolean = false,
    val showWalkAwayDialog: Boolean = false,
    val resultId: Long? = null,
    val exitRequested: Boolean = false,
)

class GameViewModel(
    private val playerId: Long,
    private val playerRepository: PlayerRepository,
    private val startGameUseCase: StartGameUseCase,
    private val saveGameResult: SaveGameResultUseCase,
    private val gameEngine: GameEngine,
    private val clock: Clock,
    private val feedback: FeedbackManager,
) : ViewModel() {

    private val _state = MutableStateFlow(GameUiState())
    val state: StateFlow<GameUiState> = _state.asStateFlow()

    init {
        start()
    }

    private fun start() {
        viewModelScope.launch {
            when (val outcome = startGameUseCase(playerId)) {
                is StartGameOutcome.Ready -> {
                    val player = playerRepository.getPlayer(playerId)
                        ?: return@launch updateError(Res.string.error_player_not_found)
                    playerRepository.markPlayed(playerId, clock.now())
                    val game = gameEngine.start(player.ageGroup, outcome.questions, clock.now())
                    _state.update { it.copy(loading = false, nickname = player.nickname, game = game) }
                }

                StartGameOutcome.PlayerNotFound -> updateError(Res.string.error_player_not_found)
                StartGameOutcome.NotEnoughQuestions -> updateError(Res.string.error_not_enough_questions)
            }
        }
    }

    private fun updateError(res: StringResource) {
        _state.update { it.copy(loading = false, errorRes = res) }
    }

    fun onSelect(option: AnswerOption) {
        val game = _state.value.game ?: return
        val updated = gameEngine.select(game, option)
        if (updated !== game) {
            feedback.play(SoundEffect.CLICK)
            feedback.haptic(HapticEffect.TICK)
        }
        _state.update { it.copy(game = updated) }
    }

    fun onCancelSelection() {
        val game = _state.value.game ?: return
        _state.update { it.copy(game = gameEngine.cancelSelection(game)) }
    }

    fun onConfirm() {
        val game = _state.value.game ?: return
        val revealing = gameEngine.confirm(game)
        if (revealing.phase != Phase.REVEALING) return
        _state.update { it.copy(game = revealing) }
        feedback.play(SoundEffect.SELECT)

        viewModelScope.launch {
            delay(REVEAL_DELAY_MS)
            val revealed = gameEngine.completeReveal(revealing)
            _state.update { it.copy(game = revealed) }
            when (revealed.phase) {
                Phase.ANSWER_CORRECT -> {
                    feedback.play(SoundEffect.CORRECT)
                    feedback.haptic(HapticEffect.SUCCESS)
                }

                else -> {
                    feedback.play(SoundEffect.WRONG)
                    feedback.haptic(HapticEffect.ERROR)
                }
            }
            markSeen(revealed.questions[revealed.currentIndex].id)
        }
    }

    /** "Далі" after the reveal: either the next question or the result screen. */
    fun onContinue() {
        val game = _state.value.game ?: return
        when (game.phase) {
            Phase.ANSWER_CORRECT -> {
                val advanced = gameEngine.advance(game)
                _state.update { it.copy(game = advanced) }
                if (advanced.phase == Phase.FINISHED) finish(advanced)
            }

            Phase.ANSWER_WRONG -> {
                val finished = gameEngine.finishWithWrongAnswer(game)
                _state.update { it.copy(game = finished) }
                finish(finished)
            }

            else -> Unit
        }
    }

    fun onWalkAwayRequest() {
        val game = _state.value.game ?: return
        if (game.phase == Phase.AWAITING_ANSWER || game.phase == Phase.CONFIRMING) {
            _state.update { it.copy(showWalkAwayDialog = true) }
        }
    }

    fun onWalkAwayConfirmed() {
        val game = _state.value.game ?: return
        val finished = gameEngine.walkAway(game)
        _state.update { it.copy(game = finished, showWalkAwayDialog = false) }
        if (finished.phase == Phase.FINISHED) {
            feedback.play(SoundEffect.WALK_AWAY)
            finish(finished)
        }
    }

    fun onWalkAwayDismissed() = _state.update { it.copy(showWalkAwayDialog = false) }

    fun onExitRequest() = _state.update { it.copy(showExitDialog = true) }

    fun onExitCancelled() = _state.update { it.copy(showExitDialog = false) }

    /** Leaving without confirmation loses the current round (no result is stored). */
    fun onExitConfirmed() = _state.update { it.copy(showExitDialog = false, exitRequested = true) }

    fun onLifeline(lifeline: Lifeline) {
        val game = _state.value.game ?: return
        if (!gameEngine.canUseLifeline(game, lifeline)) {
            when (lifeline) {
                Lifeline.ASK_AUDIENCE -> if (Lifeline.ASK_AUDIENCE in game.usedLifelines) _state.update { it.copy(showAudience = true) }
                Lifeline.PHONE_FRIEND -> if (Lifeline.PHONE_FRIEND in game.usedLifelines) _state.update { it.copy(showFriend = true) }
                Lifeline.FIFTY_FIFTY -> Unit
            }
            return
        }
        val updated = when (lifeline) {
            Lifeline.FIFTY_FIFTY -> gameEngine.useFiftyFifty(game)
            Lifeline.ASK_AUDIENCE -> gameEngine.useAudience(game)
            Lifeline.PHONE_FRIEND -> gameEngine.usePhoneFriend(game)
        }
        feedback.play(SoundEffect.CLICK)
        _state.update {
            it.copy(
                game = updated,
                showAudience = lifeline == Lifeline.ASK_AUDIENCE,
                showFriend = lifeline == Lifeline.PHONE_FRIEND,
            )
        }
    }

    fun onAudienceDismissed() = _state.update { it.copy(showAudience = false) }

    fun onFriendDismissed() = _state.update { it.copy(showFriend = false) }

    fun currentQuestion(game: GameState): Question = gameEngine.currentQuestion(game)

    fun visibleOptions(game: GameState): List<AnswerOption> = gameEngine.visibleOptions(game)

    private fun markSeen(questionId: Int) {
        viewModelScope.launch {
            playerRepository.markQuestionsSeen(playerId, listOf(questionId), clock.now())
        }
    }

    private fun finish(game: GameState) {
        viewModelScope.launch {
            val question = game.questions[game.currentIndex]
            val wrong = game.outcome == GameOutcome.WRONG_ANSWER
            val result = GameResult(
                playerId = playerId,
                nickname = _state.value.nickname,
                ageGroup = game.ageGroup,
                startedAt = game.startedAt,
                finishedAt = clock.now(),
                correctAnswers = game.correctAnswers,
                prize = game.prize,
                outcome = game.outcome ?: GameOutcome.WALKED_AWAY,
                lifelinesUsed = game.usedLifelines,
                lastQuestionText = question.text.takeIf { wrong },
                lastCorrectAnswerText = question.options[question.correct]?.takeIf { wrong },
            )
            val id = saveGameResult(result)
            if (game.outcome == GameOutcome.WON) feedback.play(SoundEffect.WIN)
            _state.update { it.copy(resultId = id) }
        }
    }

    private companion object {
        /** Suspense between "Підтвердити" and the verdict (FR-2.6). */
        const val REVEAL_DELAY_MS = 2_000L
    }
}
