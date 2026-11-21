package com.vibecoding.millionaire.domain.game


sealed interface NicknameError {
    data object Blank : NicknameError
    data object TooShort : NicknameError
    data object TooLong : NicknameError
    data object InvalidCharacters : NicknameError
    data object AlreadyTaken : NicknameError
}

/**
 * Nickname rules (FR-1.2): 2..16 characters, Ukrainian/Latin letters, digits, space, `_`, `-`
 * and the two apostrophes. Uniqueness is checked case-insensitively.
 */
object NicknameValidator {

    const val MIN_LENGTH = 2
    const val MAX_LENGTH = 16

    private val allowedChars = Regex("^[\\p{L}\\p{Lt}\\p{Lu}\\p{Ll}\\d'ʼ _\\-]+$")

    fun normalize(raw: String): String = raw.trim().lowercase()

    fun validate(raw: String): NicknameError? {
        val trimmed = raw.trim()
        if (trimmed.isEmpty()) return NicknameError.Blank
        if (trimmed.codePointCount(0, trimmed.length) < MIN_LENGTH) return NicknameError.TooShort
        if (trimmed.codePointCount(0, trimmed.length) > MAX_LENGTH) return NicknameError.TooLong
        if (!allowedChars.matches(trimmed)) return NicknameError.InvalidCharacters
        return null
    }

    fun isValid(raw: String): Boolean = validate(raw) == null
}