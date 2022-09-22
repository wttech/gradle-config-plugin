package com.wttech.gradle.config

import com.wttech.gradle.config.gui.Dialog
import org.gradle.api.DefaultTask
import org.gradle.api.tasks.Internal
import org.gradle.api.tasks.TaskAction

open class Config : DefaultTask() {

    @get:Internal
    val config by lazy { project.extensions.getByType(ConfigExtension::class.java) }

    @get:Internal
    val fileManager by lazy { config.fileManager() }

    fun fileManager(options: FileManager.() -> Unit) {
        fileManager.apply(options)
    }

    @Internal
    val inputMode = project.objects.property(InputMode::class.java).apply {
        set(InputMode.GUI)
    }

    @Internal
    val outputCacheFile = project.objects.fileProperty().apply {
        set(project.layout.projectDirectory.file(".gradle/config/$name.cache.yml"))
    }

    @Internal
    val outputYmlFile = project.objects.fileProperty().apply {
        set(project.layout.projectDirectory.file(".gradle/config/$name.yml"))
    }

    @Internal
    val outputJsonFile = project.objects.fileProperty().apply {
        set(project.layout.projectDirectory.file(".gradle/config/$name.json"))
    }

    // TODO deal with loading in build/tasks in GAT
    @Internal
    val outputLoaded = project.objects.property(Boolean::class.java).apply {
        set(false)
    }

    @Internal
    val groups = project.objects.listProperty(Group::class.java)
    
    fun group(groupName: String, options: Group.() -> Unit) {
        groups.add(project.provider { Group(this, groupName).apply(options) })
    }

    @get:Internal
    val props get() = groups.get().flatMap { it.props.get() }

    fun findProp(propName: String) = props.firstOrNull { it.name == propName }

    fun hasProp(propName: String) = findProp(propName) != null

    fun prop(propName: String) = findProp(propName)
        ?: throw ConfigException("Prop '$propName' is not defined!")

    fun value(propName: String) = prop(propName).value()

    @get:Internal
    var values: Map<String, Any?>
        get() = props.associate { it.name to it.value() }
        set(vs) { vs.forEach { (k, v) -> findProp(k)?.value(v) } }

    @get:Internal
    val valuesCurrent: Map<String, Any?>
        get() = props.filter { it.group.visible.get() && it.visible.get() }.associate { it.name to it.value() }

    @TaskAction
    fun process() {
        lockDefinitions()
        printDefinitions()
        readValues()
        captureValues()
        printValues()
        saveValues()
    }

    private fun lockDefinitions() {
        // Freeze lazy definitions
        groups.finalizeValueOnRead()
        groups.get().forEach { it.props.finalizeValueOnRead() }
    }

    private fun printDefinitions() {
        if (config.debugMode.get()) {
            logger.lifecycle("Config '$name' groups and properties are defined like follows (debug mode is on)")
            println()
            groups.get().forEach { group ->
                println(group)
                group.props.get().forEach { prop ->
                    println(prop)
                }
            }
            println()
        }
    }

    private fun readValues() {
        val file = outputCacheFile.get().asFile
        if (file.exists()) {
            logger.lifecycle("Config '$name' is loading values from output file '$file'")
            values = fileManager.readYml(file)
        }
    }

    private fun captureValues() {
        logger.lifecycle("Config '$name' is gathering values using input mode '${inputMode.orNull}'")
        when (inputMode.get()) {
            InputMode.GUI -> { Dialog.render(this) }
            InputMode.CLI -> TODO("Config CLI input mode is not yet supported!")
            else -> throw ConfigException("Config input mode is not specified!")
        }
    }

    private fun printValues() {
        if (config.debugMode.get()) {
            logger.lifecycle("Config '$name' values are as follows (debug mode is on)")
            println("\n${fileManager.yaml.get().dump(values)}\n")
        }
    }

    private fun saveValues() {
        val cache = outputCacheFile.asFile.get()
        logger.lifecycle("Config '$name' is saving cached values to file '$cache'")
        fileManager.writeYml(cache, values)

        val currentYml = outputYmlFile.asFile.get()
        logger.lifecycle("Config '$name' is saving current values to file '$currentYml'")
        fileManager.writeYml(currentYml, valuesCurrent)

        val currentJson = outputJsonFile.asFile.get()
        logger.lifecycle("Config '$name' is saving current values to file '$currentJson'")
        fileManager.writeJson(currentJson, valuesCurrent)
    }
}