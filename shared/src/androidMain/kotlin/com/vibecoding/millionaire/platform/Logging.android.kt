package com.vibecoding.millionaire.platform

import android.util.Log

actual fun logInfo(tag: String, message: String) {
    Log.i(tag, message)
}

actual fun logWarn(tag: String, message: String) {
    Log.w(tag, message)
}
