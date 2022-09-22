package com.wttech.gradle.config

abstract class Prop<V: Any>(val group: Group, val name: String) {

    private val project = group.project

    // CLI & GUI input

    val label = project.objects.property(String::class.java).convention(name.capitalize())

    val description = project.objects.property(String::class.java)

    // GUI input only

    val visible = project.objects.property(Boolean::class.java).convention(true)

    fun visible(predicate: () -> Boolean) {
        visible.set(project.provider { predicate() })
    }

    val enabled = project.objects.property(Boolean::class.java).convention(true)

    fun enabled(predicate: () -> Boolean) {
        enabled.set(project.provider { predicate() })
    }

    abstract fun value(): V?

    override fun toString() = "Prop(group=${group.name}, name=$name, value=${value()})"
}