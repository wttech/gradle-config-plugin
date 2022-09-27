package com.wttech.gradle.config

import com.wttech.gradle.config.gui.Gui
import com.wttech.gradle.config.tpl.TemplateEngine
import com.wttech.gradle.config.util.capitalLetter
import com.wttech.gradle.config.util.capitalWords
import org.gradle.api.Project
import org.gradle.api.file.RegularFile
import org.gradle.api.provider.Provider
import java.io.File

@Suppress("TooManyFunctions")
open class Definition(val name: String, val project: Project) {

    private val logger = project.logger

    val settings by lazy { project.extensions.getByType(ConfigExtension::class.java) }

    val fileManager by lazy { FileManager(project) }

    fun fileManager(options: FileManager.() -> Unit) {
        fileManager.apply(options)
    }

    val templateEngine by lazy { TemplateEngine(project) }

    fun templateEngine(options: TemplateEngine.() -> Unit) {
        templateEngine.apply(options)
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

    val outputCapturedFile = outputDir.map { it.file("$name.captured.yml") }

    fun outputFile(extension: String) = outputDir.map { it.file("$name.$extension") }

    fun outputFile(type: FileType) = outputFile(type.extension())

    val outputYmlFile get() = outputFile(FileType.YML).get().asFile

    val outputXmlFile get() = outputFile(FileType.XML).get().asFile

    val outputJsonFile get() = outputFile(FileType.JSON).get().asFile

    val outputPropertiesFile get() = outputFile(FileType.PROPERTIES).get().asFile

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

    private var valueSaveFilter: Prop.() -> Boolean = { group.visible.get() && visible.get() }

    fun valueSaveAll() = valueSaveFilter { true }

    fun valueSaveVisible() = valueSaveFilter { group.visible.get() && visible.get() }

    fun valueSaveEnabled() = valueSaveFilter { group.enabled.get() && enabled.get() }

    fun valueSaveEnabledAndVisible() = valueSaveFilter { group.visible.get() && visible.get() }

    fun valueSaveFilter(predicate: Prop.() -> Boolean) {
        this.valueSaveFilter = predicate
    }

    val valuesSaved: Map<String, Any?>
        get() = props.filter(valueSaveFilter).associate { it.name to it.valueSaved() }

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
        logger.info("Config '$name' groups and properties are defined like follows (debug mode is on)")
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
            logger.info("Config '$name' is reading values from file '$file'")
            values = fileManager.readYml(file)
        }
    }

    private fun readInputValues() {
        val file = inputFile.get().asFile
        if (!file.exists()) {
            throw ConfigException("Config '$name' cannot read values as input file does not exist '$file'!")
        }

        logger.info("Config '$name' is reading values from input file '$file'")
        values = when (file.extension) {
            "yml" -> fileManager.readYml(file)
            "json" -> fileManager.readJson(file)
            else -> throw ConfigException("Config '$name' cannot read values from unsupported type of input file '$file'!")
        }
    }

    fun captureValues() {
        logger.info("Config '$name' is capturing values using input mode '${inputMode.get()}'")
        when (inputMode.get()) {
            InputMode.GUI -> Gui.render(this)
            InputMode.CLI -> TODO("Config CLI input mode is not yet supported!")
            InputMode.FILE -> readInputValues()
            else -> throw ConfigException("Config '$name' uses unsupported input mode!")
        }
    }

    private fun printValues() {
        logger.info("Config '$name' values are as follows (debug mode is on)")
        println("\n${fileManager.yaml.get().dump(values)}\n")
    }

    private val valueSavers = mutableSetOf<() -> Unit>()

    fun valueSave(saver: () -> Unit) {
        valueSavers.add(saver)
    }

    fun valueSaveYml() = valueSave {
        fileManager.writeYml(outputYmlFile, valuesSaved)
    }

    fun valueSaveXml() = valueSave {
        fileManager.writeXml(outputXmlFile, valuesSaved)
    }

    fun valueSaveProperties() = valueSave {
        fileManager.writeProperties(outputPropertiesFile, valuesSaved)
    }

    fun valueSaveJson() = valueSave {
        fileManager.writeJson(outputJsonFile, valuesSaved)
    }

    fun valueSaveTemplate(templatePath: String, targetPath: String) = valueSaveTemplate(project.file(templatePath), project.file(targetPath))

    fun valueSaveTemplate(template: File, target: File) = valueSave {
        templateEngine.renderFile(template, target, valuesSaved)
    }
    fun valueSaveTemplate(template: Provider<RegularFile>, target: Provider<RegularFile>) = valueSave {
        templateEngine.renderFile(template.get().asFile, target.get().asFile, valuesSaved)
    }

    fun valueSaveGradleProperties() = valueSave {
        val template = project.file("gradle.properties.peb")
        val target = project.file("gradle.properties")

        logger.info("Config '$name' is saving Gradle properties file '$target'.\nEnsure having it ignored by version control system (like Git)!")
        templateEngine.renderFile(template, target, valuesSaved)
    }

    @Suppress("TooGenericExceptionCaught")
    private fun saveValues() {
        outputCapturedFile.get().asFile.let { file ->
            logger.info("Config '$name' is saving captured values to file '$file'")
            fileManager.writeYml(file, values)
        }

        if (valueSavers.isNotEmpty()) {
            logger.info("Config '$name' is saving values additionally (${valueSavers.size})'")
            valueSavers.forEach { valueSaver ->
                try {
                    valueSaver()
                } catch (e: Exception) {
                    logger.warn("Config '$name' cannot save values properly! Cause: ${e.message}", e)
                }
            }
        }
    }
}
