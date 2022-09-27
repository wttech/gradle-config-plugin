package com.wttech.gradle.config.tpl

import com.mitchellbosecke.pebble.extension.AbstractExtension
import com.wttech.gradle.config.tpl.filter.SubstituteFilter
import org.gradle.api.Project

class TemplateExtension(val project: Project) : AbstractExtension() {

    override fun getFilters() = mutableMapOf(
        "substitute" to SubstituteFilter(),
    )
}
