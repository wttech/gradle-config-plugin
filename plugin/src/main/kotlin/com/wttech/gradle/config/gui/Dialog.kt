package com.wttech.gradle.config.gui

import com.formdev.flatlaf.FlatLightLaf
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

    fun layoutConstraints(vararg constraints: String) = constraints.toMutableList().apply {
        if (layoutDebug) add("debug")
    }.joinToString(",")

    private val dialog = JDialog().apply {
        title = "Config"
        layout = MigLayout(layoutConstraints("fill"))
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

    class PropPanel(val data: Prop, val container: JPanel, val field: JComponent)
    private val propPanels = mutableListOf<PropPanel>()

    private fun propField(prop: Prop): JComponent = when (prop) {
        is SingleProp -> {
            if (prop.options.get().isEmpty()) {
                JTextField().apply {
                    Bindings.bind(this, object : PropValueModel() {
                        override fun getValue() = prop.singleValue
                        override fun updateValue(v: Any?) { prop.value(v?.toString()) }
                    })
                }
            } else {
                JComboBox<String>().apply {
                    val valueModel = object : PropValueModel() {
                        override fun getValue() = prop.singleValue
                        override fun updateValue(v: Any?) { prop.value(v?.toString()) }
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
                        override fun getValue() = prop.listValue?.joinToString("\n")
                        override fun updateValue(v: Any?) { prop.value(v?.toString()?.split("\n")) }
                    })
                }
            } else {
                TODO("multiple options selection is not yet implemented")
            }
        }
        is MapProp -> {
            JTextArea().apply {
                val valueModel = object : PropValueModel() {
                    override fun getValue() = prop.mapValue?.map { "${it.key}=${it.value}" }?.joinToString("\n")
                    override fun updateValue(v: Any?) {
                        prop.value(v?.toString()?.split("\n")?.associate {
                            it.substringBefore("=") to it.substringAfter("=")
                        })
                    }
                }
                Bindings.bind(this, valueModel)
            }
        }
        else -> throw ConfigException("Config property '${prop.name}' has invalid type!")
    }

    class GroupTab(val group: Group, val panel: JPanel)
    private val groupTabs = mutableListOf<GroupTab>()

    private val tabPane = JTabbedPane().also { tabs ->
        dialog.add(tabs, "grow, span, wrap")

        config.groups.get().forEach { group ->
            val panel = JPanel(MigLayout(layoutConstraints("fillx", "insets 5"))).also { tab ->
                group.props.get().forEach { prop: Prop ->
                    tab.add(JPanel(MigLayout(layoutConstraints("fill", "insets 5"))).also { propPanel ->
                        propPanel.add(JLabel(prop.label.get()), "wrap")
                        val propField = propField(prop)
                        when (propField) {
                            is JTextArea -> propPanel.add(propField, "w 300::, h 60::, growx, wrap")
                            else -> propPanel.add(propField, "w 300::, growx, wrap")
                        }
                        propPanels.add(PropPanel(prop, propPanel, propField))
                    }, "growx, wrap, top")
                }
            }
            groupTabs.add(GroupTab(group, panel))
        }
    }

    private val applyButton = JButton("Apply").apply {
        addActionListener { dialog.dispose() }
        dialog.add(this, "span, wrap")
    }

    private val actionsPanel = JPanel(MigLayout(layoutConstraints("fill"))).apply {
        add(applyButton, "align center")
        dialog.add(this, "span, growx, wrap, south")
    }

    private fun JDialog.centre() {
        val dimension = Toolkit.getDefaultToolkit().screenSize
        val x = ((dimension.getWidth() - width) / 2).toInt()
        val y = ((dimension.getHeight() - height) / 2).toInt()

        setLocation(x, y)
    }

    private var groupsVisibleOld = -1

    /**
     * There is no direct way to hide particular panel.
     * As a workaround, only visible tabs are recreated.
     * At the same time, recreation is avoided because field focus is lost.
     */
    fun updateGroupTabs() {
        val groupsVisible = config.groups.get().filter { it.visible.get() }
        val groupsVisibleNew = groupsVisible.map { Pair(it.name, it.visible.get()) }.hashCode()
        if (groupsVisibleOld != groupsVisibleNew) {
            tabPane.removeAll()
            groupTabs.filter { it.group.visible.get() }.forEach { groupTab ->
                tabPane.addTab(groupTab.group.label.get(), groupTab.panel)
            }
            groupsVisibleOld = groupsVisibleNew
        }

        groupTabs.filter { it.group.visible.get() }.forEachIndexed { index, groupTab ->
            tabPane.setEnabledAt(index, groupTab.group.enabled.get())
        }
    }

    fun updatePropPanels() {
        propPanels.forEach { panel ->
            panel.container.isVisible = panel.data.visible.get()
            panel.field.isEnabled = panel.data.enabled.get()

            // fix two-way syncing for text field and area (combo works fine)
            val normalizedValue by lazy {
                when (panel.data) {
                    is ListProp -> panel.data.value()?.joinToString("\n")
                    is MapProp -> panel.data.value()?.map { "${it.key}=${it.value}" }?.joinToString("\n")
                    else -> panel.data.value()?.toString()
                }
            }
            when {
                panel.field is JTextField && panel.field.text != normalizedValue -> tryMutate { panel.field.text = normalizedValue }
                panel.field is JTextArea && panel.field.text != normalizedValue -> tryMutate { panel.field.text = normalizedValue }
            }
        }
    }

    private fun tryMutate(action: () -> Unit) {
        try {
            action()
        } catch (e: Exception) {
            if (e.message != "Attempt to mutate in notification") throw e
        }
    }

    fun render(initial: Boolean = false) {
        updateGroupTabs()
        updatePropPanels()

        dialog.pack()
        if (initial) dialog.centre()
        dialog.isVisible = true
    }

    companion object {

        private const val TROUBLESHOOTING = "Please run 'sh gradlew --stop' then try again.\n" +
                "Ultimately run command with '--no-daemon' option."

        @Suppress("TooGenericExceptionCaught")
        fun render(config: Config) = try {
            FlatLightLaf.setup()
            val dialog = Dialog(config)
            dialog.render(true)
        } catch (e: HeadlessException) {
            throw ConfigException("Config GUI dialog cannot be opened in headless mode!\n$TROUBLESHOOTING")
        } catch (e: Exception) {
            throw ConfigException("Config GUI dialog cannot be opened!\n$TROUBLESHOOTING", e)
        }
    }
}