package com.wttech.gradle.config

import com.wttech.gradle.config.gui.Dialog
import org.gradle.api.DefaultTask
import org.gradle.api.tasks.Internal
import org.gradle.api.tasks.TaskAction
import org.yaml.snakeyaml.Yaml

open class Config : DefaultTask() {

    @get:Internal
    val config by lazy { project.extensions.getByType(ConfigExtension::class.java) }

    @Internal
    val inputMode = project.objects.property(InputMode::class.java).apply {
        set(InputMode.GUI)
    }

    @Internal
    val outputFile = project.objects.fileProperty().apply {
        set(project.layout.projectDirectory.file(".gradle/config/$name.yml"))
    }

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
    val props get() = groups.get().flatMap { it.props.get() }
    fun prop(propName: String) = props.firstOrNull { it.name == propName }
        ?: throw ConfigException("Prop '$propName' is not defined!")

    fun value(propName: String) = prop(propName).value

    @get:Internal
    val values get() = props.associate {
        println(it)
        it.name to it.valueHolder.value.orNull
    }

    @TaskAction
    fun evaluate() {
        // Freeze lazy definitions
        groups.finalizeValueOnRead()
        groups.get().forEach { it.props.finalizeValueOnRead() }

        // Capture values
        var valuesCaptured: Map<String, Any?> = mapOf()
        when (inputMode.get()) {
            InputMode.GUI -> {
                Dialog.render(this) {
                    valuesCaptured = values
                }
            }
            InputMode.CLI -> TODO()
            else -> throw ConfigException("Config input mode is not specified!")
        }

        println(Yaml().dump(valuesCaptured))
    }

    init {
        if (outputLoaded.get()) { // TODO determine when to load props
            loadOutputFile()
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