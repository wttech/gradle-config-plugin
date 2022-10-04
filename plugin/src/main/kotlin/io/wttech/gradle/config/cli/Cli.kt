package io.wttech.gradle.config.cli

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

    val userInput by lazy { project.getService<UserInputHandler>() }

    fun render() {
        while (true) {
            when (userInput.selectOption(composeCommandQuestion(), listOf("show-properties", "update-property", "quit"), "show-properties")) {
                "show-properties" -> showProperties()
                "update-property" -> updateProperty()
                "quit" -> break
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

            // TODO print with property name in the beginning; all else wrapped by 2 spaces including: value, label, description etc
            definition.groups.get().filter { it.visible.get() }.forEach { group ->
                val groupHeader = "${group.label.get()} (${group.name})"
                println(groupHeader)
                println("=".repeat(groupHeader.length))

                group.props.get().filter { it.visible.get() }.forEach { prop ->
                    println("${prop.label.get()} (${prop.name}) = ${prop.value()}")
                }

                println()
            }
        }
        println(printQuestion.toString())
    }

    // TODO print current value in option
    private fun updateProperty() {
        val propName = userInput.selectOption("Select property", definition.props.map { it.name }, "none")
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
