package com.wttech.gradle.config

abstract class Prop(val group: Group, val name: String) {

    private val project = group.project

    // CLI & GUI input

    val label = project.objects.property(String::class.java).apply {
        convention(project.provider { group.config.label(name) })
    }

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


    abstract fun value(): Any?

    abstract fun value(v: Any?)

    val singleValue: String? get() = when (this) {
        is SingleProp -> value()
        else -> throw ConfigException("Config prop '$name' is not a single!")
    }
    val listValue: List<String?>? get() = when (this) {
        is ListProp -> value()
        else -> throw ConfigException("Config prop '$name' is not a list!")
    }

    val mapValue: Map<String, Any?>? get() = when (this) {
        is MapProp -> value()
        else -> throw ConfigException("Config prop '$name' is not a map!")
    }

    fun other(propName: String) = group.config.prop(propName)

    fun otherValue(propName: String) = other(propName).singleValue

    fun otherListValue(propName: String) = other(propName).listValue

    fun otherMapValue(propName: String) = other(propName).mapValue

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