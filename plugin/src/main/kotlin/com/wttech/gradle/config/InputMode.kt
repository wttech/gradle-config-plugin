package com.wttech.gradle.config

enum class InputMode {
    GUI,
    CLI;

    companion object {
        fun of(name: String) = values().firstOrNull { it.name.equals(name, true) }
            ?: throw ConfigException("Config input mode '$name' is not supported!")
    }
}