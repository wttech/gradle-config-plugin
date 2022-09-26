package com.wttech.gradle.config

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.SerializationFeature
import org.gradle.api.Project
import org.yaml.snakeyaml.DumperOptions
import org.yaml.snakeyaml.Yaml
import java.io.File

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
    }

    fun <T> readYml(file: File): T {
        if (!file.exists()) {
            throw ConfigException("Config YML file does not exist '$file'!")
        }

        try {
            return file.bufferedReader().use {
                yaml.get().load(it)
            }
        } catch (e: Exception) {
            throw ConfigException("Config YML file cannot be read '$file'!", e)
        }
    }

    inline fun <reified T> readJson(file: File): T {
        if (!file.exists()) {
            throw ConfigException("Config JSON file does not exist '$file'!")
        }

        try {
            return file.bufferedReader().use {
                json.get().readValue(it, T::class.java)
            }
        } catch (e: Exception) {
            throw ConfigException("Config JSON file cannot be read '$file'!", e)
        }
    }

    fun writeJson(file: File, values: Map<String, Any?> = mapOf()) {
        try {
            file.apply {
                parentFile.mkdirs()
            }.bufferedWriter().use { out ->
                json.get().writeValue(out, values)
            }
        } catch (e: Exception) {
            throw ConfigException("Config JSON file cannot be saved '$file'!", e)
        }
    }

    fun writeYml(file: File, values: Map<String, Any?> = mapOf()) {
        try {
            file.apply {
                parentFile.mkdirs()
            }.bufferedWriter().use { out ->
                yaml.get().dump(values, out)
            }
        } catch (e: Exception) {
            throw ConfigException("Config YML file cannot be saved '$file'!", e)
        }
    }
}