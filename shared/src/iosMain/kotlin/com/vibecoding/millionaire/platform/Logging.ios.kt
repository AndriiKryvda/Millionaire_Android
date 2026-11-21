package com.vibecoding.millionaire.platform

import platform.Foundation.NSLog

actual fun logInfo(tag: String, message: String) {
    NSLog("%@\n%@", tag, message)
}

actual fun logWarn(tag: String, message: String) {
    NSLog("%@ warning: %@", tag, message)
}
