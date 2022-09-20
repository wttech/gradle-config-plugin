package com.wttech.gradle.config

import org.gradle.api.Project
import org.gradle.api.Plugin

class ConfigPlugin: Plugin<Project> {

    override fun apply(project: Project) {
        project.tasks.register("greeting") { task ->
            task.doLast {
                println("Hello from plugin 'com.wttech.config'")
            }
        }
    }
}
