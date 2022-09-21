package com.wttech.gradle.config.value

import com.wttech.gradle.config.Prop
import com.wttech.gradle.config.Value

class Bool(val prop: Prop): Value<Boolean> {

    override val value = prop.project.objects.property(Boolean::class.java)
}