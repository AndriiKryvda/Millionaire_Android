package com.vibecoding.millionaire.data.question

import com.vibecoding.millionaire.domain.model.AgeGroup
import com.vibecoding.millionaire.domain.model.AnswerOption
import com.vibecoding.millionaire.domain.model.QuestionBank
import kotlinx.serialization.json.Json
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

/**
 * Guards the shipped asset bank: the JSON files in `app/src/main/assets/questions` must parse with
 * zero skipped entries and must cover every age group / difficulty cell (FR-7.2, FR-7.3).
 */
class QuestionBankIntegrityTest {

    private val json = Json { ignoreUnknownKeys = true }

    private val parsed: ParsedBank by lazy { QuestionBankParser.parse(rawBank()) }
    private val bank: QuestionBank by lazy { QuestionBank(parsed.questions) }

    @Test
    fun `the shipped bank parses without a single skipped question`() {
        assertEquals(emptyList<String>(), parsed.skipped)
        assertEquals(8000, parsed.questions.size)
    }

    @Test
    fun `question ids are unique`() {
        assertEquals(parsed.questions.size, parsed.questions.map { it.id }.distinct().size)
    }

    @Test
    fun `every question has four distinct options and the correct one among them`() {
        for (question in parsed.questions) {
            assertEquals(4, question.options.size)
            assertEquals(4, question.options.values.map { it.trim().lowercase() }.distinct().size)
            assertTrue(question.options.values.all { it.isNotBlank() })
            assertTrue(question.correct in question.options)
            assertTrue(question.difficultyByAge.isNotEmpty())
        }
    }

    @Test
    fun `every age group and difficulty cell has at least fifty questions`() {
        for (age in AgeGroup.entries) {
            for (difficulty in 1..5) {
                val size = bank.pool(age, difficulty).size
                assertTrue("$age / difficulty $difficulty has only $size questions", size >= 50)
            }
        }
    }

    @Test
    fun `every age group has enough questions for a full ladder`() {
        for (age in AgeGroup.entries) {
            assertTrue("$age has ${bank.countFor(age)} questions", bank.countFor(age) >= 250)
        }
    }

    @Test
    fun `correct answer letters are spread evenly`() {
        val counts = AnswerOption.entries.associateWith { option -> parsed.questions.count { it.correct == option } }
        for ((option, count) in counts) {
            assertTrue("$option appears $count times", count in 1800..2200)
        }
    }

    @Test
    fun `question texts respect the length limits and end with a mark`() {
        for (question in parsed.questions) {
            assertTrue(question.text.length in 8..200)
            assertTrue(question.text.endsWith("?") || question.text.endsWith(":"))
        }
    }

    private fun rawBank(): RawQuestions {
        val dir = listOf("src/main/assets/questions", "app/src/main/assets/questions")
            .map(::File)
            .firstOrNull { File(it, QUESTIONS_FILE).exists() }
            ?: error("Question assets not found - run the test from the :app module directory")

        return RawQuestions(
            questions = json.decodeFromString<QuestionsFile>(File(dir, QUESTIONS_FILE).readText(Charsets.UTF_8)).questions,
            answers = json.decodeFromString<AnswersFile>(File(dir, ANSWERS_FILE).readText(Charsets.UTF_8)).answers,
        )
    }

    private companion object {
        const val QUESTIONS_FILE = "questions.json"
        const val ANSWERS_FILE = "answers.json"
    }
}