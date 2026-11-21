package com.vibecoding.millionaire.domain.game

import com.vibecoding.millionaire.domain.model.AnswerOption
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import kotlin.test.Test
import kotlin.random.Random

class LifelineEngineTest {

    private val engine = LifelineEngine()
    private val all = listOf(AnswerOption.A, AnswerOption.B, AnswerOption.C, AnswerOption.D)

    @Test
    fun `fifty fifty hides two wrong options and keeps the correct one`() {
        for (seed in 0..50) {
            val hidden = engine.fiftyFifty(all, AnswerOption.C, Random(seed))
            assertEquals(2, hidden.size)
            assertFalse(AnswerOption.C in hidden)
            assertTrue(hidden.all { it != AnswerOption.C })
        }
    }

    @Test
    fun `fifty fifty after a reduced set keeps two options`() {
        val hidden = engine.fiftyFifty(listOf(AnswerOption.A, AnswerOption.B, AnswerOption.C), AnswerOption.A, Random(7))
        assertEquals(1, hidden.size)
        assertFalse(AnswerOption.A in hidden)
    }

    @Test
    fun `audience percentages always add up to one hundred`() {
        for (seed in 0..200) {
            for (difficulty in 1..5) {
                val result = engine.audience(all, AnswerOption.B, difficulty, Random(seed))
                assertEquals(all.toSet(), result.keys)
                assertEquals(100, result.values.sum())
                assertTrue(result.values.all { it >= 0 })
            }
        }
    }

    @Test
    fun `audience only votes for the options that are still visible`() {
        val visible = listOf(AnswerOption.A, AnswerOption.D)
        val result = engine.audience(visible, AnswerOption.D, 3, Random(11))
        assertEquals(visible.toSet(), result.keys)
    }

    @Test
    fun `phone friend suggests a visible option`() {
        for (seed in 0..100) {
            for (difficulty in 1..5) {
                val hint = engine.phoneFriend(all, AnswerOption.A, difficulty, Random(seed))
                assertTrue(hint.answer in all)
                assertTrue(hint.friend in Friends.all)
            }
        }
    }

    @Test
    fun `lifelines get less reliable with the difficulty`() {
        val accuracies = (1..5).map { engine.accuracyFor(it) }
        assertEquals(accuracies.sortedDescending(), accuracies)
        assertTrue(accuracies.first() > accuracies.last())
    }
}