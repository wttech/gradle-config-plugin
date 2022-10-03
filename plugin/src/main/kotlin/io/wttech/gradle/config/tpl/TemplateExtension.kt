package io.wttech.gradle.config.tpl

import com.mitchellbosecke.pebble.extension.AbstractExtension
import io.wttech.gradle.config.tpl.filter.EndsWith
import io.wttech.gradle.config.tpl.filter.StartsWith
import io.wttech.gradle.config.tpl.filter.Substitute
import io.wttech.gradle.config.tpl.filter.Wildcard
import org.gradle.api.Project

class TemplateExtension(val project: Project) : AbstractExtension() {

    override fun getFilters() = mutableMapOf(
        "wildcard" to Wildcard(),
        "substitute" to Substitute(),
        "startsWith" to StartsWith(),
        "endsWith" to EndsWith(),
    )
}
