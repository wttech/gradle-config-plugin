package com.wttech.gradle.config

class MapProp(group: Group, name: String): Prop<Map<String, Any?>>(group, name) {

    private val project = group.project

    val value = project.objects.mapProperty(String::class.java, Any::class.java)

    override fun value() = value.orNull

    @Suppress("unchecked_cast")
    override fun value(v: Any?) = when (v) {
        is Map<*, *>? -> value.set(v as Map<String, Any?>)
        else -> project.logger.warn("Config value '$v' type of prop '$name' is not a map! Skipping it")
    }

    override fun valueBy(provider: () -> Map<String, Any?>?) {
        value.set(project.provider(provider))
    }
}