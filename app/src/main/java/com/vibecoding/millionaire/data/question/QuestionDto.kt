package com.vibecoding.millionaire.data.question

import kotlinx.serialization.Serializable

@Serializable
data class QuestionsFile(val questions: List<QuestionDto>)

@Serializable
data class QuestionDto(
    val id: Int,
    val text: String,
    /** Option key ("A".."D") -> option text. */
    val options: Map<String, String>,
    val category: String,
    val ratings: List<RatingDto>,
)

@Serializable
data class RatingDto(val age: String, val difficulty: Int)

@Serializable
data class AnswersFile(val answers: List<AnswerDto>)

@Serializable
data class AnswerDto(val id: Int, val correct: String)

data class RawQuestions(val questions: List<QuestionDto>, val answers: List<AnswerDto>)