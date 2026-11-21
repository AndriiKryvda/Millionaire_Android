package com.vibecoding.millionaire.platform

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.core.Preferences
import androidx.room.Room
import androidx.sqlite.driver.bundled.BundledSQLiteDriver
import com.vibecoding.millionaire.data.local.AppDatabase
import kotlinx.cinterop.ExperimentalForeignApi
import okio.Path
import okio.Path.Companion.toPath
import platform.Foundation.NSBundle
import platform.Foundation.NSDocumentDirectory
import platform.Foundation.NSFileManager
import platform.Foundation.NSUserDomainMask

/**
 * iOS side of the shared object graph.
 *
 * NOTE: this target can only be compiled on a macOS host with Xcode installed.
 */
@OptIn(ExperimentalForeignApi::class)
class IosPlatformContext : PlatformContext {

    @Volatile
    private var cachedDataStore: DataStore<Preferences>? = null

    override val appVersion: String
        get() = NSBundle.mainBundle.objectForInfoDictionaryKey("CFBundleShortVersionString") as? String
            ?: UNKNOWN_VERSION

    override fun createDatabase(): AppDatabase =
        Room.databaseBuilder<AppDatabase>(AppDatabase.NAME)
            .fallbackToDestructiveMigration(dropAllTables = true)
            .setDriver(BundledSQLiteDriver())
            .build()

    override fun settingsDataStore(): DataStore<Preferences> = cachedDataStore ?: synchronized(this) {
        cachedDataStore ?: PreferenceDataStoreFactory.createWithPath(
            produceFile = { documentDirectory() / DATASTORE_FILE },
        ).also { cachedDataStore = it }
    }

    override fun createFeedback(): PlatformFeedback = IosFeedback()

    private fun documentDirectory(): Path {
        val url = NSFileManager.defaultManager.URLForDirectory(
            directory = NSDocumentDirectory,
            inDomain = NSUserDomainMask,
            appropriateForURL = null,
            create = false,
            error = null,
        ) ?: error("The iOS documents directory is unavailable")
        return (requireNotNull(url.path)).toPath()
    }

    private companion object {
        const val DATASTORE_FILE = "settings.preferences_pb"
        const val UNKNOWN_VERSION = "0.0.0"
    }
}
