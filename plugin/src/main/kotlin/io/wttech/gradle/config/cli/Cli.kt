package io.wttech.gradle.config.cli

import io.wttech.gradle.config.CancelException
import io.wttech.gradle.config.ConfigException
import io.wttech.gradle.config.Definition
import io.wttech.gradle.config.prop.ListProp
import io.wttech.gradle.config.prop.MapProp
import io.wttech.gradle.config.prop.StringProp
import org.gradle.api.internal.tasks.userinput.UserInputHandler
import java.io.PrintWriter
import java.io.StringWriter

class Cli(val definition: Definition) {

    val project = definition.project

    private val userInput by lazy { project.getService<UserInputHandler>() }

    private val commands = mapOf(
        "show-properties" to { showProperties(); false },
        "update-property" to { updateProperty(); false },
        "save" to { true },
        "cancel" to { throw CancelException("Config '${definition.name}' CLI input has been closed!") }
    )

    fun render() {
        while (true) {
            val commandName = userInput.selectOption(composeCommandQuestion(), commands.keys, commands.keys.first())
            if (commands[commandName]!!()) {
                break
            }
        }
    }

    private fun composeCommandQuestion(): String {
        val result = StringWriter()
        PrintWriter(result).apply {
            val invalidProps = definition.props.filter { !it.valid }.map { it.name }
            when {
                invalidProps.isEmpty() -> println("Config is valid. No properties need an update.")
                else -> println("Config is not valid! Properties to be updated: ${invalidProps.joinToString(", ")}")
            }
            print("Select command")
        }
        return result.toString()
    }

    private fun showProperties() {
        val printQuestion = StringWriter()
        PrintWriter(printQuestion).apply {
            println()

            definition.groups.get().filter { it.visible.get() }.forEach { group ->
                val groupHeader = "${group.label.get()} (${group.name})"
                println(groupHeader)

                println()

                group.props.get().filter { it.visible.get() }.forEach { prop ->
                    println("  ${prop.label.get()} (${prop.name})")
                    println("    Value: ${prop.value()?.toString()?.ifBlank { "<empty>" }}")

                    val desc = prop.description.orNull?.trim()
                    if (!desc.isNullOrBlank()) {
                        if (desc.contains("\n")) {
                            println("    Description:")
                            println(prop.description.orNull?.prependIndent("      "))
                        } else {
                            println("    Description: $desc")
                        }
                    }
                }

                println()
            }
        }
        println(printQuestion.toString())
    }

    private fun updateProperty() {
        val propEnabled = definition.props
            .filter { it.group.visible.get() && it.visible.get() && it.enabled.get() }
            .map { it.name }
        val propName = userInput.selectOption("Select property", propEnabled, "none")
        if (propName != "none") {
            when (val prop = definition.getProp(propName)) {
                is StringProp -> {
                    if (prop.options.get().isNotEmpty()) {
                        val propValue = userInput.selectOption(
                            "Select value for property '$propName'",
                            prop.options.get(),
                            prop.value()
                        )
                        prop.valueSet(propValue)
                    } else {
                        val propValue = userInput.askQuestion("Enter value for property '$propName'", prop.value())
                        prop.valueSet(propValue)
                    }
                }
                is ListProp -> {
                    TODO("List prop is not yet supported by CLI mode!")
                }
                is MapProp -> {
                    TODO("Map prop is not yet supported by CLI mode!")
                }
            }
        }
    }

    companion object {

        @Suppress("TooGenericExceptionCaught")
        fun render(definition: Definition) {
            try {
                Cli(definition).render()
            } catch (e: Exception) {
                throw ConfigException("Config '${definition.name}' CLI error!", e)
            }
        }
    }
}
