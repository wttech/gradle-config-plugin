package com.wttech.gradle.config

typealias SingleType = String

class SingleProp(group: Group, name: String): Prop<SingleType>(group, name) {

    private val project = group.project

    val options = project.objects.listProperty(String::class.java).apply {
        set(listOf())
    }

    fun options(options: Iterable<String>) {
        this.options.set(options)
    }
    fun options(vararg options: String) = options(options.asIterable())

    private var mutator: (SingleType?) -> SingleType? = { it }

    fun mutate(mutator: (value: SingleType?) -> SingleType?) {
        this.mutator = mutator
    }

    @Suppress("NULLABILITY_MISMATCH_BASED_ON_JAVA_ANNOTATIONS")
    private val value = project.objects.property(String::class.java).convention(options.map { it.firstOrNull() })

    override fun value() = mutator(value.orNull)

    override fun value(v: Any?) {
        value.set(v?.toString())
    }

    fun int() = value()?.toInt()

    fun boolean() = value()?.toBoolean()
}