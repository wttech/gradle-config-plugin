package com.wttech.gradle.config

import com.wttech.gradle.config.gui.Dialog
import org.gradle.api.DefaultTask
import org.gradle.api.tasks.Internal
import org.gradle.api.tasks.TaskAction
import java.io.File

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
    val outputFile = project.objects.fileProperty().apply {
        set(project.layout.projectDirectory.file(".gradle/config/$name.yml"))
    }

    fun outputFile(path: String) {
        outputFile.set(project.layout.projectDirectory.file(path))
    }

    @Internal
    var outputAction: () -> Unit = { saveValuesAsJson() }

    fun outputAction(action: () -> Unit) {
        this.outputAction = action
    }

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
        val file = outputFile.get().asFile
        logger.lifecycle("Config '$name' is loading values from output file '$file'")
        values = fileManager.readYml(file)
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
        saveValuesAsYml(outputFile.get().asFile)
        outputAction()
    }

    fun saveValuesAsYml() = saveValuesAsYml(outputFile.get().asFile)

    fun saveValuesAsYml(file: File) {
        logger.lifecycle("Config '$name' is outputting values to file '$file'")
        fileManager.writeYml(file, values)
    }

    fun saveValuesAsJson() = saveValuesAsJson(outputFile.get().asFile.let { it.parentFile.resolve("${it.nameWithoutExtension}.json") })

    fun saveValuesAsJson(file: File) {
        logger.lifecycle("Config '$name' is outputting values to file '$file'")
        fileManager.writeJson(file, values)
    }
}