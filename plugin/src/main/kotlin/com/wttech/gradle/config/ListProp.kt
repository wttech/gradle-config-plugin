package com.wttech.gradle.config

class ListProp(group: Group, name: String): Prop<List<String>>(group, name) {

    private val project = group.project

    val prop = project.objects.listProperty(String::class.java)

    override var value: List<String>?
        get() = prop.orNull
        set(value) { prop.set(value) }

    val options = project.objects.listProperty(String::class.java).apply {
        set(listOf())
    }
}