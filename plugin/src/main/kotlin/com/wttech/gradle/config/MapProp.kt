package com.wttech.gradle.config

class MapProp(group: Group, name: String): Prop<Map<String, Any?>>(group, name) {

    private val project = group.project

    val prop = project.objects.mapProperty(String::class.java, Any::class.java)

    override var value: Map<String, Any?>?
        get() = prop.orNull
        set(value) { prop.set(value) }

}