package com.wttech.gradle.config.util

val Throwable.rootCause: Throwable get() {
    var result = this
    while (result.cause != null && result.cause !== result) {
        result = result.cause!!
    }
    return result
}
