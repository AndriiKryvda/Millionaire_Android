package com.vibecoding.millionaire

import com.vibecoding.millionaire.domain.model.AgeGroup
import com.vibecoding.millionaire.domain.model.AnswerOption
import com.vibecoding.millionaire.domain.model.Question
import com.vibecoding.millionaire.domain.model.QuestionBank

/** Builds a question that satisfies all parser rules. */
fun question(
    id: Int,
    difficulty: Int,
    correct: AnswerOption = AnswerOption.A,
    ages: List<AgeGroup> = AgeGroup.entries,
): Question = Question(
    id = id,
    text = "Це тестове питання номер $id?",
    options = AnswerOption.entries.associateWith { option -> "Варіант ${option.name} до питання $id" },
    category = "Тест",
    difficultyByAge = ages.associateWith { difficulty },
    correct = correct,
)

/** A bank with [perDifficulty] questions per difficulty level for a single age group. */
fun bankWith(
    perDifficulty: Int,
    age: AgeGroup = AgeGroup.AGE_13_14,
    levels: IntRange = 1..5,
): QuestionBank {
    val questions = ArrayList<Question>()
    var id = 1
    for (level in levels) {
        repeat(perDifficulty) { questions += question(id++, level, ages = listOf(age)) }
    }
    return QuestionBank(questions)
}

/** The 15 questions of one game: 3 per difficulty, cycling correct letters. */
fun fifteenQuestions(): List<Question> = (1..15).map { index ->
    question(
        id = index,
        difficulty = (index - 1) / 3 + 1,
        correct = AnswerOption.entries[(index - 1) % AnswerOption.entries.size],
    )
}