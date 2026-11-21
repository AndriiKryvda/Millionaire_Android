package com.vibecoding.millionaire.platform

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import com.vibecoding.millionaire.data.local.AppDatabase

/**
 * Everything the shared object graph needs from the host platform. Implemented once per platform
 * (`AndroidPlatformContext`, `IosPlatformContext`) so the shared code never touches
 * `android.content.Context` or platform frameworks directly.
 */
interface PlatformContext {
    /** Marketing version shown in the menu and in "About". */
    val appVersion: String

    /**
     * Room is built per platform: the Android builder needs a `Context`, the native one needs a
     * bundled SQLite driver. The `@Database` class itself stays in `commonMain`.
     */
    fun createDatabase(): AppDatabase

    fun settingsDataStore(): DataStore<Preferences>

    fun createFeedback(): PlatformFeedback
}
