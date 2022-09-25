package com.wttech.gradle.config

class CancelException : ConfigException {

    constructor(message: String) : super(message)

    constructor(message: String, e: Exception) : super(message, e)

}