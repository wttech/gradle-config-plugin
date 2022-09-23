package com.wttech.gradle.config

typealias SingleType = String

class SingleProp(group: Group, name: String): Prop(group, name) {

    private val project = group.project

    val options = project.objects.listProperty(String::class.java).apply {
        set(listOf())
    }

    fun options(options: Iterable<String>) {
        this.options.set(options)
    }
    fun options(vararg options: String) = options(options.asIterable())

    val optionsStyle = project.objects.property(OptionsStyle::class.java).convention(OptionsStyle.SELECT)

    fun checkbox() {
        optionsStyle.set(OptionsStyle.CHECKBOX)
    }

    fun select() {
        optionsStyle.set(OptionsStyle.SELECT)
    }
    enum class OptionsStyle {
        CHECKBOX,
        SELECT
    }

    private var valueMutator: (SingleType?) -> SingleType? = { it }

    fun valueDynamic(mutator: (value: SingleType?) -> SingleType?) {
        this.valueMutator = mutator
    }

    @Suppress("NULLABILITY_MISMATCH_BASED_ON_JAVA_ANNOTATIONS")
    private val value = project.objects.property(String::class.java).convention(options.map { it.firstOrNull() })

    override fun value() = valueMutator(value.orNull)

    override fun value(v: Any?) {
        value.set(v?.toString())
    }

    fun int() = value()?.toInt()

    fun boolean() = value()?.toBoolean()
}