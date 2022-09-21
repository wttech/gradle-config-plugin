package com.wttech.gradle.config

import com.wttech.gradle.config.tasks.Config as ConfigTask
import org.gradle.api.Project

open class ConfigExtension(val project: Project) {

    companion object {
        const val NAME = "config"
    }

    fun task(name: String, options: ConfigTask.() -> Unit) = project.tasks.register(name, ConfigTask::class.java, options)
}