package com.vibecoding.millionaire.data.local

import androidx.room.Database
import androidx.room.RoomDatabase

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
