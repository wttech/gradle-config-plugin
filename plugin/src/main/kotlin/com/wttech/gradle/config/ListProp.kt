package com.wttech.gradle.config

class ListProp(group: Group, name: String): Prop<List<String>>(group, name) {

    private val project = group.project

    val value = project.objects.listProperty(String::class.java)

    val options = project.objects.listProperty(String::class.java).apply {
        set(listOf())
    }

    override fun value() = value.orNull?.toList()
}