package com.vibecoding.millionaire.domain.model

/**
 * In-memory, immutable view of the question bank with a `age x difficulty` index.
 */
class QuestionBank(val all: List<Question>) {

    private val byId: Map<Int, Question> = all.associateBy { it.id }

    private val index: Map<AgeGroup, Map<Int, List<Question>>> = buildIndex(all)

    val size: Int get() = all.size

    fun byId(id: Int): Question? = byId[id]

    fun pool(age: AgeGroup, difficulty: Int): List<Question> = index[age]?.get(difficulty).orEmpty()

    /** How many questions are rated for [age] at all. */
    fun countFor(age: AgeGroup): Int = index[age]?.values?.sumOf { it.size } ?: 0

    /** How many questions rated for [age] the player has not seen yet (FR-3.4). */
    fun availableFor(age: AgeGroup, seenIds: Set<Int>): Int =
        all.count { it.difficultyByAge.containsKey(age) && it.id !in seenIds }

    /** How many not-yet-seen questions exist for a given cell of the matrix. */
    fun availablePool(age: AgeGroup, difficulty: Int, seenIds: Set<Int>): List<Question> =
        pool(age, difficulty).filter { it.id !in seenIds }

    private fun buildIndex(questions: List<Question>): Map<AgeGroup, Map<Int, List<Question>>> {
        val result = HashMap<AgeGroup, HashMap<Int, MutableList<Question>>>()
        for (question in questions) {
            for ((age, difficulty) in question.difficultyByAge) {
                result.getOrPut(age) { HashMap() }
                    .getOrPut(difficulty) { ArrayList() }
                    .add(question)
            }
        }
        return result.mapValues { (_, byDifficulty) -> byDifficulty.mapValues { it.value.toList() } }
    }
}