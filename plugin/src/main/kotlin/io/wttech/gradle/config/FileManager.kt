package io.wttech.gradle.config

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.SerializationFeature
import org.gradle.api.Project
import org.yaml.snakeyaml.DumperOptions
import org.yaml.snakeyaml.Yaml
import java.io.File
import java.io.InputStream
import java.io.OutputStream
import java.util.Properties

class FileManager(val project: Project) {

    val yaml = project.objects.property(Yaml::class.java).apply {
        convention(project.provider {
            Yaml(DumperOptions().apply {
                indent = 2
                isPrettyFlow = true
                defaultFlowStyle = DumperOptions.FlowStyle.BLOCK
            })
        })
        finalizeValueOnRead()
    }

    val json = project.objects.property(ObjectMapper::class.java).apply {
        convention(project.provider {
            ObjectMapper().enable(SerializationFeature.INDENT_OUTPUT)
        })
        finalizeValueOnRead()
    }

    inline fun <reified T> readYml(file: File): T = readFile(file) { yaml.get().load(it) }

    inline fun <reified T> readJson(file: File) = readFile(file) { json.get().readValue(it.bufferedReader(), T::class.java) }

    @Suppress("TooGenericExceptionCaught")
    inline fun <reified T> readFile(file: File, reader: (InputStream) -> T): T {
        val ext = file.extension.uppercase()
        if (!file.exists()) {
            throw ConfigException("Config $ext file does not exist '$file'!")
        }
        try {
            return file.inputStream().use(reader)
        } catch (e: Exception) {
            throw ConfigException("Config $ext file cannot be read '$file'!", e)
        }
    }

    fun writeJson(file: File, values: Map<String, Any?>) = writeFile(file) { json.get().writeValue(it, values) }

    fun writeYml(file: File, values: Map<String, Any?>) = writeFile(file) { yaml.get().dump(values, it.bufferedWriter()) }

    fun writeXml(file: File, values: Map<String, Any?>) = writeFile(file) {
        Properties().apply {
            putAll(values.mapValues { it.value.toString() })
            storeToXML(it, null)
        }
    }

    fun writeProperties(file: File, values: Map<String, Any?>) = writeFile(file) {
        Properties().apply {
            putAll(values.mapValues { it.value.toString() })
            store(it, null)
        }
    }

    @Suppress("TooGenericExceptionCaught")
    private fun writeFile(file: File, writer: (OutputStream) -> Unit) {
        val ext = file.extension.uppercase()
        try {
            file.apply { parentFile.mkdirs() }.outputStream().use(writer)
        } catch (e: Exception) {
            throw ConfigException("Config $ext file cannot be saved '$file'!", e)
        }
    }
}
