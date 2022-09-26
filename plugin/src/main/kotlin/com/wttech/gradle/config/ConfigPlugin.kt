package com.wttech.gradle.config

import org.gradle.api.Plugin
import org.gradle.api.Project

class ConfigPlugin : Plugin<Project> {

    override fun apply(project: Project) {
        project.extensions.create(ConfigExtension.NAME, ConfigExtension::class.java)
    }
}
