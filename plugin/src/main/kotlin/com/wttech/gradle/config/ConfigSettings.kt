package com.wttech.gradle.config

import com.wttech.gradle.config.tasks.Config as ConfigTask
import org.gradle.api.Project

open class ConfigSettings(val project: Project) {

    val defined = project.objects.listProperty(Config::class.java).apply {
        finalizeValueOnRead()
        set(listOf())
    }

    fun define(options: Config.() -> Unit) = define(DEFAULT_NAME, options, DEFAULT_TASK)

    fun define(name: String, options: Config.() -> Unit, taskName: String = name, taskOptions: ConfigTask.() -> Unit = {}) {
        val definition = project.provider { Config(name, project).apply(options) }

        defined.add(definition)

        project.tasks.register(taskName, ConfigTask::class.java) { task ->
            task.definition.set(definition)
            task.apply(taskOptions)
        }
    }

    fun String.invoke(options: Config.() -> Unit) = define(this, options)

    fun fileManager() = FileManager(project)

    val debugMode = project.objects.property(Boolean::class.java).apply {
        convention(false)
        project.findProperty("config.debugMode")?.let { set(it.toString().toBoolean()) }
    }

    companion object {
        const val NAME = "config"

        const val DEFAULT_TASK = "config"

        const val DEFAULT_NAME = "default"
    }
}