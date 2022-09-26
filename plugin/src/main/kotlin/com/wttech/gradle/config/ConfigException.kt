package com.wttech.gradle.config

import org.gradle.api.GradleException

open class ConfigException : GradleException {
    constructor(message: String) : super(message)
    constructor(message: String, e: Exception) : super(message, e)
}
