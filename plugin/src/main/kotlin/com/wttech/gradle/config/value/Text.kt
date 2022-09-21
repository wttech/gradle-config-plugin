package com.wttech.gradle.config.value

import com.wttech.gradle.config.Prop
import com.wttech.gradle.config.Value

class Text(val prop: Prop): Value<String> {

    override val value = prop.project.objects.property(String::class.java)

    val options = prop.project.objects.listProperty(String::class.java).apply {
        set(listOf())
    }
}