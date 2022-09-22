package com.wttech.gradle.config.gui

import com.jgoodies.binding.adapter.ComboBoxAdapter
import com.jgoodies.binding.adapter.TextComponentConnector
import com.jgoodies.binding.beans.PropertyAdapter
import com.wttech.gradle.config.*
import net.miginfocom.swing.MigLayout
import java.awt.HeadlessException
import java.awt.Toolkit
import java.awt.event.WindowAdapter
import java.awt.event.WindowEvent
import javax.swing.*


class Dialog(val config: Config, val onApply: () -> Unit) {

    private var cancelled = false

    val layoutDebug = config.config.debugMode.get()

    val layoutConstraints = mutableListOf("fill").apply { if (layoutDebug) add("debug") }.joinToString(",")

    private val dialog = JDialog().apply {
        title = "Config"
        layout = MigLayout(layoutConstraints)
        isAlwaysOnTop = true
        isModal = true
        isResizable = true

        addWindowListener(object : WindowAdapter() {
            override fun windowClosing(e: WindowEvent) {
                e.window.dispose()
                cancelled = true
            }
        })
    }

    private val tabPane = JTabbedPane().also { tabs ->
        dialog.add(tabs, "grow, span, wrap")

        config.groups.get().forEach { group ->
            tabs.addTab(group.label.get(), JPanel(MigLayout("$layoutConstraints, insets 0")).also { tab ->
                group.props.get().forEach { prop ->
                    tab.add(JPanel(MigLayout("$layoutConstraints, insets 5")).also { propPanel ->
                        propPanel.add(JLabel(prop.label.get()), "wrap")
                        when (val propField = propField(prop)) {
                            is JTextArea -> propPanel.add(propField, "w 300::, h 60::, growx, wrap")
                            else -> propPanel.add(propField, "w 300::, growx, wrap")
                        }
                    }, "growx, wrap")
                }
            })
        }
    }

    private fun propField(prop: Prop<out Any>): JComponent = when (prop) {
        is SingleProp -> {
            if (prop.options.get().isEmpty()) {
                JTextField().also { field ->
                    TextComponentConnector(prop.toAdapter(), field).updateTextComponent();
                }
            } else {
                val model = ComboBoxAdapter(prop.options.get(), prop.toAdapter())
                JComboBox(model)
            }
        }
        is ListProp -> {
            if (prop.options.get().isEmpty()) {
                JTextArea().also { field ->
                    TextComponentConnector(prop.toAdapter("\n"), field).updateTextComponent();
                }
            } else {
                TODO("multiple options selection is not yet implemented")
            }
        }
        is MapProp -> {
            TODO("map value is not yet supported")
        }
        else -> throw ConfigException("Config property '${prop.name}' has invalid type!")
    }.apply {
        isVisible = prop.visible.get()
    }

    private val applyButton = JButton("Apply").apply {
        addActionListener {
            onApply()
            dialog.dispose()
        }
        dialog.add(this, "span, wrap")
    }

    private val actionsPanel = JPanel(MigLayout(layoutConstraints)).apply {
        add(applyButton, "align center")
        dialog.add(this, "span, growx, wrap")
    }

    private fun JDialog.centre() {
        val dimension = Toolkit.getDefaultToolkit().screenSize
        val x = ((dimension.getWidth() - width) / 2).toInt()
        val y = ((dimension.getHeight() - height) / 2).toInt()

        setLocation(x, y)
    }

    fun render() {
        dialog.pack()
        dialog.centre()
        dialog.isVisible = true
    }

    companion object {

        private const val TROUBLESHOOTING = "Please run 'sh gradlew --stop' then try again.\n" +
                "Ultimately run command with '--no-daemon' option."

        @Suppress("TooGenericExceptionCaught")
        fun render(config: Config, onApply: () -> Unit) = try {
            val laf = UIManager.getLookAndFeel()
            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName())
            val dialog = Dialog(config, onApply)
            UIManager.setLookAndFeel(laf)
            dialog.render()
        } catch (e: HeadlessException) {
            throw ConfigException("Config GUI dialog cannot be opened in headless mode!\n$TROUBLESHOOTING")
        } catch (e: Exception) {
            throw ConfigException("Config GUI dialog cannot be opened!\n$TROUBLESHOOTING", e)
        }
    }
}

fun MapProp.toAdapter(): PropertyAdapter<Any> {
    return PropertyAdapter(object {
        var p: Map<String, Any?>?
            get() = value.orNull
            set(v) { value.set(v) }
    }, "p")
}

fun SingleProp.toAdapter(): PropertyAdapter<Any> {
    return PropertyAdapter(object {
        var p: String?
            get() = value.orNull
            set(v) { value.set(v) }
    }, "p")
}

fun ListProp.toAdapter(separator: String): PropertyAdapter<Any> {
    return PropertyAdapter(object {
        var p: String?
            get() = value.orNull?.joinToString(separator)
            set(v) { value.set(v?.split(separator)) }
    }, "p")
}


