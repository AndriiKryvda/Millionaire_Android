package com.vibecoding.millionaire.domain.model

import org.jetbrains.compose.resources.StringResource

import com.vibecoding.millionaire.generated.resources.Res
import com.vibecoding.millionaire.generated.resources.*

/** Age groups the question bank is rated for (FR-1.5). */
enum class AgeGroup(val jsonKey: String, val titleRes: StringResource) {
    AGE_10_12("10-12", Res.string.age_10_12),
    AGE_13_14("13-14", Res.string.age_13_14),
    AGE_15_18("15-18", Res.string.age_15_18),
    AGE_19_60("19-60", Res.string.age_19_60);

    companion object {
        fun fromJsonKey(key: String): AgeGroup? = entries.firstOrNull { it.jsonKey == key }
        fun fromName(name: String): AgeGroup? = entries.firstOrNull { it.name == name }
    }
}

/** Original option keys used in `questions.json` / `answers.json`. */
enum class AnswerOption {
    A, B, C, D;

    companion object {
        fun fromKey(key: String): AnswerOption? = entries.firstOrNull { it.name.equals(key.trim(), ignoreCase = true) }
    }
}

data class Question(
    val id: Int,
    val text: String,
    val options: Map<AnswerOption, String>,
    val category: String,
    val difficultyByAge: Map<AgeGroup, Int>,
    val correct: AnswerOption,
) {
    fun difficultyFor(age: AgeGroup): Int = difficultyByAge[age] ?: DEFAULT_DIFFICULTY

    companion object {
        const val DEFAULT_DIFFICULTY = 3
    }
}

data class Player(
    val id: Long,
    val nickname: String,
    val ageGroup: AgeGroup,
    val createdAt: Long,
    val lastPlayedAt: Long?,
)

enum class Lifeline { FIFTY_FIFTY, ASK_AUDIENCE, PHONE_FRIEND }

enum class GameOutcome { WON, WRONG_ANSWER, WALKED_AWAY }

data class GameResult(
    val id: Long = 0L,
    val playerId: Long,
    val nickname: String = "",
    val ageGroup: AgeGroup,
    val startedAt: Long,
    val finishedAt: Long,
    val correctAnswers: Int,
    val prize: Int,
    val outcome: GameOutcome,
    val lifelinesUsed: Set<Lifeline> = emptySet(),
    /** Text of the last shown question; only set when the game ended with a wrong answer (FR-5.1). */
    val lastQuestionText: String? = null,
    val lastCorrectAnswerText: String? = null,
)

data class PlayerStats(
    val games: Int = 0,
    val bestPrize: Int = 0,
    val totalPrize: Int = 0,
    val wins: Int = 0,
)
