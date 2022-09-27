package com.wttech.gradle.config.tpl

import com.mitchellbosecke.pebble.PebbleEngine
import com.mitchellbosecke.pebble.lexer.Syntax
import com.mitchellbosecke.pebble.loader.FileLoader
import com.wttech.gradle.config.ConfigException
import org.gradle.api.Project
import java.io.File
import java.io.StringWriter
import java.util.regex.Pattern

class TemplateEngine(val project: Project) {

    private var engineOptions: PebbleEngine.Builder.() -> Unit = {}

    val props = project.objects.mapProperty(String::class.java, Any::class.java).apply {
        set(mapOf())
        putAll(mapOf(
            "env" to System.getenv(),
            "system" to System.getProperties().entries.fold(mutableMapOf<String, String>()) { props, prop ->
                props[prop.key.toString()] = prop.value.toString(); props
            },
            "project" to project.provider { project.properties },
        ))
    }

    fun engine(options: PebbleEngine.Builder.() -> Unit) {
        this.engineOptions = options
    }

    private val engine by lazy {
        PebbleEngine.Builder()
            .extension(TemplateExtension(project))
            .autoEscaping(false)
            .cacheActive(false)
            .strictVariables(true)
            .newLineTrimming(false)
            .loader(FileLoader())
            .syntax(
                Syntax.Builder()
                    .setEnableNewLineTrimming(false)
                    .setPrintOpenDelimiter(VAR_PREFIX)
                    .setPrintCloseDelimiter(VAR_SUFFIX)
                    .build()
            )
            .apply(engineOptions)
            .build()
    }

    @Suppress("TooGenericExceptionCaught")
    fun renderFile(template: File, target: File, props: Map<String, Any?> = mapOf()) {
        if (!template.exists()) {
            throw ConfigException("Template file does not exist '$template'!")
        }
        try {
            val propsCombined = this.props.get() + props
            target.parentFile.mkdirs()
            target.bufferedWriter().use { output ->
                engine.getTemplate(template.absolutePath).evaluate(output, propsCombined)
            }
        } catch (e: Exception) {
            throw ConfigException("Template file cannot be rendered '$template'!", e)
        }
    }

    @Suppress("TooGenericExceptionCaught")
    fun renderString(template: String, props: Map<String, Any?>): String {
        try {
            val renderer = StringWriter()
            val propsCombined = this.props.get() + props
            engine.getLiteralTemplate(template).evaluate(renderer, propsCombined)
            return renderer.toString()
        } catch (e: Exception) {
            throw ConfigException("Template string cannot be rendered!\n\n$template\n\n", e)
        }
    }

    fun parse(template: String): List<String> {
        val m = PROP_PATTERN.matcher(template)

        val result = mutableListOf<String>()
        while (m.find()) {
            val prop = m.group(1)

            result += if (prop.contains(PROP_DELIMITER)) {
                val parts = prop.split(PROP_DELIMITER).map { it.trim() }
                parts[0]
            } else {
                prop
            }
        }

        return result.map { it.trim() }
    }

    companion object {

        private val PROP_PATTERN = Pattern.compile("\\{\\{(.+?)}}")

        private const val PROP_DELIMITER = "|"

        private const val VAR_PREFIX = "{{"

        private const val VAR_SUFFIX = "}}"
    }
}
