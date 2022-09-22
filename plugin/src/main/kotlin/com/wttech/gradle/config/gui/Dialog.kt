package com.wttech.gradle.config.gui

import com.jgoodies.binding.adapter.Bindings
import com.jgoodies.binding.list.SelectionInList
import com.jgoodies.binding.value.AbstractValueModel
import com.wttech.gradle.config.*
import net.miginfocom.swing.MigLayout
import java.awt.HeadlessException
import java.awt.Toolkit
import java.awt.event.WindowAdapter
import java.awt.event.WindowEvent
import javax.swing.*

class Dialog(val config: Config) {

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

    private abstract inner class PropValueModel : AbstractValueModel() {
        override fun setValue(v: Any?) {
            updateValue(v)
            render()
        }
        open fun updateValue(v: Any?) {}
    }

    class PropPanel(val data: Prop<*>, val container: JPanel, val field: JComponent)
    private val propPanels = mutableListOf<PropPanel>()

    private fun propField(prop: Prop<out Any>): JComponent = when (prop) {
        is SingleProp -> {
            if (prop.options.get().isEmpty()) {
                JTextField().apply {
                    Bindings.bind(this, object : PropValueModel() {
                        override fun getValue() = prop.value.orNull
                        override fun updateValue(v: Any?) { prop.value.set(v?.toString()) }
                    })
                }
            } else {
                JComboBox<String>().apply {
                    val valueModel = object : PropValueModel() {
                        override fun getValue() = prop.value.orNull
                        override fun updateValue(v: Any?) { prop.value.set(v?.toString()) }
                    }
                    val optionsModel = object : PropValueModel() {
                        override fun getValue() = prop.options.orNull ?: listOf()
                    }
                    Bindings.bind<String>(this, SelectionInList(optionsModel, valueModel))
                }
            }
        }
        is ListProp -> {
            if (prop.options.get().isEmpty()) {
                JTextArea().apply {
                    Bindings.bind(this, object : PropValueModel() {
                        override fun getValue() = prop.value.orNull?.joinToString("\n")
                        override fun setValue(v: Any?) { prop.value.set(v?.toString()?.split("\n")) }
                    })
                }
            } else {
                TODO("multiple options selection is not yet implemented")
            }
        }
        is MapProp -> {
            JTextArea().apply {
                val valueModel = object : PropValueModel() {
                    override fun getValue() = prop.value.orNull?.map { "${it.key}=${it.value}" }?.joinToString("\n")
                    override fun updateValue(v: Any?) {
                        prop.value.set(v?.toString()?.split("\n")?.associate {
                            it.substringBefore("=") to it.substringAfter("=")
                        })
                    }
                }
                Bindings.bind(this, valueModel)
                Bindings.addComponentPropertyHandler(this, valueModel)
            }
        }
        else -> throw ConfigException("Config property '${prop.name}' has invalid type!")
    }

    private val tabPane = JTabbedPane().also { tabs ->
        dialog.add(tabs, "grow, span, wrap")

        config.groups.get().forEach { group ->
            tabs.addTab(group.label.get(), JPanel(MigLayout("$layoutConstraints, insets 0")).also { tab ->
                group.props.get().forEach { prop ->
                    tab.add(JPanel(MigLayout("$layoutConstraints, insets 5")).also { propPanel ->
                        propPanel.add(JLabel(prop.label.get()), "wrap")
                        val propField = propField(prop)
                        when (propField) {
                            is JTextArea -> propPanel.add(propField, "w 300::, h 60::, growx, wrap")
                            else -> propPanel.add(propField, "w 300::, growx, wrap")
                        }
                        propPanels.add(PropPanel(prop, propPanel, propField))
                    }, "growx, wrap")
                }
            })
        }
    }

    private val applyButton = JButton("Apply").apply {
        addActionListener { dialog.dispose() }
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

    fun updateGroupTabs() {
        // TODO ...
    }

    fun updatePropPanels() {
        propPanels.forEach { panel ->
            panel.container.isVisible = panel.data.visible.get()
            panel.field.isEnabled = panel.data.enabled.get()
        }
    }

    fun render() {
        updateGroupTabs()
        updatePropPanels()

        dialog.pack()
        dialog.centre()
        dialog.isVisible = true
    }

    companion object {

        private const val TROUBLESHOOTING = "Please run 'sh gradlew --stop' then try again.\n" +
                "Ultimately run command with '--no-daemon' option."

        @Suppress("TooGenericExceptionCaught")
        fun render(config: Config) = try {
            val laf = UIManager.getLookAndFeel()
            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName())
            val dialog = Dialog(config)
            UIManager.setLookAndFeel(laf)
            dialog.render()
        } catch (e: HeadlessException) {
            throw ConfigException("Config GUI dialog cannot be opened in headless mode!\n$TROUBLESHOOTING")
        } catch (e: Exception) {
            throw ConfigException("Config GUI dialog cannot be opened!\n$TROUBLESHOOTING", e)
        }
    }
}
