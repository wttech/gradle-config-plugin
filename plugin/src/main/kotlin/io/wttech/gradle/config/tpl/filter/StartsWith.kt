package io.wttech.gradle.config.tpl.filter

import com.mitchellbosecke.pebble.extension.Filter
import com.mitchellbosecke.pebble.template.EvaluationContext
import com.mitchellbosecke.pebble.template.PebbleTemplate
import io.wttech.gradle.config.ConfigException

class StartsWith : Filter {

    override fun apply(input: Any, args: MutableMap<String, Any>, self: PebbleTemplate, context: EvaluationContext, lineNumber: Int) = when (input) {
        is String -> {
            val prefix = args["prefix"] ?: throw ConfigException("No 'prefix' argument passed to 'startsWith' filter!")
            when (prefix) {
                is String -> input.startsWith(prefix)
                else -> null
            }
        }
        else -> null
    }

    override fun getArgumentNames() = listOf("prefix")
}
