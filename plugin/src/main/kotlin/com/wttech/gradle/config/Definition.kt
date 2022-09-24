package com.wttech.gradle.config

import com.wttech.gradle.config.gui.Dialog
import com.wttech.gradle.config.util.capitalLetter
import com.wttech.gradle.config.util.capitalWords
import org.gradle.api.Project

open class Definition(val name: String, val project: Project) {

    private val logger = project.logger

    val settings by lazy { project.extensions.getByType(ConfigExtension::class.java) }

    val fileManager by lazy { settings.fileManager() }

    fun fileManager(options: FileManager.() -> Unit) {
        fileManager.apply(options)
    }

    val debugMode = project.objects.property(Boolean::class.java).apply {
        convention(false)
    }

    val inputMode = project.objects.property(InputMode::class.java).apply {
        set(InputMode.GUI)
    }

    val outputDir = project.objects.directoryProperty().apply {
        set(project.layout.projectDirectory.dir(".gradle/config"))
    }

    val outputCapturedFile = project.objects.fileProperty().apply {
        set(outputDir.map { it.file("$name.captured.yml")})
    }

    val outputYmlFile = project.objects.fileProperty().apply {
        set(outputDir.map { it.file("$name.yml")})
    }

    val outputJsonFile = project.objects.fileProperty().apply {
        set(outputDir.map { it.file("$name.json")})
    }

    // TODO deal with loading in build/tasks in GAT
    val outputLoaded = project.objects.property(Boolean::class.java).apply {
        set(false)
    }

    val groups = project.objects.listProperty(Group::class.java)
    
    fun group(groupName: String, options: Group.() -> Unit) {
        groups.add(project.provider { Group(this, groupName).apply(options) })
    }

    @Suppress("unchecked_cast")
    val props get() = groups.get().flatMap { it.props.get() } as List<Prop>

    fun findProp(propName: String) = props.firstOrNull { it.name == propName }

    fun hasProp(propName: String) = findProp(propName) != null

    fun getProp(propName: String) = findProp(propName)
        ?: throw ConfigException("Prop '$propName' is not defined!")

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

    val valuesSaved: Map<String, Any?>
        get() = props.filter(valueFilter).associate { it.name to it.valueSaved() }

    fun value(propName: String) = getProp(propName).single.value()

    fun listValue(propName: String) = getProp(propName).list.value()

    fun mapValue(propName: String) = getProp(propName).map.value()

    val labelDict = project.objects.mapProperty(String::class.java, String::class.java).apply {
        set(mapOf())
    }
    fun labelAbbrs(abbrs: Iterable<String>) {
        labelDict.putAll(abbrs.associate { it.capitalLetter() to it.uppercase() })
    }

    fun labelAbbrs(vararg abbrs: String) = labelAbbrs(abbrs.asIterable())

    fun composeLabel(text: String): String = labelDict.get().entries.fold(text.capitalWords()) { n, (s, r) -> n.replace(s, r) }

    fun capture() {
        lockDefinitions()
        if (debugMode.get()) printDefinitions()
        readValues()
        captureValues()
        if (debugMode.get()) printValues()
        saveValues()
    }

    private fun lockDefinitions() {
        groups.finalizeValueOnRead()
        groups.get().forEach { it.props.finalizeValueOnRead() }
    }

    private fun printDefinitions() {
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

    private fun readValues() {
        val file = outputCapturedFile.get().asFile
        if (file.exists()) {
            logger.lifecycle("Config '$name' is loading values from output file '$file'")
            values = fileManager.readYml(file)
        }
    }

    fun captureValues() {
        logger.lifecycle("Config '$name' is capturing values using input mode '${inputMode.get()}'")
        when (inputMode.get()) {
            InputMode.GUI -> { Dialog.render(this) }
            InputMode.CLI -> TODO("Config CLI input mode is not yet supported!")
            else -> throw ConfigException("Config '$name' uses unsupported input mode!")
        }
    }

    private fun printValues() {
        logger.lifecycle("Config '$name' values are as follows (debug mode is on)")
        println("\n${fileManager.yaml.get().dump(values)}\n")
    }

    private fun saveValues() {
        val ymlCached = outputCapturedFile.asFile.get()
        logger.lifecycle("Config '$name' is saving captured values to file '$ymlCached'")
        fileManager.writeYml(ymlCached, values)

        val ymlFiltered = outputYmlFile.asFile.get()
        logger.lifecycle("Config '$name' is saving filtered values to file '$ymlFiltered'")
        fileManager.writeYml(ymlFiltered, valuesSaved)

        val jsonFiltered = outputJsonFile.asFile.get()
        logger.lifecycle("Config '$name' is saving filtered values to file '$jsonFiltered'")
        fileManager.writeJson(jsonFiltered, valuesSaved)
    }
}