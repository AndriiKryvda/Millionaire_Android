package com.vibecoding.millionaire.domain.game

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class NicknameValidatorTest {

    @Test
    fun `accepts Ukrainian and Latin nicknames`() {
        assertNull(NicknameValidator.validate("Олена"))
        assertNull(NicknameValidator.validate("Olena"))
        assertNull(NicknameValidator.validate("Юрко123"))
        assertNull(NicknameValidator.validate("Олесь Ґаврилюк"))
        assertNull(NicknameValidator.validate("Іван-Пилипенко"))
        assertNull(NicknameValidator.validate("Марія_К"))
        assertNull(NicknameValidator.validate("П'ятниця"))
        assertNull(NicknameValidator.validate("Oksanaʼs"))
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
        assertNull(NicknameValidator.validate("a".repeat(16)))
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