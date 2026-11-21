package com.vibecoding.millionaire.data.repository

import android.util.Log
import com.vibecoding.millionaire.data.local.PlayerDao
import com.vibecoding.millionaire.data.local.PlayerEntity
import com.vibecoding.millionaire.data.local.SeenQuestionDao
import com.vibecoding.millionaire.data.local.SeenQuestionEntity
import com.vibecoding.millionaire.domain.game.NicknameValidator
import com.vibecoding.millionaire.domain.model.AgeGroup
import com.vibecoding.millionaire.domain.model.Player
import com.vibecoding.millionaire.domain.repository.PlayerRepository
import com.vibecoding.millionaire.util.Clock
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class PlayerRepositoryImpl @Inject constructor(
    private val playerDao: PlayerDao,
    private val seenQuestionDao: SeenQuestionDao,
    private val clock: Clock,
) : PlayerRepository {

    override fun observePlayers(): Flow<List<Player>> =
        playerDao.observeAll().map { rows -> rows.map { it.toDomain() } }

    override fun observePlayer(id: Long): Flow<Player?> = playerDao.observeById(id).map { it?.toDomain() }

    override suspend fun getPlayer(id: Long): Player? = playerDao.getById(id)?.toDomain()

    override suspend fun isNicknameTaken(nickname: String, excludeId: Long): Boolean =
        playerDao.countByNickname(NicknameValidator.normalize(nickname), excludeId) > 0

    override suspend fun createPlayer(nickname: String, ageGroup: AgeGroup): Long =
        playerDao.insert(
            PlayerEntity(
                nickname = nickname.trim(),
                normalizedNickname = NicknameValidator.normalize(nickname),
                ageGroup = ageGroup.name,
                createdAt = clock.now(),
            ),
        )

    override suspend fun updatePlayer(id: Long, nickname: String, ageGroup: AgeGroup) {
        val existing = playerDao.getById(id) ?: return
        playerDao.update(
            existing.copy(
                nickname = nickname.trim(),
                normalizedNickname = NicknameValidator.normalize(nickname),
                ageGroup = ageGroup.name,
            ),
        )
    }

    override suspend fun deletePlayer(id: Long) {
        playerDao.getById(id)?.let { playerDao.delete(it) }
    }

    override suspend fun markPlayed(id: Long, at: Long) = playerDao.markPlayed(id, at)

    override suspend fun seenQuestionIds(playerId: Long): Set<Int> = seenQuestionDao.seenQuestionIds(playerId).toSet()

    override suspend fun markQuestionsSeen(playerId: Long, questionIds: Collection<Int>, at: Long) {
        if (questionIds.isEmpty()) return
        seenQuestionDao.insertAll(questionIds.distinct().map { SeenQuestionEntity(playerId, it, at) })
    }

    override suspend fun clearHistory(playerId: Long) = seenQuestionDao.clear(playerId)

    private fun PlayerEntity.toDomain(): Player = Player(
        id = id,
        nickname = nickname,
        ageGroup = AgeGroup.fromName(ageGroup) ?: AgeGroup.AGE_19_60.also {
            Log.w(TAG, "Unknown age group '$ageGroup' for player $id, falling back to $it")
        },
        createdAt = createdAt,
        lastPlayedAt = lastPlayedAt,
    )

    private companion object {
        const val TAG = "PlayerRepository"
    }
}