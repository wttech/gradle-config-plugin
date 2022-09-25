package com.wttech.gradle.config.prop

import com.wttech.gradle.config.Group
import com.wttech.gradle.config.Prop

class ConstProp(group: Group, name: String): Prop(group, name) {

    private var valueProvider: () -> Any? = {}

    override fun value() = valueProvider()

    override fun value(v: Any?) {
        this.valueProvider = { v }
    }

    fun valueDynamic(v: () -> Any?) {
        this.valueProvider = v
    }
}
