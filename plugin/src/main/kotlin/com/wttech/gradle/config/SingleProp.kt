package com.wttech.gradle.config

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

    enum class OptionsStyle {
        CHECKBOX,
        SELECT
    }

    fun checkbox() {
        optionsStyle.set(OptionsStyle.CHECKBOX)
        valueType.set(ValueType.BOOL)
    }

    fun select() {
        optionsStyle.set(OptionsStyle.SELECT)
    }

    val valueType = project.objects.property(ValueType::class.java).convention(ValueType.STRING)

    enum class ValueType {
        STRING,
        INT,
        DOUBLE,
        BOOL
    }

    fun valueString() { valueType.set(ValueType.STRING) }
    fun valueInt() { valueType.set(ValueType.INT) }
    fun valueDouble() { valueType.set(ValueType.DOUBLE) }
    fun valueBool() { valueType.set(ValueType.BOOL) }

    private var valueDynamic: (String?) -> String? = { it }

    fun valueDynamic(mutator: (value: String?) -> String?) {
        this.valueDynamic = mutator
    }

    private var valueSaved: (String?) -> Any? = { v ->
        when (valueType.get()) {
            ValueType.BOOL -> v?.toBoolean()
            ValueType.INT -> v?.toInt()
            ValueType.DOUBLE -> v?.toDouble()
            else -> v
        }
    }

    fun valueSaved(processor: (value: String?) -> String?) {
        this.valueSaved = processor
    }

    @Suppress("NULLABILITY_MISMATCH_BASED_ON_JAVA_ANNOTATIONS")
    private val value = project.objects.property(String::class.java).convention(options.map { it.firstOrNull() })

    override fun value() = valueDynamic(value.orNull)

    override fun valueSaved() = valueSaved(value())

    override fun value(v: Any?) {
        value.set(v?.toString())
    }
}