package com.vibecoding.millionaire

import android.app.Application
import com.vibecoding.millionaire.di.AppGraph
import com.vibecoding.millionaire.platform.AndroidPlatformContext

class MillionaireApp : Application() {

    /** The shared composition root; one instance per process. */
    lateinit var graph: AppGraph
        private set

    override fun onCreate() {
        super.onCreate()
        graph = AppGraph(AndroidPlatformContext(this))
    }
}
