package com.wttech.gradle.config

import com.wttech.gradle.config.gui.Dialog
import com.wttech.gradle.config.util.capitalLetter
import com.wttech.gradle.config.util.capitalWords
import org.gradle.api.Project
import java.io.File

open class Definition(val name: String, val project: Project) {

    private val logger = project.logger

    val settings by lazy { project.extensions.getByType(ConfigExtension::class.java) }

    val fileManager by lazy { FileManager(this) }

    fun fileManager(options: FileManager.() -> Unit) {
        fileManager.apply(options)
    }

    val label = project.objects.property(String::class.java).apply {
        convention(project.provider { composeLabel(name) })
    }

    fun label(text: String) {
        label.set(text)
    }

    val inputMode = project.objects.property(InputMode::class.java).apply {
        set(InputMode.GUI)
    }

    val inputFile = project.objects.fileProperty()

    val debug = project.objects.property(Boolean::class.java).apply {
        convention(false)
    }

    val verbose = project.objects.property(Boolean::class.java).apply {
        convention(inputMode.map { it == InputMode.FILE })
    }

    val outputDir = project.objects.directoryProperty().apply {
        set(project.layout.projectDirectory.dir(".gradle/config"))
    }

    val outputCapturedFile = project.objects.fileProperty().apply {
        set(outputDir.map { it.file("$name.captured.yml") })
    }

    val outputYmlFile = project.objects.fileProperty().apply {
        set(outputDir.map { it.file("$name.yml") })
    }

    val outputJsonFile = project.objects.fileProperty().apply {
        set(outputDir.map { it.file("$name.json") })
    }

    val outputXmlFile = project.objects.fileProperty().apply {
        set(outputDir.map { it.file("$name.xml") })
    }

    val outputPropertiesFile = project.objects.fileProperty().apply {
        set(outputDir.map { it.file("$name.properties") })
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

    fun value(propName: String) = getProp(propName).value()

    fun stringValue(propName: String) = getProp(propName).string.value()

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
        if (debug.get()) printDefinitions()
        readCapturedValues()
        captureValues()
        if (debug.get()) printValues()
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

    internal fun readCapturedValues() {
        val file = outputCapturedFile.get().asFile
        if (file.exists()) {
            logger.lifecycle("Config '$name' is reading values from file '$file'")
            values = fileManager.readYml(file)
        }
    }

    private fun readInputValues() {
        val file = inputFile.get().asFile
        if (!file.exists()) {
            throw ConfigException("Config '$name' cannot read values as input file does not exist '$file'!")
        }

        logger.lifecycle("Config '$name' is reading values from input file '$file'")
        values = when (file.extension) {
            "yml" -> fileManager.readYml(file)
            "json" -> fileManager.readJson(file)
            else -> throw ConfigException("Config '$name' cannot read values from unsupported type of input file '$file'!")
        }
    }

    fun captureValues() {
        logger.lifecycle("Config '$name' is capturing values using input mode '${inputMode.get()}'")
        when (inputMode.get()) {
            InputMode.GUI -> Dialog.render(this)
            InputMode.CLI -> TODO("Config CLI input mode is not yet supported!")
            InputMode.FILE -> readInputValues()
            else -> throw ConfigException("Config '$name' uses unsupported input mode!")
        }
    }

    private fun printValues() {
        logger.lifecycle("Config '$name' values are as follows (debug mode is on)")
        println("\n${fileManager.yaml.get().dump(values)}\n")
    }

    private fun saveValues() {
        outputCapturedFile.asFile.get().let { saveValues(it, true) { fileManager.writeYml(it, values) } }
        outputYmlFile.asFile.get().let { saveValues(it) { fileManager.writeYml(it, valuesSaved) } }
        outputJsonFile.asFile.get().let { saveValues(it) { fileManager.writeJson(it, valuesSaved) } }
        outputPropertiesFile.asFile.get().let { saveValues(it) { fileManager.writeProperties(it, valuesSaved) } }
        outputXmlFile.asFile.get().let { saveValues(it) { fileManager.writeXml(it, valuesSaved) } }
    }

    private fun saveValues(file: File, verbose: Boolean = false, saver: () -> Unit) {
        try {
            logger.lifecycle("Config '$name' is saving values to file '$file'")
            saver()
        } catch (e: Exception) {
            when {
                verbose -> throw ConfigException("Config '$name' could not save values to file '$file'! Cause: ${e.message}", e)
                else -> logger.warn("Config '$name' could not save values to file '$file'!", e)
            }
        }
    }
}
