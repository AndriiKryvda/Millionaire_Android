package com.vibecoding.millionaire.util

import javax.inject.Inject
import javax.inject.Singleton

/** Injectable time source so that domain/data code stays free of wall-clock calls in tests. */
interface Clock {
    fun now(): Long
}

@Singleton
class SystemClock @Inject constructor() : Clock {
    override fun now(): Long = System.currentTimeMillis()
}