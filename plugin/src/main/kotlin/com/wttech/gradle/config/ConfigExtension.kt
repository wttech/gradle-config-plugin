package com.wttech.gradle.config

import com.wttech.gradle.config.Config as ConfigTask
import org.gradle.api.Project

open class ConfigExtension(val project: Project) {

    fun task(options: ConfigTask.() -> Unit) = task(TASK_DEFAULT, options)

    fun task(name: String, options: ConfigTask.() -> Unit) = project.tasks.register(name, ConfigTask::class.java, options)

    companion object {
        const val NAME = "config"

        const val TASK_DEFAULT = "config"
    }
}