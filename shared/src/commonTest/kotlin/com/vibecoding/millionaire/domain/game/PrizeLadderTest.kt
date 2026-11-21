package com.vibecoding.millionaire.domain.game

import kotlin.test.assertEquals
import kotlin.test.Test

class PrizeLadderTest {

    @Test
    fun `ladder has fifteen amounts ending at one thousand`() {
        assertEquals(15, PrizeLadder.prizes.size)
        assertEquals(1000, PrizeLadder.prizes.last())
        assertEquals(1, PrizeLadder.prizes.first())
    }

    @Test
    fun `three questions per difficulty level`() {
        assertEquals(PrizeLadder.QUESTIONS_COUNT / PrizeLadder.QUESTIONS_PER_DIFFICULTY, 5)
        assertEquals(1, PrizeLadder.difficultyFor(0))
        assertEquals(1, PrizeLadder.difficultyFor(2))
        assertEquals(2, PrizeLadder.difficultyFor(3))
        assertEquals(5, PrizeLadder.difficultyFor(12))
        assertEquals(5, PrizeLadder.difficultyFor(14))
    }

    @Test
    fun `safe havens are the fifth and the tenth question`() {
        assertEquals(setOf(4, 9), PrizeLadder.safeHavenIndices)
        assertEquals(10, PrizeLadder.prizes[4])
        assertEquals(100, PrizeLadder.prizes[9])
    }

    @Test
    fun `walk away keeps the prize of the last answered question`() {
        assertEquals(0, PrizeLadder.walkAwayPrize(0))
        assertEquals(1, PrizeLadder.walkAwayPrize(1))
        assertEquals(3, PrizeLadder.walkAwayPrize(3))
        assertEquals(1000, PrizeLadder.walkAwayPrize(15))
    }

    @Test
    fun `wrong answer returns the last passed safe haven`() {
        assertEquals(0, PrizeLadder.wrongAnswerPrize(0))
        assertEquals(0, PrizeLadder.wrongAnswerPrize(4))
        assertEquals(10, PrizeLadder.wrongAnswerPrize(5))
        assertEquals(10, PrizeLadder.wrongAnswerPrize(9))
        assertEquals(100, PrizeLadder.wrongAnswerPrize(10))
        assertEquals(100, PrizeLadder.wrongAnswerPrize(14))
    }
}