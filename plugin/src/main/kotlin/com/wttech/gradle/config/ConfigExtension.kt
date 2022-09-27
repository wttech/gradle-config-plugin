package com.wttech.gradle.config

import org.gradle.api.Project
import com.wttech.gradle.config.tasks.Config as ConfigTask

open class ConfigExtension(val project: Project) {

    val definitions = project.objects.listProperty(Definition::class.java).apply {
        finalizeValueOnRead()
        set(listOf())
    }

    fun named(name: String) = definitions.get().firstOrNull { it.name == name }
        ?: throw ConfigException("Config '$name' is not defined!")

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

    fun read(name: String) = named(name).apply { readCapturedValues() }

    fun read() = read(DEFAULT_NAME)

    operator fun get(name: String) = value(name)

    fun value(propName: String) = read().value(propName)

    fun stringValue(propName: String) = read().stringValue(propName)

    fun listValue(propName: String) = read().listValue(propName)

    fun mapValue(propName: String) = read().mapValue(propName)

    fun get() = named(DEFAULT_NAME)

    val ymlFile get() = get().outputYmlFile

    val xmlFile get() = get().outputXmlFile

    val jsonFile get() = get().outputJsonFile

    val propertiesFile get() = get().outputPropertiesFile

    companion object {
        const val NAME = "config"

        const val DEFAULT_TASK = "config"

        const val DEFAULT_NAME = "default"
    }
}
