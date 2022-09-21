package com.wttech.gradle.config

import com.wttech.gradle.config.value.Text

class Prop(val group: Group, val name: String) {

    val project = group.project

    internal var valueHolder: Value<out Any> = Text(this)

    fun text(options: Text.() -> Unit) {
        valueHolder = Text(this).apply(options)
    }

    val value get() = valueHolder.value.orNull

    override fun toString() = "Prop(name=$name, valueHolder=$valueHolder)"
}