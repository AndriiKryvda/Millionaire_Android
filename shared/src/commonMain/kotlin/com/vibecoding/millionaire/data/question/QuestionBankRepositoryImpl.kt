package com.vibecoding.millionaire.data.question

import com.vibecoding.millionaire.platform.logInfo
import com.vibecoding.millionaire.platform.logWarn

import com.vibecoding.millionaire.domain.model.QuestionBank
import com.vibecoding.millionaire.domain.repository.QuestionBankRepository
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

/**
 * Loads, validates and indexes the bundled bank once per process (FR-7.1).
 * Invalid entries are skipped with a warning instead of crashing the game.
 */
class QuestionBankRepositoryImpl(
    private val loader: QuestionAssetLoader,
) : QuestionBankRepository {

    private val mutex = Mutex()
    private var cached: QuestionBank? = null

    override suspend fun getBank(): QuestionBank = mutex.withLock {
        cached ?: run {
            val parsed = QuestionBankParser.parse(loader.loadRaw())
            parsed.skipped.forEach { logWarn(TAG, "Skipping $it") }
            val bank = QuestionBank(parsed.questions)
            check(bank.size > 0) { "Question bank is empty - assets/questions/*.json are damaged" }
            logInfo(TAG, "Question bank ready: ${bank.size} questions, ${parsed.skipped.size} skipped")
            bank.also { cached = it }
        }
    }

    private companion object {
        const val TAG = "QuestionBank"
    }
}