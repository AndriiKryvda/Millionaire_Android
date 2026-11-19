package com.vibecoding.millionaire.domain.model

import com.vibecoding.millionaire.bankWith
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class QuestionBankTest {

    private val age = AgeGroup.AGE_15_18

    @Test
    fun `indexes questions by age group and difficulty`() {
        val bank = bankWith(perDifficulty = 7, age = age, levels = 1..3)
        assertEquals(21, bank.size)
        assertEquals(7, bank.pool(age, 1).size)
        assertEquals(0, bank.pool(age, 4).size)
        assertEquals(21, bank.countFor(age))
    }

    @Test
    fun `looks questions up by id`() {
        val bank = bankWith(perDifficulty = 2, age = age, levels = 1..3)
        assertEquals(1, bank.byId(1)?.id)
        assertNull(bank.byId(999))
    }

    @Test
    fun `available questions shrink with the seen history`() {
        val bank = bankWith(perDifficulty = 4, age = age, levels = 1..3)
        assertEquals(12, bank.availableFor(age, emptySet()))
        assertEquals(10, bank.availableFor(age, setOf(1, 2)))
        assertEquals(0, bank.availableFor(age, bank.all.map { it.id }.toSet()))

        val pool = bank.availablePool(age, 1, setOf(bank.pool(age, 1).first().id))
        assertEquals(3, pool.size)
        assertTrue(pool.none { it.id == bank.pool(age, 1).first().id })
    }

    @Test
    fun `questions rated for another age group are not counted`() {
        val bank = bankWith(perDifficulty = 3, age = AgeGroup.AGE_10_12, levels = 1..3)
        assertEquals(0, bank.countFor(age))
    }
}