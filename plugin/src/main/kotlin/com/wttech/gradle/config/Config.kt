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
    val outputDir = project.objects.directoryProperty().apply {
        set(project.layout.projectDirectory.dir(".gradle/config"))
    }

    @Internal
    val outputCacheFile = project.objects.fileProperty().apply {
        set(outputDir.map { it.file(".gradle/config/$name.cache.yml")})
    }

    @Internal
    val outputYmlFile = project.objects.fileProperty().apply {
        set(outputDir.map { it.file(".gradle/config/$name.yml")})
    }

    @Internal
    val outputJsonFile = project.objects.fileProperty().apply {
        set(outputDir.map { it.file(".gradle/config/$name.json")})
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
    @Suppress("unchecked_cast")
    val props get() = groups.get().flatMap { it.props.get() } as List<Prop<Any>>

    fun findProp(propName: String) = props.firstOrNull { it.name == propName }

    fun hasProp(propName: String) = findProp(propName) != null

    fun prop(propName: String) = findProp(propName)
        ?: throw ConfigException("Prop '$propName' is not defined!")

    fun value(propName: String) = prop(propName).value()

    @get:Internal
    var values: Map<String, Any?>
        get() = props.associate { it.name to it.value() }
        set(vs) { vs.forEach { (k, v) -> findProp(k)?.value(v) } }

    private var valueFilter: Prop<Any>.() -> Boolean = { group.visible.get() && visible.get() }

    fun valueFilter(predicate: Prop<Any>.() -> Boolean) {
        this.valueFilter = predicate
    }

    @get:Internal
    val valuesFiltered: Map<String, Any?>
        get() = props.filter(valueFilter).associate { it.name to it.value() }

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
        logger.lifecycle("Config '$name' is capturing values using input mode '${inputMode.orNull}'")
        when (inputMode.get()) {
            InputMode.GUI -> { Dialog.render(this) }
            InputMode.CLI -> TODO("Config CLI input mode is not yet supported!")
            else -> throw ConfigException("Config '$name' input mode is not specified!")
        }
    }

    private fun printValues() {
        if (config.debugMode.get()) {
            logger.lifecycle("Config '$name' values are as follows (debug mode is on)")
            println("\n${fileManager.yaml.get().dump(values)}\n")
        }
    }

    private fun saveValues() {
        val ymlCached = outputCacheFile.asFile.get()
        logger.lifecycle("Config '$name' is saving cached values to file '$ymlCached'")
        fileManager.writeYml(ymlCached, values)

        val ymlFiltered = outputYmlFile.asFile.get()
        logger.lifecycle("Config '$name' is saving filtered values to file '$ymlFiltered'")
        fileManager.writeYml(ymlFiltered, valuesFiltered)

        val jsonFiltered = outputJsonFile.asFile.get()
        logger.lifecycle("Config '$name' is saving filtered values to file '$jsonFiltered'")
        fileManager.writeJson(jsonFiltered, valuesFiltered)
    }
}