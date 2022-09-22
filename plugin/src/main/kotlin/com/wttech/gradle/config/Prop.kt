package com.wttech.gradle.config

abstract class Prop<V: Any>(val group: Group, val name: String) {

    private val project = group.project

    val label = project.objects.property(String::class.java).convention(name.capitalize())

    val visible = project.objects.property(Boolean::class.java).convention(true)

    fun visible(predicate: () -> Boolean) {
        visible.set(project.provider { predicate() })
    }

    val description = project.objects.property(String::class.java)


    override fun toString() = "Prop(group=${group.name}, name=$name, value=$value)"

    abstract var value: V?
}