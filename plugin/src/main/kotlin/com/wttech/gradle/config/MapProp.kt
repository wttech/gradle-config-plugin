package com.wttech.gradle.config

class MapProp(group: Group, name: String): Prop<Map<String, Any?>>(group, name) {

    private val project = group.project

    val value = project.objects.mapProperty(String::class.java, Any::class.java)

    override fun value() = value.orNull?.toMap()
}