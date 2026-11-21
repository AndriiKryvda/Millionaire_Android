package com.vibecoding.millionaire.data.local

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "players",
    indices = [Index(value = ["normalizedNickname"], unique = true)],
)
data class PlayerEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0L,
    val nickname: String,
    val normalizedNickname: String,
    /** Name of [com.vibecoding.millionaire.domain.model.AgeGroup]. */
    val ageGroup: String,
    val createdAt: Long,
    val lastPlayedAt: Long? = null,
)

@Entity(
    tableName = "game_results",
    foreignKeys = [
        ForeignKey(
            entity = PlayerEntity::class,
            parentColumns = ["id"],
            childColumns = ["playerId"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
    indices = [Index("playerId")],
)
data class GameResultEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0L,
    val playerId: Long,
    /** Denormalised copy so that records survive a later rename of the player (FR-6.2). */
    val nickname: String = "",
    val ageGroup: String,
    val startedAt: Long,
    val finishedAt: Long,
    val correctAnswers: Int,
    val prize: Int,
    /** Name of [com.vibecoding.millionaire.domain.model.GameOutcome]. */
    val outcome: String,
    /** Comma separated names of [com.vibecoding.millionaire.domain.model.Lifeline]. */
    val lifelinesUsed: String = "",
    @ColumnInfo(defaultValue = "") val lastQuestionText: String = "",
    @ColumnInfo(defaultValue = "") val lastCorrectAnswerText: String = "",
)

@Entity(
    tableName = "seen_questions",
    primaryKeys = ["playerId", "questionId"],
    foreignKeys = [
        ForeignKey(
            entity = PlayerEntity::class,
            parentColumns = ["id"],
            childColumns = ["playerId"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
    indices = [Index("playerId")],
)
data class SeenQuestionEntity(
    val playerId: Long,
    val questionId: Int,
    val seenAt: Long,
)