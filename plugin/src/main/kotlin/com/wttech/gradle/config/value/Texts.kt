package com.wttech.gradle.config.value

import com.wttech.gradle.config.Prop
import com.wttech.gradle.config.Value

class Texts(val prop: Prop): Value<List<String>> {

    override val value = prop.project.objects.listProperty(String::class.java)

    val options = prop.project.objects.listProperty(String::class.java).apply {
        set(listOf())
    }
}