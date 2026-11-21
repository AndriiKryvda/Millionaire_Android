package com.vibecoding.millionaire.util

import kotlin.time.Clock as TimeSourceClock
import kotlin.time.ExperimentalTime

/** Injectable time source so that domain/data code stays free of wall-clock calls in tests. */
interface Clock {
    fun now(): Long
}

class SystemClock : Clock {
    @OptIn(ExperimentalTime::class)
    override fun now(): Long = TimeSourceClock.System.now().toEpochMilliseconds()
}
