package com.wttech.gradle.config

abstract class Prop(val group: Group, val name: String) {

    private val project = group.project

    // CLI & GUI input

    val label = project.objects.property(String::class.java).apply {
        convention(project.provider { group.config.composeLabel(name) })
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

    val enabled = project.objects.property(Boolean::class.java).convention(true)

    fun enabled(predicate: () -> Boolean) {
        enabled.set(project.provider { predicate() })
    }


    abstract fun value(): Any?

    open fun valueSaved() = value()

    abstract fun value(v: Any?)

    val single: SingleProp get() = when (this) {
        is SingleProp -> this
        else -> throw ConfigException("Config prop '$name' is not a single!")
    }
    val list: ListProp get() = when (this) {
        is ListProp -> this
        else -> throw ConfigException("Config prop '$name' is not a list!")
    }

    val map: MapProp get() = when (this) {
        is MapProp -> this
        else -> throw ConfigException("Config prop '$name' is not a map!")
    }

    val singleValue get() = single.value()

    val listValue get() = list.value()

    val mapValue get() = map.value()

    fun other(propName: String) = group.config.getProp(propName)

    fun otherValue(propName: String) = other(propName).single.value()

    fun otherListValue(propName: String) = other(propName).list.value()

    fun otherMapValue(propName: String) = other(propName).map.value()

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