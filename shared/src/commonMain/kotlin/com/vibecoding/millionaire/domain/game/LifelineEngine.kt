package com.vibecoding.millionaire.domain.game

import com.vibecoding.millionaire.domain.model.AnswerOption
import kotlin.random.Random

/**
 * Probabilistic helpers for the three lifelines (FR-4). All randomness is injected so the
 * behaviour can be tested with a seeded [Random].
 */
class LifelineEngine {

    /** Hides all but two options; the correct answer is always kept (FR-4.1). */
    fun fiftyFifty(
        visible: List<AnswerOption>,
        correct: AnswerOption,
        random: Random = Random.Default,
    ): Set<AnswerOption> {
        val wrong = visible.filter { it != correct }
        if (wrong.size <= 1) return emptySet()
        return wrong.shuffled(random).take(wrong.size - 1).toSet()
    }

    /**
     * Audience vote. The majority follows the correct answer with the accuracy that depends on the
     * difficulty of the question (FR-4.2). Percentages always add up to 100.
     */
    fun audience(
        visible: List<AnswerOption>,
        correct: AnswerOption,
        difficulty: Int,
        random: Random = Random.Default,
    ): Map<AnswerOption, Int> {
        val wrong = visible.filter { it != correct }
        if (wrong.isEmpty()) return mapOf(correct to 100)

        val accuracy = accuracyFor(difficulty)
        val correctPct = if (random.nextDouble() < accuracy) {
            random.nextInt(55, 91)
        } else {
            random.nextInt(5, 26)
        }.coerceAtMost(100)

        val shares = distribute(100 - correctPct, wrong.map { random.nextDouble(0.25, 1.0) })
        val result = LinkedHashMap<AnswerOption, Int>()
        result[correct] = correctPct
        wrong.forEachIndexed { index, option -> result[option] = shares[index] }
        return result
    }

    /** A random friend with a hint whose confidence follows the difficulty (FR-4.4). */
    fun phoneFriend(
        visible: List<AnswerOption>,
        correct: AnswerOption,
        difficulty: Int,
        random: Random = Random.Default,
    ): FriendHint {
        val friend = Friends.all[random.nextInt(Friends.all.size)]
        val knows = random.nextDouble() < accuracyFor(difficulty)
        val answer = if (knows) {
            correct
        } else {
            visible.filter { it != correct }.randomOrNull(random) ?: correct
        }
        val confidence = when {
            knows && answer == correct && difficulty <= 2 -> Confidence.HIGH
            knows && answer == correct -> Confidence.MEDIUM
            else -> Confidence.LOW
        }
        return FriendHint(friend, answer, confidence)
    }

    /** Probability that the lifeline gives the correct answer, by difficulty 1..5. */
    fun accuracyFor(difficulty: Int): Double = when (difficulty) {
        1 -> 0.90
        2 -> 0.80
        3 -> 0.70
        4 -> 0.60
        else -> 0.50
    }

    private fun distribute(total: Int, weights: List<Double>): List<Int> {
        if (weights.isEmpty()) return emptyList()
        if (total <= 0) return List(weights.size) { 0 }
        val sum = weights.sum().takeIf { it > 0 } ?: 1.0
        val raw = weights.map { it / sum * total }
        val result = raw.map { it.toInt() }.toMutableList()
        var rest = total - result.sum()
        val order = raw.indices.sortedByDescending { raw[it] - raw[it].toInt() }
        var i = 0
        while (rest > 0 && order.isNotEmpty()) {
            result[order[i % order.size]] += 1
            rest -= 1
            i += 1
        }
        return result
    }
}

private fun <T> List<T>.randomOrNull(random: Random): T? =
    if (isEmpty()) null else this[random.nextInt(size)]