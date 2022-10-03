package io.wttech.gradle.config

import io.wttech.gradle.config.prop.ListProp
import io.wttech.gradle.config.prop.MapProp
import io.wttech.gradle.config.prop.StringProp

class Group(val definition: Definition, val name: String) {

    val project = definition.project

    val label = project.objects.property(String::class.java).apply {
        convention(project.provider { proposeLabel() })
    }

    fun proposeLabel() = definition.composeLabel(name)

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

    fun prop(name: String, options: StringProp.() -> Unit = {}) = stringProp(name, options)

    fun const(name: String, value: String?) = stringConst(name, value)
    fun const(name: String, valueProvider: () -> String?) = stringConst(name, valueProvider)

    fun stringProp(name: String, options: StringProp.() -> Unit = {}) {
        props.add(project.provider { StringProp(this, name).apply(options) })
    }

    fun stringConst(name: String, value: String?) = stringProp(name) { const(); setValue(value) }
    fun stringConst(name: String, valueProvider: () -> String?) = stringProp(name) { const(); value.set(project.provider(valueProvider)) }

    fun listProp(name: String, options: ListProp.() -> Unit = {}) {
        props.add(project.provider { ListProp(this, name).apply(options) })
    }

    fun listConst(name: String, value: Any?) = listProp(name) { const(); setValue(value) }
    fun listConst(name: String, valueProvider: () -> List<String>?) = listProp(name) { const(); value.set(project.provider(valueProvider)) }

    fun mapProp(name: String, options: MapProp.() -> Unit = {}) {
        props.add(project.provider { MapProp(this, name).apply(options) })
    }

    fun mapConst(name: String, value: Any?) = mapProp(name) { const(); setValue(value) }
    fun mapConst(name: String, valueProvider: () -> Map<String, Any?>?) = mapProp(name) { const(); value.set(project.provider(valueProvider)) }

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
