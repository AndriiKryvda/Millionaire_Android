package com.vibecoding.millionaire.data.question

import com.vibecoding.millionaire.domain.model.AgeGroup
import com.vibecoding.millionaire.domain.model.AnswerOption
import com.vibecoding.millionaire.domain.model.Question

data class ParsedBank(val questions: List<Question>, val skipped: List<String>) {
    val size: Int get() = questions.size
}

/**
 * Pure mapping from the raw JSON DTOs to the domain model, with the validation rules of
 * TECHNICAL_DESIGN 3.1. Broken entries are reported in [ParsedBank.skipped] instead of throwing,
 * so one bad question can never crash the game - and the rules can be unit tested on the JVM.
 */
object QuestionBankParser {

    const val OPTION_COUNT = 4
    const val QUESTION_TEXT_MIN = 8
    const val QUESTION_TEXT_MAX = 200
    const val DIFFICULTY_MIN = 1
    const val DIFFICULTY_MAX = 5

    fun parse(raw: RawQuestions): ParsedBank {
        val answerByKey = HashMap<Int, String>(raw.answers.size)
        val skipped = ArrayList<String>()

        for (answer in raw.answers) {
            if (answerByKey.put(answer.id, answer.correct) != null) {
                skipped += "question ${answer.id}: duplicate answer"
            }
        }

        val questions = ArrayList<Question>(raw.questions.size)
        val seenIds = HashSet<Int>(raw.questions.size)
        for (dto in raw.questions) {
            if (!seenIds.add(dto.id)) {
                skipped += "question ${dto.id}: duplicate id"
                continue
            }
            try {
                questions += toQuestion(dto, answerByKey[dto.id])
            } catch (e: IllegalArgumentException) {
                skipped += "question ${dto.id}: ${e.message}"
            }
        }
        return ParsedBank(questions, skipped)
    }

    private fun toQuestion(dto: QuestionDto, answerKey: String?): Question {
        val correct = answerKey?.let(AnswerOption::fromKey)
            ?: throw IllegalArgumentException("missing or invalid correct answer key '$answerKey'")
        require(dto.options.size == OPTION_COUNT) { "expected $OPTION_COUNT options, got ${dto.options.size}" }

        val options = LinkedHashMap<AnswerOption, String>(OPTION_COUNT)
        for ((key, value) in dto.options) {
            val option = AnswerOption.fromKey(key) ?: throw IllegalArgumentException("unknown option key '$key'")
            require(option !in options) { "duplicated option key '$key'" }
            require(value.isNotBlank()) { "blank option text for '$key'" }
            options[option] = value.trim()
        }
        require(correct in options) { "correct answer '$correct' is not among the options" }

        val text = dto.text.trim()
        require(text.length in QUESTION_TEXT_MIN..QUESTION_TEXT_MAX) { "question text length ${text.length}" }
        require(text.endsWith("?") || text.endsWith(":")) { "question text must end with '?' or ':'" }
        require(dto.category.isNotBlank()) { "blank category" }

        val ratings = HashMap<AgeGroup, Int>(AgeGroup.entries.size)
        for (rating in dto.ratings) {
            val age = AgeGroup.fromJsonKey(rating.age)
                ?: throw IllegalArgumentException("unknown age group '${rating.age}'")
            require(age !in ratings) { "duplicated rating for age ${rating.age}" }
            require(rating.difficulty in DIFFICULTY_MIN..DIFFICULTY_MAX) {
                "difficulty ${rating.difficulty} for age ${rating.age}"
            }
            ratings[age] = rating.difficulty
        }
        require(ratings.isNotEmpty()) { "no ratings" }

        return Question(
            id = dto.id,
            text = text,
            options = options,
            category = dto.category.trim(),
            difficultyByAge = ratings,
            correct = correct,
        )
    }
}