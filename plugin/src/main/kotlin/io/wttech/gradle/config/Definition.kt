package io.wttech.gradle.config

import io.wttech.gradle.config.cli.Cli
import io.wttech.gradle.config.gui.Gui
import io.wttech.gradle.config.tpl.TemplateEngine
import io.wttech.gradle.config.tpl.TemplateSection
import io.wttech.gradle.config.util.capitalLetter
import io.wttech.gradle.config.util.capitalWords
import io.wttech.gradle.config.util.rootCause
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

    val fresh = project.objects.property(Boolean::class.java).apply {
        convention(false)
    }

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

    val captured get() = outputCapturedFile.get().asFile.exists()

    val valid get() = groups.get().all { it.valid }

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

    val props get() = groups.get().flatMap { it.props.get() } as List<Prop>

    fun findProp(propName: String) = props.firstOrNull { it.name == propName }

    fun hasProp(propName: String) = findProp(propName) != null

    fun getProp(propName: String) = findProp(propName)
        ?: throw ConfigException("Prop '$propName' is not defined!")

    var values: Map<String, Any?>
        get() = props.associate { it.name to it.value() }.toSortedMap()
        set(vs) { vs.forEach { (k, v) -> findProp(k)?.valueSet(v) } }

    private val valuesCaptured get() = props.filter { it.captured }.associate { it.name to it.value() }.toSortedMap()

    private var valueSaveFilter: (Prop) -> Boolean = { true }

    fun valueSaveFilter(predicate: (Prop) -> Boolean) {
        this.valueSaveFilter = predicate
    }

    val valuesSaved: Map<String, Any?> get() = valuesSaved(valueSaveFilter)

    fun valuesSaved(propFilter: (Prop) -> Boolean) = props.filter(propFilter).associate { it.name to it.valueSaved() }.toSortedMap()

    fun valueSaved(propName: String) = getProp(propName).valueSaved()

    fun value(propName: String) = valueOrNull(propName)
        ?: throw ConfigException("Config '$name' prop '$propName' is null!")

    fun valueOrNull(propName: String) = getProp(propName).value()

    fun stringValue(propName: String) = stringValueOrNull(propName)
        ?: throw ConfigException("Config '$name' string prop '$propName' is null!")

    fun stringValueOrNull(propName: String) = getProp(propName).string.value()

    fun boolValue(propName: String) = stringValue(propName).toBoolean()

    fun boolValueOrNull(propName: String) = stringValueOrNull(propName)?.toBoolean()

    fun intValue(propName: String) = stringValue(propName).toInt()

    fun intValueOrNull(propName: String) = stringValueOrNull(propName)?.toInt()

    fun doubleValue(propName: String) = stringValue(propName).toDouble()

    fun doubleValueOrNull(propName: String) = stringValueOrNull(propName)?.toDouble()

    fun listValue(propName: String) = listValueOrNull(propName)
        ?: throw ConfigException("Config '$name' list prop '$propName' is null!")

    fun listValueOrNull(propName: String) = getProp(propName).list.value()

    fun mapValue(propName: String) = mapValueOrNull(propName)
        ?: throw ConfigException("Config '$name' map prop '$propName' is null!")

    fun mapValueOrNull(propName: String) = getProp(propName).map.value()

    val labelDict = project.objects.mapProperty(String::class.java, String::class.java).apply {
        set(mapOf())
    }
    fun labelAbbrs(abbrs: Iterable<String>) {
        labelDict.putAll(abbrs.associate { it.capitalLetter() to it.uppercase() })
    }

    fun labelAbbrs(vararg abbrs: String) = labelAbbrs(abbrs.asIterable())

    fun composeLabel(text: String): String = labelDict.get().entries.fold(text.capitalWords()) { n, (s, r) -> n.replace(s, r) }

    fun capture() {
        finalize()
        if (debug.get()) printDefinitions()
        if (!fresh.get()) readCapturedValues()
        captureValues()
        if (debug.get()) printValues()
        validateValues()
        saveCapturedValues()
        saveValuesUsingSavers()
    }

    internal fun finalize() {
        groups.finalizeValueOnRead()
        groups.get().forEach { it.props.finalizeValueOnRead() }
        validate()
    }

    private fun validate() {
        val propsDuplicated = props.filter { p -> props.count { it.name == p.name } > 1 }.sortedBy { it.name }
        if (propsDuplicated.isNotEmpty()) {
            throw ConfigException((listOf(
                "Config '$name' has duplicated properties (${propsDuplicated.size})!"
            ) + propsDuplicated.map { "Property '${it.name}' defined in group '${it.group.name}'" }).joinToString("\n"))
        }
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
            "yml", "yaml" -> fileManager.readYml(file)
            "json" -> fileManager.readJson(file)
            else -> throw ConfigException("Config '$name' cannot read values from unsupported type of input file '$file'!")
        }
    }

    fun captureValues() {
        logger.info("Config '$name' is capturing values using input mode '${inputMode.get()}'")
        when (inputMode.get()) {
            InputMode.GUI -> Gui.render(this)
            InputMode.CLI -> Cli.render(this)
            InputMode.FILE -> readInputValues()
            InputMode.DEFAULTS -> { /* do nothing - use only defaults */ }
            else -> throw ConfigException("Config '$name' uses unsupported input mode!")
        }
    }

    private fun printValues() {
        logger.info("Config '$name' values are as follows (debug mode is on)")
        println("\n${fileManager.yaml.get().dump(values)}\n")
    }

    private fun validateValues() {
        val invalidProps = props.filter { !it.valid }
        if (invalidProps.isNotEmpty()) {
            throw ConfigException((listOf("Config '$name' does not pass validation! Issues found (${invalidProps.size}):") + invalidProps.map {
                "Property '${it.name}' with value '${it.value()}' | ${it.validation}"
            }).joinToString("\n"))
        }
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
        templateEngine.renderFile(template.get().asFile, target.get().asFile, mapOf(TEMPLATE_PROP to valuesSaved))
    }

    fun valueSaveGradleProperties() {
        valueSaveGradleProperties(project.file("gradle.properties.peb"), project.file("gradle.properties"))
    }

    fun valueSaveGradleProperties(template: File, target: File) = valueSave {
        val sectionContent = templateEngine.renderString(template.readText(), mapOf(TEMPLATE_PROP to valuesSaved))
        val section = TemplateSection(name, listOf("") + sectionContent.lines() + listOf(""))
        section.save(target)
    }

    private fun saveCapturedValues() {
        outputCapturedFile.get().asFile.let { file ->
            logger.info("Config '$name' is saving captured values to file '$file'")
            fileManager.writeYml(file, valuesCaptured)
        }
    }

    @Suppress("TooGenericExceptionCaught")
    private fun saveValuesUsingSavers() {
        if (valueSavers.isNotEmpty()) {
            logger.info("Config '$name' is saving values additionally (${valueSavers.size})")
            valueSavers.forEach { valueSaver ->
                try {
                    valueSaver()
                } catch (e: Exception) {
                    val message = "Config '$name' cannot save values properly!"
                    logger.warn("$message Cause: ${e.rootCause.message}")
                    logger.debug(message, e)
                }
            }
        }
    }

    init {
        labelAbbrs(
            "id", "url", "http", "https", "sftp", "ftp", "ssh", "aws", "az", "gcp",
            "ad", "tf", "tcp", "udp", "html", "css", "js", "sso"
        )
    }

    companion object {
        const val TEMPLATE_PROP = "config"
    }
}
