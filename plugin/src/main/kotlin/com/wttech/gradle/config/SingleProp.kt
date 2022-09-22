package com.wttech.gradle.config

class SingleProp(group: Group, name: String): Prop<String>(group, name) {

    private val project = group.project

    val value = project.objects.property(String::class.java)

    override fun value() = value.orNull

    fun int() = value()?.toInt()

    fun boolean() = value()?.toBoolean()

    val options = project.objects.listProperty(String::class.java).apply {
        set(listOf())
    }

    fun options(options: Iterable<String>) {
        this.options.set(options)
    }

    fun options(vararg options: String) = options(options.asIterable())
}