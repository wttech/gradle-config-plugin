package com.wttech.gradle.config.tasks

import com.wttech.gradle.config.CaptureOptions
import com.wttech.gradle.config.Config
import com.wttech.gradle.config.InputMode
import org.gradle.api.DefaultTask
import org.gradle.api.tasks.Internal
import org.gradle.api.tasks.TaskAction
import org.gradle.api.tasks.options.Option

open class Config : DefaultTask() {

    @Internal
    @Option(option = "input", description = "Controls how input values are captured (gui|cli)")
    val input = project.objects.property(InputMode::class.java).apply {
        convention(InputMode.GUI)
        project.findProperty("config.input")?.toString()?.let { set(InputMode.of(it)) }
    }

    @Internal
    @Option(option = "debug-config", description = "Prints additional information useful in debugging")
    val debug = project.objects.property(Boolean::class.java).apply {
        convention(false)
        project.findProperty("config.debug")?.let { set(it.toString().toBoolean()) }
    }

    @Internal
    val definition = project.objects.property(Config::class.java).apply {
        finalizeValueOnRead()
    }

    @TaskAction
    fun capture() {
        definition.get().capture(CaptureOptions(input.get(), debug.get()))
    }

    init {
        description = "Captures input values then generates JSON/YML config files"
    }
}