package com.wttech.gradle.config.tpl.filter

import com.mitchellbosecke.pebble.extension.Filter
import com.mitchellbosecke.pebble.template.EvaluationContext
import com.mitchellbosecke.pebble.template.PebbleTemplate

/**
 * There is 'replace' filter in core but takes a map as argument.
 */
class SubstituteFilter : Filter {

    override fun apply(input: Any, args: MutableMap<String, Any>, self: PebbleTemplate, context: EvaluationContext, lineNumber: Int) = when (input) {
        is String -> {
            val search = args["search"] ?: throw IllegalArgumentException("No search argument passed to substitute filter.")
            val replace = args["replace"] ?: throw IllegalArgumentException("No replace argument passed to substitute filter.")
            when {
                search is String && replace is String -> input.replace(search, replace)
                else -> null
            }
        }
        else -> null
    }

    override fun getArgumentNames() = listOf("search", "replace")
}
