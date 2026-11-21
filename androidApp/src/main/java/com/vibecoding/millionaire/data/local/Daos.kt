package com.vibecoding.millionaire.data.local

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface PlayerDao {
    @Query("SELECT * FROM players ORDER BY lastPlayedAt IS NULL, lastPlayedAt DESC, createdAt DESC")
    fun observeAll(): Flow<List<PlayerEntity>>

    @Query("SELECT * FROM players WHERE id = :id")
    fun observeById(id: Long): Flow<PlayerEntity?>

    @Query("SELECT * FROM players WHERE id = :id")
    suspend fun getById(id: Long): PlayerEntity?

    @Query("SELECT COUNT(*) FROM players WHERE normalizedNickname = :normalized AND id != :excludeId")
    suspend fun countByNickname(normalized: String, excludeId: Long): Int

    @Insert suspend fun insert(entity: PlayerEntity): Long

    @Update suspend fun update(entity: PlayerEntity)

    @Delete suspend fun delete(entity: PlayerEntity)

    @Query("UPDATE players SET lastPlayedAt = :at WHERE id = :id")
    suspend fun markPlayed(id: Long, at: Long)
}

@Dao
interface GameResultDao {
    @Insert suspend fun insert(entity: GameResultEntity): Long

    @Query("SELECT * FROM game_results WHERE id = :id")
    fun observeById(id: Long): Flow<GameResultEntity?>

    @Query(
        """
        SELECT game_results.* FROM game_results
        INNER JOIN players ON players.id = game_results.playerId
        WHERE (:ageGroup IS NULL OR players.ageGroup = :ageGroup)
        ORDER BY game_results.prize DESC, game_results.finishedAt ASC
        LIMIT :limit
        """,
    )
    fun observeTop(ageGroup: String?, limit: Int): Flow<List<GameResultEntity>>

    @Query(
        """
        SELECT COUNT(*) AS games,
               IFNULL(MAX(prize), 0) AS bestPrize,
               IFNULL(SUM(prize), 0) AS totalPrize,
               IFNULL(SUM(CASE WHEN outcome = 'WON' THEN 1 ELSE 0 END), 0) AS wins
        FROM game_results WHERE playerId = :playerId
        """,
    )
    fun observeStats(playerId: Long): Flow<PlayerStatsRow>

    @Query("DELETE FROM game_results WHERE playerId = :playerId")
    suspend fun deleteForPlayer(playerId: Long)
}

data class PlayerStatsRow(
    val games: Int,
    val bestPrize: Int,
    val totalPrize: Int,
    val wins: Int,
)

@Dao
interface SeenQuestionDao {
    @Query("SELECT questionId FROM seen_questions WHERE playerId = :playerId")
    suspend fun seenQuestionIds(playerId: Long): List<Int>

    @Query("SELECT COUNT(*) FROM seen_questions WHERE playerId = :playerId")
    suspend fun countSeen(playerId: Long): Int

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertAll(rows: List<SeenQuestionEntity>)

    @Query("DELETE FROM seen_questions WHERE playerId = :playerId")
    suspend fun clear(playerId: Long)
}