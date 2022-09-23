package com.wttech.gradle.config

import com.wttech.gradle.config.gui.Dialog
import com.wttech.gradle.config.util.capitalLetter
import com.wttech.gradle.config.util.capitalWords
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
    val outputCapturedFile = project.objects.fileProperty().apply {
        set(outputDir.map { it.file("$name.captured.yml")})
    }

    @Internal
    val outputYmlFile = project.objects.fileProperty().apply {
        set(outputDir.map { it.file("$name.yml")})
    }

    @Internal
    val outputJsonFile = project.objects.fileProperty().apply {
        set(outputDir.map { it.file("$name.json")})
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
    val props get() = groups.get().flatMap { it.props.get() } as List<Prop>

    fun findProp(propName: String) = props.firstOrNull { it.name == propName }

    fun hasProp(propName: String) = findProp(propName) != null

    fun getProp(propName: String) = findProp(propName)
        ?: throw ConfigException("Prop '$propName' is not defined!")

    @get:Internal
    var values: Map<String, Any?>
        get() = props.associate { it.name to it.value() }
        set(vs) { vs.forEach { (k, v) -> findProp(k)?.value(v) } }

    private var valueFilter: Prop.() -> Boolean = { group.visible.get() && visible.get() }

    fun valueSaveAll() = valueFilter { true }

    fun valueSaveVisible() = valueFilter { group.visible.get() && visible.get() }

    fun valueSaveEnabled() = valueFilter { group.enabled.get() && enabled.get() }

    fun valueSaveEnabledAndVisible() = valueFilter { group.visible.get() && visible.get() }

    fun valueFilter(predicate: Prop.() -> Boolean) {
        this.valueFilter = predicate
    }

    @get:Internal
    val valuesSaved: Map<String, Any?>
        get() = props.filter(valueFilter).associate { it.name to it.value() }

    fun value(propName: String) = getProp(propName).single.value()

    fun listValue(propName: String) = getProp(propName).list.value()

    fun mapValue(propName: String) = getProp(propName).map.value()

    @Internal
    val labelDict = project.objects.mapProperty(String::class.java, String::class.java).apply {
        set(mapOf())
    }
    fun labelAbbrs(abbrs: Iterable<String>) {
        labelDict.putAll(abbrs.associate { it.capitalLetter() to it.uppercase() })
    }

    fun labelAbbrs(vararg abbrs: String) = labelAbbrs(abbrs.asIterable())

    fun composeLabel(text: String): String = labelDict.get().entries.fold(text.capitalWords()) { n, (s, r) -> n.replace(s, r) }

    @TaskAction
    fun process() {
        lockDefinitions()
        printDefinitions()
        // TODO readValues()
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
        val file = outputCapturedFile.get().asFile
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
        val ymlCached = outputCapturedFile.asFile.get()
        logger.lifecycle("Config '$name' is saving cached values to file '$ymlCached'")
        fileManager.writeYml(ymlCached, values)

        val ymlFiltered = outputYmlFile.asFile.get()
        logger.lifecycle("Config '$name' is saving filtered values to file '$ymlFiltered'")
        fileManager.writeYml(ymlFiltered, valuesSaved)

        val jsonFiltered = outputJsonFile.asFile.get()
        logger.lifecycle("Config '$name' is saving filtered values to file '$jsonFiltered'")
        fileManager.writeJson(jsonFiltered, valuesSaved)
    }
}