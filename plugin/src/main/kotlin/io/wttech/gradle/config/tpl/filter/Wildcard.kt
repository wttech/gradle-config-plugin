package io.wttech.gradle.config.tpl.filter

import com.mitchellbosecke.pebble.extension.Filter
import com.mitchellbosecke.pebble.template.EvaluationContext
import com.mitchellbosecke.pebble.template.PebbleTemplate
import io.wttech.gradle.config.ConfigException
import org.apache.commons.io.FilenameUtils

class Wildcard : Filter {

    override fun apply(input: Any, args: MutableMap<String, Any>, self: PebbleTemplate, context: EvaluationContext, lineNumber: Int) = when (input) {
        is String -> {
            val pattern = args["pattern"] ?: throw ConfigException("No 'pattern' argument passed to 'wildcard' filter!")
            when (pattern) {
                is String -> FilenameUtils.wildcardMatch(input, pattern)
                else -> null
            }
        }
        else -> null
    }

    override fun getArgumentNames() = listOf("pattern")
}
