package com.wttech.gradle.config

class ListProp(group: Group, name: String): Prop<List<String>>(group, name) {

    private val project = group.project

    val value = project.objects.listProperty(String::class.java)

    override fun value() = value.orNull

    override fun valueBy(provider: () -> List<String>?) {
        value.set(project.provider(provider))
    }

    @Suppress("unchecked_cast")
    override fun value(v: Any?) = when (v) {
        is List<*>? -> value.set(v as List<String>?)
        else -> project.logger.warn("Config value '$v' type of prop '$name' is not a list! Skipping it")
    }

    val options = project.objects.listProperty(String::class.java).apply {
        set(listOf())
    }
}