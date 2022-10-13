package io.wttech.gradle.config

import io.wttech.gradle.config.tpl.TemplateEngine
import org.gradle.api.Project
import org.gradle.api.Task
import io.wttech.gradle.config.tasks.Config as ConfigTask

open class ConfigExtension(val project: Project) {

    val definitions = project.objects.listProperty(Definition::class.java).apply {
        finalizeValueOnRead()
        set(listOf())
    }

    private val definitionTasks = mutableMapOf<String, String>()

    private val definitionRead = mutableMapOf<String, Boolean>()

    fun named(name: String) = definitions.get().firstOrNull { it.name == name }
        ?: throw ConfigException("Config '$name' is not defined!")

    fun define(options: Definition.() -> Unit) = define(DEFAULT_NAME, options, DEFAULT_TASK)

    fun define(name: String, options: Definition.() -> Unit, taskName: String = name, taskOptions: ConfigTask.() -> Unit = {}) {
        val definition = project.provider { Definition(name, project).apply(options) }

        definitions.add(definition)
        definitionTasks[name] = taskName

        project.tasks.register(taskName, ConfigTask::class.java) { task ->
            task.definition.set(definition)
            task.apply(taskOptions)
        }
    }

    fun String.invoke(options: Definition.() -> Unit) = define(this, options)

    fun captured(name: String) = named(name)

    val captured get() = captured(DEFAULT_NAME)

    fun requiredBy(task: Task, name: String = DEFAULT_NAME) {
        project.gradle.taskGraph.whenReady {
            if (it.hasTask(task)) {
                val config = named(name)
                if (!config.captured) {
                    throw ConfigException(mutableListOf("Config '$name' is not yet captured!").apply {
                        definitionTasks[name]?.let { taskName -> add("Run task '$taskName' to provide configuration values.") }
                    }.joinToString("\n"))
                }
            }
        }
    }

    fun read() = read(DEFAULT_NAME)

    fun read(name: String) = named(name).apply {
        definitionRead.getOrPut(name) { finalize(); readCapturedValues(); true }
    }

    operator fun get(name: String) = valueOrNull(name)

    val values get() = get().values

    fun value(propName: String) = read().value(propName)

    fun valueOrNull(propName: String) = read().valueOrNull(propName)

    fun stringValueOrNull(propName: String) = read().stringValueOrNull(propName)

    fun boolValue(propName: String) = stringValue(propName).toBoolean()

    fun boolValueOrNull(propName: String) = stringValueOrNull(propName)?.toBoolean()

    fun intValue(propName: String) = stringValue(propName).toInt()

    fun intValueOrNull(propName: String) = stringValueOrNull(propName)?.toInt()

    fun doubleValue(propName: String) = stringValue(propName).toDouble()

    fun doubleValueOrNull(propName: String) = stringValueOrNull(propName)?.toDouble()

    fun stringValue(propName: String) = read().stringValue(propName)

    fun listValue(propName: String) = read().listValue(propName)

    fun listValueOrNull(propName: String) = read().listValue(propName)

    fun mapValue(propName: String) = read().mapValue(propName)

    fun mapValueOrNull(propName: String) = read().mapValueOrNull(propName)

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

        fun find(project: Project) = project.project.extensions.findByType(ConfigExtension::class.java)

        fun of(project: Project): ConfigExtension {
            val root = project == project.rootProject
            return find(project) ?: find(project.rootProject) ?: error(when {
                root -> "Config plugin with ID '${ConfigPlugin.ID}' is not applied at the root of the build!"
                else -> "Config plugin with ID '${ConfigPlugin.ID}' is not applied either at the project '${project.path}' and at the root of the build!"
            })
        }
    }
}
