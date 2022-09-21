package com.wttech.gradle.config.tasks

import com.wttech.gradle.config.ConfigExtension
import com.wttech.gradle.config.InputMode
import com.wttech.gradle.config.Property
import org.gradle.api.DefaultTask
import org.gradle.api.tasks.TaskAction
import org.yaml.snakeyaml.Yaml

class Config : DefaultTask() {

    val config by lazy { project.extensions.getByType(ConfigExtension::class.java) }

    val inputMode = project.objects.property(InputMode::class.java).apply {
        set(InputMode.GUI)
    }

    val outputFile = project.objects.fileProperty().apply {
        set(project.layout.projectDirectory.file("var/$name.yml"))
    }

    val outputLoaded = project.objects.property(Boolean::class.java).apply {
        set(false)
    }

    val properties = project.objects.listProperty(Property::class.java)

    fun property(name: String, options: Property.() -> Unit) {
        properties.add(project.provider { Property(this, name).apply(options) })
    }

    @TaskAction
    fun evaluate() {
        when (inputMode.get()) {
            InputMode.GUI -> TODO()
            InputMode.CLI -> TODO()
        }
    }

    init {
        project.afterEvaluate {
            if (outputLoaded.get()) {
                loadOutputFile()
            }
        }
    }

    private fun loadOutputFile() {
        val file = outputFile.get().asFile
        if (file.exists()) {
            val values: Map<String, Any?> = file.bufferedReader().use { Yaml().load(it) }
            values.forEach { (k, v) ->
                project.rootProject.extensions.extraProperties.set(k, v)
            }
        }
    }
}