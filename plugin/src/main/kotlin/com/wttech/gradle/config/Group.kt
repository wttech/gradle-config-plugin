package com.wttech.gradle.config

class Group(val task: Config, val name: String) {

    val project = task.project

    val label = project.objects.property(String::class.java).convention(name.capitalize())

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

    private var visiblePredicate: () -> Boolean = { true }

    fun visible(predicate: () -> Boolean) {
        this.visiblePredicate = predicate
    }

    val visible get() = visiblePredicate()
}