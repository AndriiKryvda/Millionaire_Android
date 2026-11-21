package com.vibecoding.millionaire.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Database(
    entities = [PlayerEntity::class, GameResultEntity::class, SeenQuestionEntity::class],
    version = 1,
    exportSchema = false,
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun playerDao(): PlayerDao
    abstract fun gameResultDao(): GameResultDao
    abstract fun seenQuestionDao(): SeenQuestionDao

    companion object {
        const val NAME = "millionaire.db"
    }
}

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    @Provides
    @Singleton
    fun provideDatabase(@ApplicationContext context: Context): AppDatabase =
        Room.databaseBuilder(context, AppDatabase::class.java, AppDatabase.NAME)
            .fallbackToDestructiveMigration(dropAllTables = true)
            .build()

    @Provides fun providePlayerDao(db: AppDatabase): PlayerDao = db.playerDao()

    @Provides fun provideGameResultDao(db: AppDatabase): GameResultDao = db.gameResultDao()

    @Provides fun provideSeenQuestionDao(db: AppDatabase): SeenQuestionDao = db.seenQuestionDao()
}