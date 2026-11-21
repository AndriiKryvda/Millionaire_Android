package com.vibecoding.millionaire.platform

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.core.Preferences
import androidx.room.Room
import com.vibecoding.millionaire.data.local.AppDatabase
import okio.Path.Companion.toPath
import java.io.File

/** Android side of the shared object graph. */
class AndroidPlatformContext(context: Context) : PlatformContext {

    private val appContext = context.applicationContext

    @Volatile
    private var cachedDataStore: DataStore<Preferences>? = null

    override val appVersion: String
        get() = runCatching {
            appContext.packageManager.getPackageInfo(appContext.packageName, 0).versionName
        }.getOrNull() ?: UNKNOWN_VERSION

    override fun createDatabase(): AppDatabase =
        Room.databaseBuilder(appContext, AppDatabase::class.java, AppDatabase.NAME)
            .fallbackToDestructiveMigration(dropAllTables = true)
            .build()

    override fun settingsDataStore(): DataStore<Preferences> = cachedDataStore ?: synchronized(this) {
        cachedDataStore ?: PreferenceDataStoreFactory.createWithPath(
            produceFile = { settingsFile().absolutePath.toPath() },
        ).also { cachedDataStore = it }
    }

    override fun createFeedback(): PlatformFeedback = AndroidFeedback(appContext)

    /**
     * Exactly the file the previous `preferencesDataStore(name = "settings")` delegate used, so
     * existing installations keep their sound/vibration settings after the migration.
     */
    private fun settingsFile(): File =
        File(appContext.filesDir, "datastore").resolve("settings.preferences_pb")

    private companion object {
        const val UNKNOWN_VERSION = "0.0.0"
    }
}
