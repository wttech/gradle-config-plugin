package io.wttech.gradle.config.tpl

import java.io.File

data class TemplateSection(val name: String, val entries: List<String>) {

    fun save(target: File) {
        val text = target.readText()
        val sections = parseAll(text)

        val old = sections.find { it.name == this.name }
        if (old != null) {
            target.writeText(text.replace(old.render(), render()))
        } else {
            target.appendText("${System.lineSeparator()}${render()}")
        }
    }

    fun render() = mutableListOf<String>().apply {
        add(MARKER_START)
        add("$MARKER_NAME=$name")
        addAll(entries.map { it.trim() })
        add(MARKER_END)
    }.joinToString(System.lineSeparator())

    override fun toString() = render()

    companion object {

        const val MARKER_START = "#config-start"

        const val MARKER_END = "#config-end"

        const val MARKER_NAME = "#name"

        @Suppress("NestedBlockDepth")
        fun parseAll(text: String): List<TemplateSection> {
            val sections = mutableListOf<TemplateSection>()

            var section = false
            var sectionName = ""
            val sectionLines = mutableListOf<String>()

            text.lineSequence().forEach { line ->
                when (val l = line.trim()) {
                    MARKER_START -> {
                        section = true
                    }
                    MARKER_END -> {
                        sections.add(TemplateSection(sectionName, sectionLines.toList()))
                        section = false
                        sectionLines.clear()
                    }
                    else -> {
                        if (section) {
                            if (l.startsWith("$MARKER_NAME=")) {
                                sectionName = l.substringAfter("$MARKER_NAME=")
                            } else {
                                sectionLines.add(l)
                            }
                        }
                    }
                }
            }

            return sections
        }
    }
}
