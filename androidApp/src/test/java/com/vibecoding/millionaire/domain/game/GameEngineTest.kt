package com.vibecoding.millionaire.domain.game

import com.vibecoding.millionaire.domain.model.AgeGroup
import com.vibecoding.millionaire.domain.model.AnswerOption
import com.vibecoding.millionaire.domain.model.GameOutcome
import com.vibecoding.millionaire.domain.model.Lifeline
import com.vibecoding.millionaire.fifteenQuestions
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.random.Random

class GameEngineTest {

    private val engine = GameEngine(LifelineEngine())
    private val age = AgeGroup.AGE_13_14

    private fun newGame() = engine.start(age, fifteenQuestions(), startedAt = 1_000L, random = Random(1))

    private fun correctOf(state: com.vibecoding.millionaire.domain.game.GameState) = state.questions[state.currentIndex].correct

    private fun wrongOf(state: com.vibecoding.millionaire.domain.game.GameState) =
        AnswerOption.entries.first { it != correctOf(state) }

    @Test
    fun `a new game starts on the first question with all four options`() {
        val game = newGame()
        assertEquals(0, game.currentIndex)
        assertEquals(Phase.AWAITING_ANSWER, game.phase)
        assertEquals(4, game.displayOrder.size)
        assertEquals(AnswerOption.entries.toSet(), game.displayOrder.toSet())
        assertEquals(0, game.correctAnswers)
    }

    @Test
    fun `selecting an option never evaluates the answer`() {
        val game = newGame()
        val updated = engine.select(game, correctOf(game))
        assertEquals(Phase.CONFIRMING, updated.phase)
        assertEquals(0, updated.correctAnswers)
    }

    @Test
    fun `confirming without a selection does nothing`() {
        val game = newGame()
        assertEquals(Phase.AWAITING_ANSWER, engine.confirm(game).phase)
    }

    @Test
    fun `selecting the same option again clears the selection`() {
        val game = newGame()
        val option = AnswerOption.entries.first { it !in game.hiddenOptions }
        val selected = engine.select(game, option)
        assertEquals(Phase.CONFIRMING, selected.phase)
        assertEquals(Phase.AWAITING_ANSWER, engine.select(selected, option).phase)
        assertNull(engine.select(selected, option).selected)
    }

    @Test
    fun `correct answer is revealed after the suspense and moves on`() {
        val game = newGame()
        val confirming = engine.select(game, correctOf(game))
        val revealing = engine.confirm(confirming)
        assertEquals(Phase.REVEALING, revealing.phase)

        val revealed = engine.completeReveal(revealing)
        assertEquals(Phase.ANSWER_CORRECT, revealed.phase)
        assertEquals(1, revealed.correctAnswers)

        val next = engine.advance(revealed)
        assertEquals(1, next.currentIndex)
        assertEquals(Phase.AWAITING_ANSWER, next.phase)
        assertNull(next.selected)
        assertEquals(4, next.displayOrder.size)
    }

    @Test
    fun `wrong answer on the seventh question keeps the fifth prize`() {
        val game = newGame().copy(currentIndex = 6, correctAnswers = 6)
        val revealed = engine.completeReveal(engine.confirm(engine.select(game, wrongOf(game))))
        assertEquals(Phase.ANSWER_WRONG, revealed.phase)
        assertEquals(6, revealed.correctAnswers)

        val finished = engine.finishWithWrongAnswer(revealed)
        assertEquals(GameOutcome.WRONG_ANSWER, finished.outcome)
        assertEquals(Phase.FINISHED, finished.phase)
        assertEquals(10, finished.prize)
    }

    @Test
    fun `wrong answer before the first safe haven pays nothing`() {
        val game = newGame().copy(currentIndex = 2, correctAnswers = 2)
        val finished = engine.finishWithWrongAnswer(engine.completeReveal(engine.confirm(engine.select(game, wrongOf(game)))))
        assertEquals(0, finished.prize)
    }

    @Test
    fun `wrong answer after the second safe haven pays one hundred`() {
        val game = newGame().copy(currentIndex = 10, correctAnswers = 10)
        val finished = engine.finishWithWrongAnswer(engine.completeReveal(engine.confirm(engine.select(game, wrongOf(game)))))
        assertEquals(100, finished.prize)
    }

    @Test
    fun `walking away keeps the prize of the last answered question`() {
        assertEquals(0, engine.walkAway(newGame()).prize)
        val game = newGame().copy(correctAnswers = 3)
        val walked = engine.walkAway(game)
        assertEquals(GameOutcome.WALKED_AWAY, walked.outcome)
        assertEquals(3, walked.prize)
    }

    @Test
    fun `answering the fifteenth question wins the thousand`() {
        val game = newGame().copy(currentIndex = 14, correctAnswers = 14, phase = Phase.ANSWER_CORRECT)
        val finished = engine.advance(game)
        assertEquals(Phase.FINISHED, finished.phase)
        assertEquals(GameOutcome.WON, finished.outcome)
        assertEquals(1000, finished.prize)
        assertEquals(14, finished.correctAnswers)
    }

    @Test
    fun `fifty fifty hides two wrong options and can be used once`() {
        val game = newGame()
        val used = engine.useFiftyFifty(game, Random(3))
        assertEquals(2, used.hiddenOptions.size)
        assertFalse(correctOf(game) in used.hiddenOptions)
        assertEquals(setOf(Lifeline.FIFTY_FIFTY), used.usedLifelines)
        assertEquals(2, engine.visibleOptions(used).size)

        val second = engine.useFiftyFifty(used, Random(4))
        assertEquals(2, second.hiddenOptions.size)
    }

    @Test
    fun `hidden options cannot be selected`() {
        val used = engine.useFiftyFifty(newGame(), Random(5))
        val hidden = used.hiddenOptions.first()
        assertEquals(Phase.AWAITING_ANSWER, engine.select(used, hidden).phase)
    }

    @Test
    fun `lifelines are unavailable once the answer is being revealed`() {
        val revealing = engine.confirm(engine.select(newGame(), AnswerOption.A))
        assertEquals(emptySet<AnswerOption>(), engine.useFiftyFifty(revealing, Random(6)).hiddenOptions)
        assertNull(engine.useAudience(revealing, Random(6)).audiencePercentages)
        assertNull(engine.usePhoneFriend(revealing, Random(6)).friendHint)
    }

    @Test
    fun `audience result covers the visible options and sums to one hundred`() {
        val used = engine.useAudience(newGame(), Random(9))
        val percentages = requireNotNull(used.audiencePercentages)
        assertEquals(4, percentages.size)
        assertEquals(100, percentages.values.sum())
        assertEquals(setOf(Lifeline.ASK_AUDIENCE), used.usedLifelines)
    }

    @Test
    fun `phone friend hint is cleared when the next question starts`() {
        val used = engine.usePhoneFriend(newGame(), Random(10))
        requireNotNull(used.friendHint)
        val answered = engine.completeReveal(engine.confirm(engine.select(used, correctOf(used))))
        val next = engine.advance(answered)
        assertNull(next.friendHint)
        assertNull(next.audiencePercentages)
        assertTrue(next.usedLifelines.contains(Lifeline.PHONE_FRIEND))
    }
}