package com.vibecoding.millionaire.domain.game

import com.vibecoding.millionaire.domain.model.AgeGroup
import com.vibecoding.millionaire.domain.model.AnswerOption
import com.vibecoding.millionaire.domain.model.GameOutcome
import com.vibecoding.millionaire.domain.model.Lifeline
import com.vibecoding.millionaire.domain.model.Question
import kotlin.random.Random

/** Everything a screen needs to render one game. Immutable; every step returns a new copy. */
data class GameState(
    val ageGroup: AgeGroup,
    val questions: List<Question>,
    val startedAt: Long,
    val currentIndex: Int = 0,
    /** Shuffled display order of the option letters of the current question (FR-2.5). */
    val displayOrder: List<AnswerOption> = emptyList(),
    val hiddenOptions: Set<AnswerOption> = emptySet(),
    val selected: AnswerOption? = null,
    val phase: Phase = Phase.AWAITING_ANSWER,
    val usedLifelines: Set<Lifeline> = emptySet(),
    val audiencePercentages: Map<AnswerOption, Int>? = null,
    val friendHint: FriendHint? = null,
    val correctAnswers: Int = 0,
    val outcome: GameOutcome? = null,
    val prize: Int = 0,
)

enum class Phase { AWAITING_ANSWER, CONFIRMING, REVEALING, ANSWER_CORRECT, ANSWER_WRONG, FINISHED }

/**
 * The single source of truth for game rules. Pure functions only: no Android, no coroutines,
 * no wall-clock time (NFR-9).
 */
class GameEngine(
    private val lifelines: LifelineEngine,
) {

    fun start(ageGroup: AgeGroup, questions: List<Question>, startedAt: Long, random: Random = Random.Default): GameState {
        require(questions.isNotEmpty()) { "A game needs at least one question" }
        return GameState(
            ageGroup = ageGroup,
            questions = questions,
            startedAt = startedAt,
            displayOrder = questions[0].options.keys.shuffled(random),
        )
    }

    fun currentQuestion(state: GameState): Question = state.questions[state.currentIndex]

    fun visibleOptions(state: GameState): List<AnswerOption> =
        state.displayOrder.filter { it !in state.hiddenOptions }

    fun difficultyOf(state: GameState): Int = currentQuestion(state).difficultyFor(state.ageGroup)

    fun prizeForCurrentQuestion(state: GameState): Int = PrizeLadder.prizeFor(state.currentIndex)

    fun walkAwayPrize(state: GameState): Int = PrizeLadder.walkAwayPrize(state.correctAnswers)

    fun safeHavenPrize(state: GameState): Int = PrizeLadder.wrongAnswerPrize(state.currentIndex)

    /** FR-2.4: selecting never evaluates the answer. Tapping the selected option again clears it. */
    fun select(state: GameState, option: AnswerOption): GameState {
        if (state.phase != Phase.AWAITING_ANSWER && state.phase != Phase.CONFIRMING) return state
        if (option in state.hiddenOptions) return state
        if (option !in currentQuestion(state).options) return state
        if (state.selected == option) return state.copy(selected = null, phase = Phase.AWAITING_ANSWER)
        return state.copy(selected = option, phase = Phase.CONFIRMING)
    }

    fun cancelSelection(state: GameState): GameState =
        if (state.phase == Phase.CONFIRMING) state.copy(selected = null, phase = Phase.AWAITING_ANSWER) else state

    /** Called when the player presses "Підтвердити" - starts the suspense phase. */
    fun confirm(state: GameState): GameState =
        if (state.phase == Phase.CONFIRMING && state.selected != null) {
            state.copy(phase = Phase.REVEALING)
        } else {
            state
        }

    /** Called after the suspense delay: the answer becomes known (FR-2.6). */
    fun completeReveal(state: GameState): GameState {
        if (state.phase != Phase.REVEALING || state.selected == null) return state
        val question = currentQuestion(state)
        return if (state.selected == question.correct) {
            state.copy(phase = Phase.ANSWER_CORRECT, correctAnswers = state.correctAnswers + 1)
        } else {
            state.copy(phase = Phase.ANSWER_WRONG)
        }
    }

    /** Moves to the next question after a correct answer; ends the game after the 15th (FR-2.8). */
    fun advance(state: GameState, random: Random = Random.Default): GameState {
        if (state.phase != Phase.ANSWER_CORRECT) return state
        val next = state.currentIndex + 1
        if (next >= PrizeLadder.QUESTIONS_COUNT) {
            return state.copy(
                phase = Phase.FINISHED,
                outcome = GameOutcome.WON,
                prize = PrizeLadder.prizes.last(),
            )
        }
        return state.copy(
            currentIndex = next,
            displayOrder = state.questions[next].options.keys.shuffled(random),
            hiddenOptions = emptySet(),
            selected = null,
            audiencePercentages = null,
            friendHint = null,
            phase = Phase.AWAITING_ANSWER,
        )
    }

    /** Ends the game with the last safe haven prize (FR-2.6). */
    fun finishWithWrongAnswer(state: GameState): GameState {
        if (state.phase != Phase.ANSWER_WRONG) return state
        return state.copy(
            phase = Phase.FINISHED,
            outcome = GameOutcome.WRONG_ANSWER,
            prize = safeHavenPrize(state),
        )
    }

    /** FR-2.7: walk away keeps the prize of the last answered question. */
    fun walkAway(state: GameState): GameState {
        if (state.phase != Phase.AWAITING_ANSWER && state.phase != Phase.CONFIRMING) return state
        return state.copy(
            phase = Phase.FINISHED,
            outcome = GameOutcome.WALKED_AWAY,
            prize = walkAwayPrize(state),
        )
    }

    fun useFiftyFifty(state: GameState, random: Random = Random.Default): GameState {
        if (!canUseLifeline(state, Lifeline.FIFTY_FIFTY)) return state
        val hidden = lifelines.fiftyFifty(visibleOptions(state), currentQuestion(state).correct, random)
        return state.copy(
            hiddenOptions = state.hiddenOptions + hidden,
            usedLifelines = state.usedLifelines + Lifeline.FIFTY_FIFTY,
        )
    }

    fun useAudience(state: GameState, random: Random = Random.Default): GameState {
        if (!canUseLifeline(state, Lifeline.ASK_AUDIENCE)) return state
        return state.copy(
            audiencePercentages = lifelines.audience(
                visible = visibleOptions(state),
                correct = currentQuestion(state).correct,
                difficulty = difficultyOf(state),
                random = random,
            ),
            usedLifelines = state.usedLifelines + Lifeline.ASK_AUDIENCE,
        )
    }

    fun usePhoneFriend(state: GameState, random: Random = Random.Default): GameState {
        if (!canUseLifeline(state, Lifeline.PHONE_FRIEND)) return state
        return state.copy(
            friendHint = lifelines.phoneFriend(
                visible = visibleOptions(state),
                correct = currentQuestion(state).correct,
                difficulty = difficultyOf(state),
                random = random,
            ),
            usedLifelines = state.usedLifelines + Lifeline.PHONE_FRIEND,
        )
    }

    fun canUseLifeline(state: GameState, lifeline: Lifeline): Boolean =
        state.phase == Phase.AWAITING_ANSWER && lifeline !in state.usedLifelines

    fun isFinished(state: GameState): Boolean = state.phase == Phase.FINISHED
}