package com.vibecoding.millionaire.domain.game

import com.vibecoding.millionaire.domain.model.AgeGroup
import com.vibecoding.millionaire.domain.model.Question
import com.vibecoding.millionaire.domain.model.QuestionBank
import kotlin.random.Random

sealed interface SelectionResult {
    data class Ready(val questions: List<Question>, val fallbackUsed: Boolean) : SelectionResult
    data object NotEnoughQuestions : SelectionResult
}

/**
 * Builds the 15 questions of a game: 3 questions per difficulty level for the player's age group,
 * preferring questions the player has not seen yet (FR-3.3, FR-3.4).
 */
class QuestionSelector {

    /** Cheap pre-check used before the "Нова гра" button actually starts a game (FR-3.4). */
    fun canStart(bank: QuestionBank, age: AgeGroup, seenIds: Set<Int>): Boolean =
        (1..PrizeLadder.DIFFICULTY_LEVELS).all { level ->
            bank.availablePool(age, level, seenIds).size >= PrizeLadder.QUESTIONS_PER_DIFFICULTY
        }

    fun select(
        bank: QuestionBank,
        age: AgeGroup,
        seenIds: Set<Int>,
        random: Random = Random.Default,
    ): SelectionResult {
        val selected = ArrayList<Question>(PrizeLadder.QUESTIONS_COUNT)
        val used = HashSet<Int>()
        var fallbackUsed = false

        for (level in 1..PrizeLadder.DIFFICULTY_LEVELS) {
            val need = PrizeLadder.QUESTIONS_PER_DIFFICULTY
            val fresh = bank.availablePool(age, level, seenIds + used).shuffled(random)
            if (fresh.size >= need) {
                fresh.take(need).forEach { selected += it; used += it.id }
                continue
            }
            // Not enough unseen questions in this cell -> reuse seen ones (FR-3.4).
            val reusable = bank.pool(age, level).filter { it.id !in used }.shuffled(random)
            if (reusable.size < need) return SelectionResult.NotEnoughQuestions
            fallbackUsed = true
            reusable.take(need).forEach { selected += it; used += it.id }
        }

        if (selected.size != PrizeLadder.QUESTIONS_COUNT) return SelectionResult.NotEnoughQuestions
        return SelectionResult.Ready(selected, fallbackUsed)
    }
}