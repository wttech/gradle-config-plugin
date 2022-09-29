package com.wttech.gradle.config.dsl

import com.wttech.gradle.config.ConfigExtension
import org.gradle.api.Project
import org.gradle.api.Task

val Project.config get() = ConfigExtension.of(this)

fun Task.requiresConfig(name: String = ConfigExtension.DEFAULT_NAME) = ConfigExtension.of(project).requiredBy(this, name)
