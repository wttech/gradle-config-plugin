package com.wttech.gradle.config

abstract class Prop<V: Any>(val group: Group, val name: String) {

    private val project = group.project

    val label = project.objects.property(String::class.java).convention(name.capitalize())

    val description = project.objects.property(String::class.java)

    abstract fun value(): V?

    override fun toString() = "Prop(group=${group.name}, name=$name, value=${value()}"
}