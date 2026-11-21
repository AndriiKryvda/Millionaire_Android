package com.vibecoding.millionaire.domain.game

import kotlin.test.assertEquals
import kotlin.test.assertTrue
import kotlin.test.Test

class NicknameValidatorTest {

    @Test
    fun `accepts Ukrainian and Latin nicknames`() {
        assertTrue(NicknameValidator.validate("Олена") == null)
        assertTrue(NicknameValidator.validate("Olena") == null)
        assertTrue(NicknameValidator.validate("Юрко123") == null)
        assertTrue(NicknameValidator.validate("Олесь Ґаврилюк") == null)
        assertTrue(NicknameValidator.validate("Іван-Пилипенко") == null)
        assertTrue(NicknameValidator.validate("Марія_К") == null)
        assertTrue(NicknameValidator.validate("П'ятниця") == null)
        assertTrue(NicknameValidator.validate("Oksanaʼs") == null)
    }

    @Test
    fun `rejects too short and blank nicknames`() {
        assertEquals(NicknameError.Blank, NicknameValidator.validate(""))
        assertEquals(NicknameError.Blank, NicknameValidator.validate("   "))
        assertEquals(NicknameError.TooShort, NicknameValidator.validate("Я"))
    }

    @Test
    fun `rejects nicknames longer than sixteen characters`() {
        assertEquals(NicknameError.TooLong, NicknameValidator.validate("a".repeat(17)))
        assertTrue(NicknameValidator.validate("a".repeat(16)) == null)
    }

    @Test
    fun `rejects forbidden characters`() {
        assertEquals(NicknameError.InvalidCharacters, NicknameValidator.validate("Олена!"))
        assertEquals(NicknameError.InvalidCharacters, NicknameValidator.validate("Nick@Name"))
        assertEquals(NicknameError.InvalidCharacters, NicknameValidator.validate("Гравець#1"))
        assertEquals(NicknameError.InvalidCharacters, NicknameValidator.validate("Привіт, світе"))
    }

    @Test
    fun `normalises case and surrounding spaces`() {
        assertEquals("оксана", NicknameValidator.normalize("  Оксана "))
    }
}