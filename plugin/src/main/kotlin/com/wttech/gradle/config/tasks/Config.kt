package com.wttech.gradle.config.tasks

import com.wttech.gradle.config.CancelException
import com.wttech.gradle.config.ConfigException
import com.wttech.gradle.config.Definition
import com.wttech.gradle.config.InputMode
import org.gradle.api.DefaultTask
import org.gradle.api.tasks.Internal
import org.gradle.api.tasks.TaskAction
import org.gradle.api.tasks.options.Option

open class Config : DefaultTask() {

    @Internal
    @Option(option = "cli", description = "Capture input values using CLI")
    val cli = project.objects.property(Boolean::class.java)

    @Internal
    @Option(option = "gui", description = "Capture input values using GUI")
    val gui = project.objects.property(Boolean::class.java)

    @Internal
    @Option(option = "file", description = "Capture input values using file")
    val file = project.objects.property(String::class.java)

    @Internal
    @Option(option = "debug-config", description = "Prints additional information useful in debugging")
    val debug = project.objects.property(Boolean::class.java)

    @Internal
    val definition = project.objects.property(Definition::class.java).apply {
        finalizeValueOnRead()
    }

    @TaskAction
    fun capture() {
        val def = definition.get()

        if (debug.isPresent) def.debug.set(debug.get())
        if (file.isPresent) {
            def.inputMode.set(InputMode.FILE)
            def.inputFile.set(project.file(file.get()))
        }
        if (cli.isPresent) def.inputMode.set(InputMode.CLI)
        if (gui.isPresent) def.inputMode.set(InputMode.GUI)

        try {
            def.capture()
        } catch (e: CancelException) {
            val message = "Config '${def.name}' capture has been cancelled!"
            when {
                def.verbose.get() -> throw ConfigException(message)
                else -> logger.lifecycle(message)
            }
        }
    }

    init {
        description = "Captures input values then generates JSON/YML config files"
    }
}
