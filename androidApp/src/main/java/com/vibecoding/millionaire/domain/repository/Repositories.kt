package com.vibecoding.millionaire.domain.repository

import com.vibecoding.millionaire.domain.model.AgeGroup
import com.vibecoding.millionaire.domain.model.GameResult
import com.vibecoding.millionaire.domain.model.Player
import com.vibecoding.millionaire.domain.model.PlayerStats
import com.vibecoding.millionaire.domain.model.QuestionBank
import kotlinx.coroutines.flow.Flow

/** Loads and indexes the bundled question bank (FR-7.1). */
interface QuestionBankRepository {
    suspend fun getBank(): QuestionBank
}

interface PlayerRepository {
    fun observePlayers(): Flow<List<Player>>
    fun observePlayer(id: Long): Flow<Player?>
    suspend fun getPlayer(id: Long): Player?
    suspend fun isNicknameTaken(nickname: String, excludeId: Long = 0L): Boolean
    suspend fun createPlayer(nickname: String, ageGroup: AgeGroup): Long
    suspend fun updatePlayer(id: Long, nickname: String, ageGroup: AgeGroup)
    suspend fun deletePlayer(id: Long)
    suspend fun markPlayed(id: Long, at: Long)
    suspend fun seenQuestionIds(playerId: Long): Set<Int>
    suspend fun markQuestionsSeen(playerId: Long, questionIds: Collection<Int>, at: Long)
    suspend fun clearHistory(playerId: Long)
}

interface GameResultRepository {
    suspend fun saveResult(result: GameResult): Long
    fun observeResult(id: Long): Flow<GameResult?>
    fun observeTopResults(ageGroup: AgeGroup?): Flow<List<GameResult>>
    fun observeStats(playerId: Long): Flow<PlayerStats>
    suspend fun countSeenQuestions(playerId: Long): Int
}

data class AppSettings(
    val soundEnabled: Boolean = true,
    val vibrationEnabled: Boolean = true,
)

interface SettingsRepository {
    val settings: Flow<AppSettings>
    suspend fun setSoundEnabled(enabled: Boolean)
    suspend fun setVibrationEnabled(enabled: Boolean)
    suspend fun lastPlayerId(): Long?
    suspend fun setLastPlayerId(id: Long?)
}