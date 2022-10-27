package io.wttech.gradle.config

import org.gradle.api.Project
import java.io.File

class PropertyLoader(val project: Project) {

    private val logger = project.logger

    private val fileManager by lazy { FileManager(project) }

    private val extraProperties by lazy { project.extensions.extraProperties }

    /**
     * Override already defined properties (both gradle.properties and provided via CLI)
     */
    val override = project.objects.property(Boolean::class.java).apply {
        convention(false)
    }

    fun load(file: File) {
        if (!file.exists()) {
            logger.debug("Properties not loaded as properties file does not exist '$file'!")
            return
        }

        logger.info("Loading properties from file '$file'")
        fileManager.readProperties(file).forEach { (name, value) ->
            when {
                name.startsWith(SYSTEM_PROP_PREFIX) -> {
                    System.setProperty(name.substringAfter(SYSTEM_PROP_PREFIX), value.toString())
                }
                else -> {
                    if (override.get() || !extraProperties.has(name)) {
                        extraProperties.set(name, value)
                    }
                }
            }
        }
    }

    fun load(filePath: String) = load(project.file(filePath))

    fun loadFrom(dirPath: String) = loadFrom(project.file(dirPath))

    fun loadFrom(dir: File) {
        if (!dir.exists()) {
            logger.debug("Properties not loaded as properties directory does not exist '$dir'!")
            return
        }

        project.fileTree(dir)
            .matching { it.include("**/*.properties") }
            .sorted()
            .forEach { load(it) }
    }

    companion object {

        const val SYSTEM_PROP_PREFIX = "systemProp."
    }
}
