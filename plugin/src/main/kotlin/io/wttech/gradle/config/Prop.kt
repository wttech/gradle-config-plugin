package io.wttech.gradle.config

import io.wttech.gradle.config.prop.ListProp
import io.wttech.gradle.config.prop.MapProp
import io.wttech.gradle.config.prop.StringProp
import io.wttech.gradle.config.util.removeCommonWords

abstract class Prop(val group: Group, val name: String) {

    private val project = group.project

    // CLI & GUI input

    val label = project.objects.property(String::class.java).apply {
        convention(project.provider { proposeLabel() })
    }
    fun proposeLabel(): String {
        val propLabel = group.definition.composeLabel(name)
        val groupLabel = group.label.get()
        return propLabel.removeCommonWords(groupLabel)
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

    internal var captured = true

    fun const() {
        captured = false
        visible.set(false)
        enabled.set(false)
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

    val required = project.objects.property(Boolean::class.java).convention(true)

    fun required(predicate: () -> Boolean) {
        required.set(project.provider { predicate() })
    }

    fun required() {
        required.set(true)
    }

    fun optional() {
        required.set(false)
    }

    private var validator: (() -> String?) = {
        if (!hasValue()) "Should has a value"
        else null
    }

    val validation: String?
        get() = when {
            !group.visible.get() || !visible.get() -> null
            required.get() || (!required.get() && hasValue()) -> validator()
            else -> null
        }

    val valid get() = validation == null

    fun validate(validator: () -> String?) {
        this.validator = validator
    }

    abstract fun value(): Any?

    open fun valueSaved() = value()

    abstract fun valueSet(v: Any?)
    fun valueDefault(v: Any?) = valueSet(v)
    fun default(v: Any?) = valueDefault(v)

    abstract fun hasValue(): Boolean

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

    override fun toString() = "Prop(group=${group.name}, name=$name, value=${value()}, visible=${visible.get()}, enabled=${enabled.get()}, valid=$valid)"

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
