package com.vibecoding.millionaire.data.question

import android.content.Context
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Reads the question JSON files from `assets` with an explicit UTF-8 charset
 * (TECHNICAL_DESIGN 3.1). Parsing errors are propagated to the caller, which shows a
 * readable error screen.
 */
@Singleton
class QuestionAssetLoader @Inject constructor(
    @ApplicationContext private val context: Context,
) {
    private val json = Json {
        ignoreUnknownKeys = true
        coerceInputValues = true
    }

    suspend fun loadRaw(): RawQuestions = withContext(Dispatchers.IO) {
        val questions = json.decodeFromString<QuestionsFile>(read(QUESTIONS_FILE)).questions
        val answers = json.decodeFromString<AnswersFile>(read(ANSWERS_FILE)).answers
        RawQuestions(questions, answers)
    }

    private fun read(path: String): String =
        context.assets.open(path).bufferedReader(Charsets.UTF_8).use { it.readText() }

    companion object {
        const val QUESTIONS_FILE = "questions/questions.json"
        const val ANSWERS_FILE = "questions/answers.json"
    }
}