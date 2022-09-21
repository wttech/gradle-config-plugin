package com.wttech.gradle.config

class Prop(val group: Group, val name: String) {

    private val project = group.project

    val type = project.objects.property(PropType::class.java).apply {
        set(PropType.TEXT)
    }

    val value = project.objects.property(String::class.java)

    fun text() {
        type.set(PropType.TEXT)
    }
}