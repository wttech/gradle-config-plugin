package com.wttech.gradle.config.tasks

import com.wttech.gradle.config.Definition
import com.wttech.gradle.config.InputMode
import org.gradle.api.DefaultTask
import org.gradle.api.tasks.Internal
import org.gradle.api.tasks.TaskAction
import org.gradle.api.tasks.options.Option

open class Config : DefaultTask() {

    @Internal
    @Option(option = "cli", description = "Capture values using CLI")
    val cli = project.objects.property(Boolean::class.java)

    @Internal
    @Option(option = "gui", description = "Capture values using GUI")
    val gui = project.objects.property(Boolean::class.java)

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
        if (cli.isPresent) def.inputMode.set(InputMode.CLI)
        if (gui.isPresent) def.inputMode.set(InputMode.GUI)
        def.capture()
    }

    init {
        description = "Captures input values then generates JSON/YML config files"
    }
}