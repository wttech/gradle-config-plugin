package com.wttech.gradle.config

import com.wttech.gradle.config.prop.ListProp
import com.wttech.gradle.config.prop.MapProp
import com.wttech.gradle.config.prop.StringProp

abstract class Prop(val group: Group, val name: String) {

    private val project = group.project

    // CLI & GUI input

    val label = project.objects.property(String::class.java).apply {
        convention(project.provider { group.definition.composeLabel(name) })
    }

    fun label(text: String) {
        label.set(text)
    }

    val description = project.objects.property(String::class.java)

    fun description(text: String) {
        description.set(text)
    }

    // GUI input only

    val visible = project.objects.property(Boolean::class.java).convention(true)

    fun visible(predicate: () -> Boolean) {
        visible.set(project.provider { predicate() })
    }

    fun const() {
        visible.set(false)
        visible.finalizeValue()
    }

    val enabled = project.objects.property(Boolean::class.java).convention(true)

    fun enabled(predicate: () -> Boolean) {
        enabled.set(project.provider { predicate() })
    }

    fun enabled() {
        enabled.set(true)
    }

    fun disabled() {
        enabled.set(false)
    }

    private var validator: (() -> String?) = { null }

    val validation get() = validator()

    val valid get() = !visible.get() || !enabled.get() || (validation == null)

    fun validate(validator: () -> String?) {
        this.validator = validator
    }

    open fun required() = validate {
        if (value() == null) "Value is required"
        else null
    }

    open fun optional() = validate { null }

    abstract fun value(): Any?

    open fun valueSaved() = value()

    abstract fun value(v: Any?)

    val string: StringProp
        get() = when (this) {
            is StringProp -> this
            else -> throw ConfigException("Config prop '$name' is not a string!")
        }
    val list: ListProp
        get() = when (this) {
            is ListProp -> this
            else -> throw ConfigException("Config prop '$name' is not a list!")
        }

    val map: MapProp
        get() = when (this) {
            is MapProp -> this
            else -> throw ConfigException("Config prop '$name' is not a map!")
        }

    val stringValue get() = string.value()

    val listValue get() = list.value()

    val mapValue get() = map.value()

    fun other(propName: String) = group.definition.getProp(propName)

    fun otherValue(propName: String) = other(propName).value()

    fun otherStringValue(propName: String) = other(propName).string.value()

    fun otherListValue(propName: String) = other(propName).list.value()

    fun otherMapValue(propName: String) = other(propName).map.value()

    override fun toString() = "Prop(group=${group.name}, name=$name, value=${value()}, visible=${visible.get()}, enabled=${enabled.get()})"

    override fun hashCode(): Int {
        var result = group.name.hashCode()
        result = 31 * result + name.hashCode()
        result = 31 * result + (label.orNull?.hashCode() ?: 0)
        result = 31 * result + (description.orNull?.hashCode() ?: 0)
        result = 31 * result + (visible.orNull?.hashCode() ?: 0)
        result = 31 * result + (enabled.orNull?.hashCode() ?: 0)
        return result
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as Prop

        if (group.name != other.group.name) return false
        if (name != other.name) return false
        if (label.orNull != other.label.orNull) return false
        if (description.orNull != other.description.orNull) return false
        if (visible.orNull != other.visible.orNull) return false
        if (enabled.orNull != other.enabled.orNull) return false

        return true
    }
}
