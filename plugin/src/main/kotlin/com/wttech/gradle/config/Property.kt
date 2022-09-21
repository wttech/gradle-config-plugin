package com.wttech.gradle.config

import com.wttech.gradle.config.tasks.Config

class Property(val config: Config, val name: String) {

    private val project = config.project

    val type = config.project.objects.property(PropertyType::class.java).apply {
        set(PropertyType.TEXT)
    }

    val value = config.project.objects.property(String::class.java)

    fun text() {
        type.set(PropertyType.TEXT)
    }
}