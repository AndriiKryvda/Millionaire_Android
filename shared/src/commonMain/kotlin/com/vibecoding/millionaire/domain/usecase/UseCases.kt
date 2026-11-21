package com.vibecoding.millionaire.domain.usecase

import com.vibecoding.millionaire.domain.game.QuestionSelector
import com.vibecoding.millionaire.domain.game.SelectionResult
import com.vibecoding.millionaire.domain.model.AgeGroup
import com.vibecoding.millionaire.domain.model.GameResult
import com.vibecoding.millionaire.domain.model.Question
import com.vibecoding.millionaire.domain.repository.GameResultRepository
import com.vibecoding.millionaire.domain.repository.PlayerRepository
import com.vibecoding.millionaire.domain.repository.QuestionBankRepository
import kotlinx.coroutines.flow.Flow

sealed interface StartGameOutcome {
    data class Ready(val questions: List<Question>, val fallbackUsed: Boolean) : StartGameOutcome
    data object PlayerNotFound : StartGameOutcome
    data object NotEnoughQuestions : StartGameOutcome
}

/** FR-3.1 - FR-3.4: builds the question set of a new game for a player. */
class StartGameUseCase(
    private val playerRepository: PlayerRepository,
    private val questionBankRepository: QuestionBankRepository,
    private val questionSelector: QuestionSelector,
) {
    suspend operator fun invoke(playerId: Long): StartGameOutcome {
        val player = playerRepository.getPlayer(playerId) ?: return StartGameOutcome.PlayerNotFound
        return select(player.id, player.ageGroup)
    }

    private suspend fun select(playerId: Long, ageGroup: AgeGroup): StartGameOutcome {
        val bank = questionBankRepository.getBank()
        val seen = playerRepository.seenQuestionIds(playerId)
        return when (val result = questionSelector.select(bank, ageGroup, seen)) {
            is SelectionResult.Ready -> StartGameOutcome.Ready(result.questions, result.fallbackUsed)
            SelectionResult.NotEnoughQuestions -> StartGameOutcome.NotEnoughQuestions
        }
    }
}

/** FR-3.4: can a new game be started without reusing already seen questions? */
class CheckGameAvailabilityUseCase(
    private val playerRepository: PlayerRepository,
    private val questionBankRepository: QuestionBankRepository,
    private val questionSelector: QuestionSelector,
) {
    suspend operator fun invoke(playerId: Long): Boolean {
        val player = playerRepository.getPlayer(playerId) ?: return false
        val bank = questionBankRepository.getBank()
        return questionSelector.canStart(bank, player.ageGroup, playerRepository.seenQuestionIds(playerId))
    }
}

class SaveGameResultUseCase(
    private val gameResultRepository: GameResultRepository,
) {
    suspend operator fun invoke(result: GameResult): Long = gameResultRepository.saveResult(result)
}

class ObserveTopResultsUseCase(
    private val gameResultRepository: GameResultRepository,
) {
    operator fun invoke(ageGroup: AgeGroup?): Flow<List<GameResult>> =
        gameResultRepository.observeTopResults(ageGroup)
}

/** FR-3.4: forgets which questions the player has already seen. */
class ResetGameHistoryUseCase(
    private val playerRepository: PlayerRepository,
) {
    suspend operator fun invoke(playerId: Long) = playerRepository.clearHistory(playerId)
}