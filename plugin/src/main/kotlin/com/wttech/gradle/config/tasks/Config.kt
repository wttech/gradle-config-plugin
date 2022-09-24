package com.wttech.gradle.config.tasks

import com.wttech.gradle.config.Config
import org.gradle.api.DefaultTask
import org.gradle.api.tasks.Internal
import org.gradle.api.tasks.TaskAction

open class Config : DefaultTask() {

    @Internal
    val definition = project.objects.property(Config::class.java).apply { finalizeValueOnRead() }

    @TaskAction
    fun process() {
        definition.get().process()
    }

    init {
        description = "Captures input values then generates JSON/YML config files"
    }
}