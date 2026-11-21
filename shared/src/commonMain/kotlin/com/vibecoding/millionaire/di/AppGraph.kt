package com.vibecoding.millionaire.di

import com.vibecoding.millionaire.data.local.AppDatabase
import com.vibecoding.millionaire.data.question.QuestionAssetLoader
import com.vibecoding.millionaire.data.question.QuestionBankRepositoryImpl
import com.vibecoding.millionaire.data.repository.GameResultRepositoryImpl
import com.vibecoding.millionaire.data.repository.PlayerRepositoryImpl
import com.vibecoding.millionaire.data.repository.SettingsRepositoryImpl
import com.vibecoding.millionaire.domain.game.GameEngine
import com.vibecoding.millionaire.domain.game.LifelineEngine
import com.vibecoding.millionaire.domain.game.QuestionSelector
import com.vibecoding.millionaire.domain.repository.GameResultRepository
import com.vibecoding.millionaire.domain.repository.PlayerRepository
import com.vibecoding.millionaire.domain.repository.QuestionBankRepository
import com.vibecoding.millionaire.domain.repository.SettingsRepository
import com.vibecoding.millionaire.domain.usecase.CheckGameAvailabilityUseCase
import com.vibecoding.millionaire.domain.usecase.ObserveTopResultsUseCase
import com.vibecoding.millionaire.domain.usecase.ResetGameHistoryUseCase
import com.vibecoding.millionaire.domain.usecase.SaveGameResultUseCase
import com.vibecoding.millionaire.domain.usecase.StartGameUseCase
import com.vibecoding.millionaire.platform.PlatformContext
import com.vibecoding.millionaire.ui.feedback.FeedbackManager
import com.vibecoding.millionaire.util.Clock
import com.vibecoding.millionaire.util.SystemClock
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob

/**
 * Hand-written composition root; it replaces the previous Hilt graph so that exactly the same
 * object graph can be built from Android, iOS and tests without an annotation processor.
 *
 * Everything is lazy: opening the database must not happen while the app class is being created.
 */
class AppGraph(val platform: PlatformContext) {

    val scope: CoroutineScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    val clock: Clock = SystemClock()

    /** Marketing version, shown in the menu and in "About" (was `BuildConfig.VERSION_NAME`). */
    val appVersion: String get() = platform.appVersion

    private val database: AppDatabase by lazy { platform.createDatabase() }

    val questionBankRepository: QuestionBankRepository by lazy {
        QuestionBankRepositoryImpl(QuestionAssetLoader())
    }

    val playerRepository: PlayerRepository by lazy {
        PlayerRepositoryImpl(database.playerDao(), database.seenQuestionDao(), clock)
    }

    val gameResultRepository: GameResultRepository by lazy {
        GameResultRepositoryImpl(database.gameResultDao(), database.playerDao(), database.seenQuestionDao())
    }

    val settingsRepository: SettingsRepository by lazy { SettingsRepositoryImpl(platform.settingsDataStore()) }

    val feedback: FeedbackManager by lazy {
        FeedbackManager(platform.createFeedback(), settingsRepository, scope)
    }

    private val questionSelector: QuestionSelector by lazy { QuestionSelector() }

    val gameEngine: GameEngine by lazy { GameEngine(LifelineEngine()) }

    val startGame: StartGameUseCase by lazy {
        StartGameUseCase(playerRepository, questionBankRepository, questionSelector)
    }

    val checkGameAvailability: CheckGameAvailabilityUseCase by lazy {
        CheckGameAvailabilityUseCase(playerRepository, questionBankRepository, questionSelector)
    }

    val saveGameResult: SaveGameResultUseCase by lazy { SaveGameResultUseCase(gameResultRepository) }

    val observeTopResults: ObserveTopResultsUseCase by lazy { ObserveTopResultsUseCase(gameResultRepository) }

    val resetGameHistory: ResetGameHistoryUseCase by lazy { ResetGameHistoryUseCase(playerRepository) }
}
