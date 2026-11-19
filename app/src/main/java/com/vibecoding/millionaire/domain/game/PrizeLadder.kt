package com.vibecoding.millionaire.domain.game

/**
 * The 15-step prize ladder (FR-2.2). All amounts are virtual hryvnias.
 */
object PrizeLadder {
    const val QUESTIONS_COUNT = 15

    /** How many difficulty levels the question bank uses (difficulties 1..5). */
    const val DIFFICULTY_LEVELS = 5

    /** How many questions of one difficulty level make up a game. */
    const val QUESTIONS_PER_DIFFICULTY = QUESTIONS_COUNT / DIFFICULTY_LEVELS

    val prizes: List<Int> = listOf(1, 2, 3, 5, 10, 15, 25, 50, 75, 100, 150, 250, 400, 600, 1000)

    /** 0-based indices of the non-spammable ("неспалима") amounts: 5th and 10th questions. */
    val safeHavenIndices: Set<Int> = setOf(4, 9)

    /** Difficulty level (1..5) of the question with 0-based [questionIndex] (FR-2.3). */
    fun difficultyFor(questionIndex: Int): Int = questionIndex / QUESTIONS_PER_DIFFICULTY + 1

    /** Prize for the question with 0-based [questionIndex]. */
    fun prizeFor(questionIndex: Int): Int = prizes[questionIndex]

    /** Prize after [correctAnswers] correct answers when the player walks away (FR-2.7). */
    fun walkAwayPrize(correctAnswers: Int): Int =
        if (correctAnswers <= 0) 0 else prizes[(correctAnswers - 1).coerceAtMost(prizes.lastIndex)]

    /** Prize guaranteed after a wrong answer on the question with 0-based [questionIndex]. */
    fun wrongAnswerPrize(questionIndex: Int): Int =
        safeHavenIndices.filter { it < questionIndex }.maxOrNull()?.let { prizes[it] } ?: 0

    /** Is the question with 0-based [questionIndex] a safe haven? */
    fun isSafeHaven(questionIndex: Int): Boolean = questionIndex in safeHavenIndices
}