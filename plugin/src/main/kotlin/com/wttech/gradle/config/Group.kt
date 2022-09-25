package com.wttech.gradle.config

import com.wttech.gradle.config.prop.ConstProp
import com.wttech.gradle.config.prop.ListProp
import com.wttech.gradle.config.prop.MapProp
import com.wttech.gradle.config.prop.SingleProp

class Group(val definition: Definition, val name: String) {

    val project = definition.project

    val label = project.objects.property(String::class.java).apply {
        convention(project.provider { definition.composeLabel(name) })
    }

    fun label(text: String) {
        label.set(text)
    }

    val description = project.objects.property(String::class.java)

    fun description(text: String) {
        description.set(text)
    }

    val visible = project.objects.property(Boolean::class.java).convention(true)

    fun visible(predicate: () -> Boolean) {
        visible.set(project.provider { predicate() })
    }

    val enabled = project.objects.property(Boolean::class.java).convention(true)

    fun enabled(predicate: () -> Boolean) {
        enabled.set(project.provider { predicate() })
    }

    val props = project.objects.listProperty<Prop>(Prop::class.java)

    fun prop(name: String, options: SingleProp.() -> Unit = {}) {
        props.add(project.provider { SingleProp(this, name).apply(options) })
    }

    fun listProp(name: String, options: ListProp.() -> Unit = {}) {
        props.add(project.provider { ListProp(this, name).apply(options) })
    }

    fun mapProp(name: String, options: MapProp.() -> Unit = {}) {
        props.add(project.provider { MapProp(this, name).apply(options) })
    }

    private fun constProp(name: String, options: ConstProp.() -> Unit = {}) {
        props.add(project.provider { ConstProp(this, name).apply(options) })
    }

    fun staticProp(name: String, value: Any?) = constProp(name) {
        value(value)
    }

    fun dynamicProp(name: String, valueProvider: Prop.() -> Any?) = constProp(name) {
        valueDynamic { valueProvider() }
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as Group

        if (name != other.name) return false

        return true
    }

    override fun toString(): String = "Group(name='$name', visible=${visible.get()}, enabled=${enabled.get()})"
    override fun hashCode(): Int {
        var result = name.hashCode()
        result = 31 * result + (label.orNull?.hashCode() ?: 0)
        result = 31 * result + (visible.orNull?.hashCode() ?: 0)
        result = 31 * result + (enabled.orNull?.hashCode() ?: 0)
        return result
    }
}