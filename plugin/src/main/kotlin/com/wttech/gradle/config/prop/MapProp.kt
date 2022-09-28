package com.wttech.gradle.config.prop

import com.wttech.gradle.config.Group
import com.wttech.gradle.config.Prop

class MapProp(group: Group, name: String) : Prop(group, name) {

    private val project = group.project

    private var valueMutator: (Map<String, Any?>?) -> Map<String, Any?>? = { it }

    fun valueDynamic(mutator: (value: Map<String, Any?>?) -> Map<String, Any?>?) {
        this.valueMutator = mutator
    }

    val value = project.objects.mapProperty(String::class.java, Any::class.java)

    override fun value() = valueMutator(value.orNull)

    fun values(values: Iterable<Pair<String, Any?>>) {
        value.set(values.toMap())
    }

    fun values(vararg values: Pair<String, Any?>) = values(values.asIterable())

    @Suppress("unchecked_cast")
    override fun value(v: Any?) = when (v) {
        is Map<*, *>? -> value.set(v as Map<String, Any?>)
        else -> project.logger.warn("Config value '$v' type of prop '$name' is not a map! Skipping it")
    }

    override fun hasValue() = value()?.isNotEmpty() ?: false

    fun notEmpty() = validate {
        if (!hasValue()) "Should not be empty"
        else null
    }

    init {
        notEmpty()
    }
}
