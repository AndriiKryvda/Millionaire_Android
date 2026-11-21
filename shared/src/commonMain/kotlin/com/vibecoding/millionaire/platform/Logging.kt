package com.vibecoding.millionaire.platform

/**
 * Writes a diagnostic line to the platform log. Android uses logcat, iOS uses NSLog.
 */
expect fun logInfo(tag: String, message: String)

expect fun logWarn(tag: String, message: String)
