package com.wttech.gradle.config.prop

import com.wttech.gradle.config.Group
import com.wttech.gradle.config.Prop

class StringProp(group: Group, name: String) : Prop(group, name) {

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
        valueTypeBool()
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

    fun valueTypeString() { valueType.set(ValueType.STRING) }
    fun valueTypeInt() { valueType.set(ValueType.INT) }
    fun valueTypeDouble() { valueType.set(ValueType.DOUBLE) }
    fun valueTypeBool() {
        valueType.set(ValueType.BOOL)
        options.set(listOf(true.toString(), false.toString()))
    }

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
    val value = project.objects.property(String::class.java).convention(options.map { it.firstOrNull() })

    override fun value() = valueDynamic(value.orNull)

    override fun valueSaved() = valueSaved(value())

    override fun value(v: Any?) {
        value.set(v?.toString())
    }

    override fun required() = validate { "Value is required".takeIf { value().isNullOrBlank() } }

    fun regex(regex: String) = validate { "Should match regex '$regex'".takeUnless { checkRegex(regex) } }

    fun alphanumeric() = validate { "Should be alphanumeric".takeUnless { checkRegex("^[a-zA-Z0-9]+$") } }

    fun numeric() = validate { "Should be numeric".takeUnless { checkRegex("^[0-9]+$") } }

    fun alpha() = validate { "Should contain only alphabetic characters".takeUnless { checkRegex("^[a-zA-Z0-9]+$") } }

    fun checkRegex(regex: String) = value().orEmpty().let { Regex(regex).matches(it) }
}
