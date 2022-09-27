package com.wttech.gradle.config

enum class FileType {
    YML,
    JSON,
    XML,
    PROPERTIES;

    fun extension() = this.name.lowercase()
}