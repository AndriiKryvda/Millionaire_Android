package com.vibecoding.millionaire.data.repository

import com.vibecoding.millionaire.data.local.GameResultDao
import com.vibecoding.millionaire.data.local.GameResultEntity
import com.vibecoding.millionaire.data.local.PlayerDao
import com.vibecoding.millionaire.data.local.SeenQuestionDao
import com.vibecoding.millionaire.domain.model.AgeGroup
import com.vibecoding.millionaire.domain.model.GameOutcome
import com.vibecoding.millionaire.domain.model.GameResult
import com.vibecoding.millionaire.domain.model.Lifeline
import com.vibecoding.millionaire.domain.model.PlayerStats
import com.vibecoding.millionaire.domain.repository.GameResultRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class GameResultRepositoryImpl @Inject constructor(
    private val gameResultDao: GameResultDao,
    private val playerDao: PlayerDao,
    private val seenQuestionDao: SeenQuestionDao,
) : GameResultRepository {

    override suspend fun saveResult(result: GameResult): Long {
        val nickname = result.nickname.ifBlank { playerDao.getById(result.playerId)?.nickname.orEmpty() }
        return gameResultDao.insert(
            GameResultEntity(
                id = result.id,
                playerId = result.playerId,
                nickname = nickname,
                ageGroup = result.ageGroup.name,
                startedAt = result.startedAt,
                finishedAt = result.finishedAt,
                correctAnswers = result.correctAnswers,
                prize = result.prize,
                outcome = result.outcome.name,
                lifelinesUsed = result.lifelinesUsed.joinToString(",") { it.name },
                lastQuestionText = result.lastQuestionText.orEmpty(),
                lastCorrectAnswerText = result.lastCorrectAnswerText.orEmpty(),
            ),
        )
    }

    override fun observeResult(id: Long): Flow<GameResult?> = gameResultDao.observeById(id).map { it?.toDomain() }

    override fun observeTopResults(ageGroup: AgeGroup?): Flow<List<GameResult>> =
        gameResultDao.observeTop(ageGroup?.name, TOP_LIMIT).map { rows -> rows.map { it.toDomain() } }

    override fun observeStats(playerId: Long): Flow<PlayerStats> =
        gameResultDao.observeStats(playerId).map {
            PlayerStats(
                games = it.games,
                bestPrize = it.bestPrize,
                totalPrize = it.totalPrize,
                wins = it.wins,
            )
        }

    override suspend fun countSeenQuestions(playerId: Long): Int = seenQuestionDao.countSeen(playerId)

    private fun GameResultEntity.toDomain(): GameResult = GameResult(
        id = id,
        playerId = playerId,
        nickname = nickname,
        ageGroup = AgeGroup.fromName(ageGroup) ?: AgeGroup.AGE_19_60,
        startedAt = startedAt,
        finishedAt = finishedAt,
        correctAnswers = correctAnswers,
        prize = prize,
        outcome = GameOutcome.entries.firstOrNull { it.name == outcome } ?: GameOutcome.WRONG_ANSWER,
        lifelinesUsed = lifelinesUsed.split(',')
            .mapNotNull { chunk -> Lifeline.entries.firstOrNull { it.name == chunk.trim() } }
            .toSet(),
        lastQuestionText = lastQuestionText.ifBlank { null },
        lastCorrectAnswerText = lastCorrectAnswerText.ifBlank { null },
    )

    private companion object {
        const val TOP_LIMIT = 100
    }
}