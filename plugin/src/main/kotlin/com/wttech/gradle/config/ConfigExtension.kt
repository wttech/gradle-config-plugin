package com.wttech.gradle.config

import com.wttech.gradle.config.tpl.TemplateEngine
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

    operator fun get(name: String) = valueOrNull(name)

    val values get() = get().values

    fun value(propName: String) = valueOrNull(propName)
        ?: throw ConfigException("Config prop '$propName' is null!")

    fun valueOrNull(propName: String) = read().value(propName)

    fun stringValue(propName: String) = stringValueOrNull(propName)
        ?: throw ConfigException("Config string prop '$propName' is null!")

    fun stringValueOrNull(propName: String) = read().stringValue(propName)

    fun listValue(propName: String) = listValueOrNull(propName)
        ?: throw ConfigException("Config list prop '$propName' is null!")

    fun listValueOrNull(propName: String) = read().listValue(propName)

    fun mapValue(propName: String) = mapValueOrNull(propName)
        ?: throw ConfigException("Config map prop '$propName' is null!")

    fun mapValueOrNull(propName: String) = read().mapValue(propName)

    fun get() = named(DEFAULT_NAME)

    val ymlFile get() = get().outputYmlFile

    val xmlFile get() = get().outputXmlFile

    val jsonFile get() = get().outputJsonFile

    val propertiesFile get() = get().outputPropertiesFile

    // Utilities

    fun fileManager() = FileManager(project)

    fun templateEngine() = TemplateEngine(project)

    companion object {
        const val NAME = "config"

        const val DEFAULT_TASK = "config"

        const val DEFAULT_NAME = "default"

        fun of(project: Project) = project.rootProject.extensions.findByType(ConfigExtension::class.java)
            ?: error("Config plugin with ID '${ConfigPlugin.ID}' is not applied at the root of the build!")
    }
}

val Project.config get() = ConfigExtension.of(this)