package com.wttech.gradle.config.prop

import com.wttech.gradle.config.Group
import com.wttech.gradle.config.Prop

class ListProp(group: Group, name: String): Prop(group, name) {

    private val project = group.project

    private var valueMutator: (List<String?>?) -> List<String?>? = { it }

    fun valueDynamic(function: (value: List<String?>?) -> List<String?>?) {
        this.valueMutator = function
    }

    val value = project.objects.listProperty(String::class.java)

    override fun value() = valueMutator(value.orNull)

    @Suppress("unchecked_cast")
    override fun value(v: Any?) = when (v) {
        is List<*>? -> value.set(v as List<String?>?)
        else -> project.logger.warn("Config value '$v' type of prop '$name' is not a list! Skipping it")
    }

    fun values(values: Iterable<String?>) {
        value.set(values)
    }

    fun values(vararg values: String?) = values(values.asIterable())

    val options = project.objects.listProperty(String::class.java).apply {
        set(listOf())
    }
}