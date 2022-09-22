package com.wttech.gradle.config

import com.wttech.gradle.config.util.capitalWords

abstract class Prop<V: Any>(val group: Group, val name: String) {

    private val project = group.project

    // CLI & GUI input

    val label = project.objects.property(String::class.java).convention(name.capitalWords())

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

    abstract fun value(v: Any?)

    abstract fun valueBy(provider: () -> V?)

    fun other(propName: String) = group.config.prop(propName)

    fun otherValue(propName: String) = other(propName).value()

    override fun toString() = "Prop(group=${group.name}, name=$name, value=${value()}, visible=${visible.get()}, enabled=${enabled.get()})"

    override fun hashCode(): Int {
        var result = group.hashCode()
        result = 31 * result + name.hashCode()
        result = 31 * result + (label.orNull?.hashCode() ?: 0)
        result = 31 * result + (description.orNull?.hashCode() ?: 0)
        result = 31 * result + (visible.orNull?.hashCode() ?: 0)
        result = 31 * result + (enabled.orNull?.hashCode() ?: 0)
        return result
    }
}