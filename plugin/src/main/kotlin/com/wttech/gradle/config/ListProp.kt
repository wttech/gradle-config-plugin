package com.wttech.gradle.config

typealias ListType = List<String?>
class ListProp(group: Group, name: String): Prop<ListType>(group, name) {

    private val project = group.project

    protected var mutator: (ListType?) -> ListType? = { it }

    fun mutate(mutator: (value: ListType?) -> ListType?) {
        this.mutator = mutator
    }

    val value = project.objects.listProperty(String::class.java)

    override fun value() = mutator(value.orNull)

    @Suppress("unchecked_cast")
    override fun value(v: Any?) = when (v) {
        is List<*>? -> value.set(v as ListType?)
        else -> project.logger.warn("Config value '$v' type of prop '$name' is not a list! Skipping it")
    }

    val options = project.objects.listProperty(String::class.java).apply {
        set(listOf())
    }
}