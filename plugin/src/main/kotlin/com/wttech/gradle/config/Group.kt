package com.wttech.gradle.config

class Group(val task: Config, val name: String) {

    val project = task.project

    val label = project.objects.property(String::class.java).convention(name.capitalize())

    val visible = project.objects.property(Boolean::class.java).convention(true)

    fun visible(predicate: () -> Boolean) {
        visible.set(project.provider { predicate() })
    }

    val props = project.objects.listProperty(Prop::class.java)

    fun prop(name: String, options: SingleProp.() -> Unit) {
        props.add(project.provider { SingleProp(this, name).apply(options) })
    }

    fun listProp(name: String, options: ListProp.() -> Unit) {
        props.add(project.provider { ListProp(this, name).apply(options) })
    }

    fun mapProp(name: String, options: MapProp.() -> Unit) {
        props.add(project.provider { MapProp(this, name).apply(options) })
    }

    override fun toString(): String = "Group(name='$name', visible=${visible.get()})"
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as Group

        if (name != other.name) return false

        return true
    }

    override fun hashCode() = name.hashCode()
}