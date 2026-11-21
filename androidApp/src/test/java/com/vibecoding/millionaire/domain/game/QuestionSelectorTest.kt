package com.vibecoding.millionaire.domain.game

import com.vibecoding.millionaire.bankWith
import com.vibecoding.millionaire.domain.model.AgeGroup
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.random.Random

class QuestionSelectorTest {

    private val selector = QuestionSelector()
    private val age = AgeGroup.AGE_13_14

    @Test
    fun `selects fifteen questions with three per difficulty level`() {
        val result = selector.select(bankWith(perDifficulty = 10), age, emptySet(), Random(1))
        assertTrue(result is SelectionResult.Ready)
        val ready = result as SelectionResult.Ready
        assertEquals(15, ready.questions.size)
        assertEquals(15, ready.questions.map { it.id }.distinct().size)
        for (level in 1..PrizeLadder.DIFFICULTY_LEVELS) {
            assertEquals(3, ready.questions.count { it.difficultyFor(age) == level })
        }
        assertFalse(ready.fallbackUsed)
    }

    @Test
    fun `prefers questions the player has not seen`() {
        val bank = bankWith(perDifficulty = 6)
        val first = selector.select(bank, age, emptySet(), Random(2)) as SelectionResult.Ready
        val seen = first.questions.map { it.id }.toSet()

        val second = selector.select(bank, age, seen, Random(3)) as SelectionResult.Ready
        assertEquals(0, second.questions.count { it.id in seen })
        assertFalse(second.fallbackUsed)
    }

    @Test
    fun `reuses seen questions when a cell runs out and reports the fallback`() {
        val bank = bankWith(perDifficulty = 4)
        val seen = bank.all
            .groupBy { it.difficultyFor(age) }
            .values
            .flatMap { group -> group.take(2).map { it.id } }
            .toSet()

        val result = selector.select(bank, age, seen, Random(4))
        assertTrue(result is SelectionResult.Ready)
        val ready = result as SelectionResult.Ready
        assertEquals(15, ready.questions.size)
        assertTrue(ready.fallbackUsed)
        assertTrue(ready.questions.any { it.id in seen })
    }

    @Test
    fun `reports not enough questions when a cell cannot provide three distinct ones`() {
        assertEquals(
            SelectionResult.NotEnoughQuestions,
            selector.select(bankWith(perDifficulty = 2), age, emptySet(), Random(5)),
        )
    }

    @Test
    fun `canStart mirrors the availability of every cell`() {
        assertTrue(selector.canStart(bankWith(perDifficulty = 3), age, emptySet()))
        assertFalse(selector.canStart(bankWith(perDifficulty = 2), age, emptySet()))

        val bank = bankWith(perDifficulty = 3)
        val seenOneQuestion = setOf(bank.pool(age, 1).first().id)
        assertFalse(selector.canStart(bank, age, seenOneQuestion))
    }
}