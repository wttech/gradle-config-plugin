package com.wttech.gradle.config

class SingleProp(group: Group, name: String): Prop<String>(group, name) {

    private val project = group.project

    val options = project.objects.listProperty(String::class.java).apply {
        set(listOf())
    }

    fun options(options: Iterable<String>) {
        this.options.set(options)
    }
    fun options(vararg options: String) = options(options.asIterable())

    @Suppress("NULLABILITY_MISMATCH_BASED_ON_JAVA_ANNOTATIONS")
    val value = project.objects.property(String::class.java).convention(options.map { it.firstOrNull() })

    override fun value() = value.orNull

    override fun value(v: Any?) {
        value.set(v?.toString())
    }

    override fun valueBy(provider: () -> String?) {
        value.set(project.provider(provider))
    }

    fun int() = value()?.toInt()

    fun boolean() = value()?.toBoolean()
}