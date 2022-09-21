package com.wttech.gradle.config

class Group(val task: Config, val name: String) {

    val project = task.project

    val props = project.objects.listProperty(Prop::class.java)

    fun prop(name: String, options: Prop.() -> Unit) {
        props.add(project.provider { Prop(this, name).apply(options) })
    }

    private var visiblePredicate: () -> Boolean = { true }

    fun visible(predicate: () -> Boolean) {
        this.visiblePredicate = predicate
    }

    val visible get() = visiblePredicate()
}