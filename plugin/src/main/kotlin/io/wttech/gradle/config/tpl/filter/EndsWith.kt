package io.wttech.gradle.config.tpl.filter

import com.mitchellbosecke.pebble.extension.Filter
import com.mitchellbosecke.pebble.template.EvaluationContext
import com.mitchellbosecke.pebble.template.PebbleTemplate
import io.wttech.gradle.config.ConfigException

class EndsWith : Filter {

    override fun apply(input: Any, args: MutableMap<String, Any>, self: PebbleTemplate, context: EvaluationContext, lineNumber: Int) = when (input) {
        is String -> {
            val suffix = args["suffix"] ?: throw ConfigException("No 'suffix' argument passed to 'endsWith' filter!")
            when (suffix) {
                is String -> input.startsWith(suffix)
                else -> null
            }
        }
        else -> null
    }

    override fun getArgumentNames() = listOf("suffix")
}
