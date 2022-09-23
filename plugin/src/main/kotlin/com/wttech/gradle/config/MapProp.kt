package com.wttech.gradle.config

typealias MapType = Map<String, Any>

class MapProp(group: Group, name: String): Prop<MapType>(group, name) {

    private val project = group.project

    private var mutator: (MapType?) -> MapType? = { it }

    fun mutate(mutator: (value: MapType?) -> MapType?) {
        this.mutator = mutator
    }

    val value = project.objects.mapProperty(String::class.java, Any::class.java)

    override fun value() = mutator(value.orNull)

    @Suppress("unchecked_cast")
    override fun value(v: Any?) = when (v) {
        is Map<*, *>? -> value.set(v as MapType)
        else -> project.logger.warn("Config value '$v' type of prop '$name' is not a map! Skipping it")
    }
}