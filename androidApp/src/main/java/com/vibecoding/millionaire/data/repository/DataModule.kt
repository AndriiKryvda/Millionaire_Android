package com.vibecoding.millionaire.data.repository

import com.vibecoding.millionaire.data.question.QuestionBankRepositoryImpl
import com.vibecoding.millionaire.domain.repository.GameResultRepository
import com.vibecoding.millionaire.domain.repository.PlayerRepository
import com.vibecoding.millionaire.domain.repository.QuestionBankRepository
import com.vibecoding.millionaire.domain.repository.SettingsRepository
import com.vibecoding.millionaire.util.Clock
import com.vibecoding.millionaire.util.SystemClock
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class RepositoryModule {

    @Binds
    @Singleton
    abstract fun bindQuestionBankRepository(impl: QuestionBankRepositoryImpl): QuestionBankRepository

    @Binds
    @Singleton
    abstract fun bindPlayerRepository(impl: PlayerRepositoryImpl): PlayerRepository

    @Binds
    @Singleton
    abstract fun bindGameResultRepository(impl: GameResultRepositoryImpl): GameResultRepository

    @Binds
    @Singleton
    abstract fun bindSettingsRepository(impl: SettingsRepositoryImpl): SettingsRepository

    @Binds
    @Singleton
    abstract fun bindClock(impl: SystemClock): Clock
}