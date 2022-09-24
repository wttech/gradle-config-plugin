package com.wttech.gradle.config

import com.wttech.gradle.config.tasks.Config as ConfigTask
import org.gradle.api.Project

open class ConfigExtension(val project: Project) {

    val definitions = project.objects.listProperty(Definition::class.java).apply {
        finalizeValueOnRead()
        set(listOf())
    }

    fun define(options: Definition.() -> Unit) = define(DEFAULT_NAME, options, DEFAULT_TASK)

    fun define(name: String, options: Definition.() -> Unit, taskName: String = name, taskOptions: ConfigTask.() -> Unit = {}) {
        val definition = project.provider { Definition(name, project).apply(options) }

        definitions.add(definition)

        project.tasks.register(taskName, ConfigTask::class.java) { task ->
            task.definition.set(definition)
            task.apply(taskOptions)
        }
    }

    fun String.invoke(options: Definition.() -> Unit) = define(this, options)

    fun fileManager() = FileManager(project)

    companion object {
        const val NAME = "config"

        const val DEFAULT_TASK = "config"

        const val DEFAULT_NAME = "default"
    }
}