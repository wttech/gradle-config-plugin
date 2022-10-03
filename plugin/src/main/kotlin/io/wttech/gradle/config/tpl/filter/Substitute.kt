package io.wttech.gradle.config.tpl.filter

import com.mitchellbosecke.pebble.extension.Filter
import com.mitchellbosecke.pebble.template.EvaluationContext
import com.mitchellbosecke.pebble.template.PebbleTemplate
import io.wttech.gradle.config.ConfigException

/**
 * There is 'replace' filter in core but takes a map as argument.
 */
class Substitute : Filter {

    override fun apply(input: Any, args: MutableMap<String, Any>, self: PebbleTemplate, context: EvaluationContext, lineNumber: Int) = when (input) {
        is String -> {
            val search = args["search"] ?: throw ConfigException("No 'search' argument passed to 'substitute' filter!")
            val replace = args["replace"] ?: throw ConfigException("No 'replace' argument passed to 'substitute' filter!")
            when {
                search is String && replace is String -> input.replace(search, replace)
                else -> null
            }
        }
        else -> null
    }

    override fun getArgumentNames() = listOf("search", "replace")
}
