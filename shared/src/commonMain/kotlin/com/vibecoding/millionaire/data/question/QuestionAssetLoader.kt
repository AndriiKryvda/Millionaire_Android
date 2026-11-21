package com.vibecoding.millionaire.data.question

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import com.vibecoding.millionaire.generated.resources.Res

/**
 * Reads the question JSON files from the Compose resources with an explicit UTF-8 charset
 * (TECHNICAL_DESIGN 3.1). Parsing errors are propagated to the caller, which shows a
 * readable error screen.
 */
class QuestionAssetLoader {

    private val json = Json {
        ignoreUnknownKeys = true
        coerceInputValues = true
    }

    suspend fun loadRaw(): RawQuestions = withContext(Dispatchers.Default) {
        val questions = json.decodeFromString<QuestionsFile>(read(QUESTIONS_FILE)).questions
        val answers = json.decodeFromString<AnswersFile>(read(ANSWERS_FILE)).answers
        RawQuestions(questions, answers)
    }

    private suspend fun read(path: String): String =
        Res.readBytes(path).decodeToString()

    companion object {
        const val QUESTIONS_FILE = "files/questions/questions.json"
        const val ANSWERS_FILE = "files/questions/answers.json"
    }
}
