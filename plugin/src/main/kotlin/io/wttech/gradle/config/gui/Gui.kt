package io.wttech.gradle.config.gui

import com.formdev.flatlaf.FlatLightLaf
import com.jgoodies.binding.adapter.Bindings
import com.jgoodies.binding.list.SelectionInList
import com.jgoodies.binding.value.AbstractValueModel
import io.wttech.gradle.config.*
import io.wttech.gradle.config.prop.ListProp
import io.wttech.gradle.config.prop.MapProp
import io.wttech.gradle.config.prop.StringProp
import net.miginfocom.layout.CC
import net.miginfocom.layout.LC
import net.miginfocom.swing.MigLayout
import java.awt.*
import java.awt.event.FocusEvent
import java.awt.event.FocusListener
import java.awt.event.WindowAdapter
import java.awt.event.WindowEvent
import javax.swing.*
import javax.swing.text.JTextComponent

@Suppress("UnusedPrivateMember", "MagicNumber")
class Gui(val definition: Definition) {

    private val logger = definition.project.logger

    private var cancelled = false

    private fun lc() = LC().apply {
        if (definition.debug.get()) this.debug(100)
    }

    private val dialog = JDialog().apply {
        title = definition.label.get()
        layout = MigLayout(lc().fill().width("640").height("480"))
        isAlwaysOnTop = true
        isModal = true
        isResizable = true

        addWindowListener(object : WindowAdapter() {
            override fun windowClosing(e: WindowEvent) {
                cancelled = true
                SwingUtilities.invokeLater { // without it sometimes window needed to be double-closed
                    e.window.dispose()
                }
            }
        })
    }

    private var textComponentFocused: JTextComponent? = null

    private abstract inner class PropValueModel : AbstractValueModel() {

        override fun setValue(v: Any?) {
            updateValue(v)
            render()
        }
        open fun updateValue(v: Any?) {}
    }

    class PropPanel(val data: Prop, val container: JPanel, val field: JComponent, val validation: JLabel)
    private val propPanels = mutableListOf<PropPanel>()

    @Suppress("NestedBlockDepth", "ComplexMethod")
    private fun propField(prop: Prop): JComponent = when (prop) {
        is StringProp -> {
            if (prop.options.get().isEmpty()) {
                when (prop.valueType.get()) {
                    StringProp.ValueType.PASSWORD -> JPasswordField()
                    else -> JTextField()
                }.apply {
                    Bindings.bind(this, object : PropValueModel() {
                        override fun getValue() = prop.stringValue
                        override fun updateValue(v: Any?) { prop.valueSet(v?.toString()) }
                    })
                }
            } else {
                if (prop.optionsStyle.get() == StringProp.OptionsStyle.SELECT) {
                    JComboBox<String>().apply {
                        val valueModel = object : PropValueModel() {
                            override fun getValue() = prop.stringValue
                            override fun updateValue(v: Any?) { prop.valueSet(v?.toString()) }
                        }
                        val optionsModel = object : PropValueModel() {
                            override fun getValue() = prop.options.orNull ?: listOf()
                        }
                        Bindings.bind<String>(this, SelectionInList(optionsModel, valueModel))
                    }
                } else if (prop.optionsStyle.get() == StringProp.OptionsStyle.CHECKBOX) {
                    JCheckBox().apply {
                        val valueModel = object : PropValueModel() {
                            override fun getValue() = prop.stringValue?.toBoolean()
                            override fun updateValue(v: Any?) { prop.valueSet(v?.toString()?.toBoolean()) }
                        }
                        Bindings.bind(this, valueModel)
                    }
                } else {
                    throw ConfigException("Config prop '${prop.name}' is using unsupported options style!")
                }
            }
        }
        is ListProp -> {
            if (prop.options.get().isEmpty()) {
                JTextArea().apply {
                    Bindings.bind(this, object : PropValueModel() {
                        override fun getValue() = prop.listValue?.joinToString("\n")
                        override fun updateValue(v: Any?) { prop.valueSet(v?.toString()?.split("\n")) }
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
                        prop.valueSet(v?.toString()?.split("\n")?.associate {
                            it.substringBefore("=") to it.substringAfter("=")
                        })
                    }
                }
                Bindings.bind(this, valueModel)
            }
        }
        else -> throw ConfigException("Config property '${prop.name}' has invalid type!")
    }

    class GroupTab(val group: Group, val panel: JComponent)
    private val groupTabs = mutableListOf<GroupTab>()

    private val tabPane = JTabbedPane().also { tabs ->
        tabs.tabLayoutPolicy = JTabbedPane.SCROLL_TAB_LAYOUT

        dialog.add(tabs, CC().grow().span().wrap())

        definition.groups.get().forEach { group ->
            val panel = JPanel(MigLayout(lc().fillX().insets("10"))).also { tab ->
                if (!group.description.orNull.isNullOrBlank()) {
                    tab.add(JPanel(MigLayout(lc().fill().insets("1"))).also { groupPanel ->
                        groupPanel.add(JLabel().apply {
                            textFormatted(group.description.get())
                            font = scaleFont()
                        }, CC().wrap())
                    }, CC().growX().wrap().alignY("top"))
                }

                group.props.get().filter { it.captured }.forEach { prop: Prop ->
                    tab.add(JPanel(MigLayout(lc().fill().insets("1"))).also { propPanel ->
                        propPanel.add(JLabel(prop.label.get()), CC().wrap())
                        if (!prop.description.orNull.isNullOrBlank()) {
                            propPanel.add(JLabel().apply {
                                textFormatted(prop.description.get())
                                font = scaleFont()
                            }, CC().wrap())
                        }
                        val propField = propField(prop).apply {
                            addFocusListener(object : FocusListener {
                                override fun focusGained(e: FocusEvent) {
                                    when (this@apply) {
                                        is JTextComponent -> textComponentFocused = this@apply
                                        else -> textComponentFocused = null
                                    }
                                    render()
                                }
                                override fun focusLost(e: FocusEvent) { render() }
                            })
                        }
                        when (propField) {
                            is JTextArea -> propPanel.add(propField, CC().minWidth("300").minHeight("60").growX().wrap())
                            else -> propPanel.add(propField, CC().minWidth("300").growX().wrap())
                        }
                        val validationLabel = JLabel().apply {
                            font = scaleFont()
                            foreground = Color(221, 76, 85) // #DD4C55
                        }
                        propPanel.add(validationLabel, CC().wrap())
                        propPanels.add(PropPanel(prop, propPanel, propField, validationLabel))
                    }, CC().growX().wrap().hideMode(1).alignY("top"))
                }
            }
            val scrollPane = JScrollPane(panel).apply {
                horizontalScrollBarPolicy = JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED
                verticalScrollBarPolicy = JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED
            }
            groupTabs.add(GroupTab(group, scrollPane))
        }
    }

    private val pathChooser by lazy {
        JFileChooser().apply {
            currentDirectory = definition.project.rootProject.layout.projectDirectory.asFile
            fileSelectionMode = JFileChooser.FILES_AND_DIRECTORIES
            isFileHidingEnabled = false
        }
    }

    @Suppress("TooGenericExceptionCaught")
    private val pathButton = JButton(ImageIcon(javaClass.getResource("/file-search.png")).scaleSize(16, 16)).apply {
        addActionListener {
            try {
                if (pathChooser.showOpenDialog(this) == JFileChooser.APPROVE_OPTION && textComponentFocused != null) {
                    textComponentFocused!!.document.insertString(
                        textComponentFocused!!.caretPosition,
                        pathChooser.selectedFile.absolutePath,
                        null
                    )
                }
            } catch (e: Exception) {
                logger.debug("Config '${definition.name}' has a problem with opening path chooser!", e)
            }
        }
    }

    private val applyButton = JButton("Apply").apply {
        addActionListener { dialog.dispose() }
    }

    private val actionsPanel = JPanel(MigLayout(lc().fill())).apply {
        add(pathButton, CC().alignX("right"))
        add(applyButton, CC().alignX("left"))
        dialog.add(this, CC().span().growX().wrap().dockSouth())
    }

    private var groupsVisibleOld = -1

    private var tabPaneBgColorDefault: Color? = null

    /**
     * There is no direct way to hide particular panel.
     * As a workaround, only visible tabs are recreated.
     * At the same time, recreation is avoided because field focus is lost.
     */
    fun updateGroupTabs(): Boolean {
        var updated = false

        val groupsVisible = definition.groups.get().filter { it.visible.get() }
        val groupsVisibleNew = groupsVisible.map { Pair(it.name, it.visible.get()) }.hashCode()
        if (groupsVisibleOld != groupsVisibleNew) {
            tabPane.removeAll()
            groupTabs.filter { it.group.visible.get() }.forEach { groupTab ->
                tabPane.addTab(groupTab.group.label.get(), groupTab.panel)
            }
            groupsVisibleOld = groupsVisibleNew
            updated = true
        }

        groupTabs.filter { it.group.visible.get() }.forEachIndexed { index, groupTab ->
            tabPane.setEnabledAt(index, groupTab.group.enabled.get())

            if (tabPaneBgColorDefault == null) {
                tabPaneBgColorDefault = tabPane.getBackgroundAt(index)
            }
            tabPane.setBackgroundAt(index, if (groupTab.group.valid) tabPaneBgColorDefault else Color(255, 200, 200))
        }

        return updated
    }

    fun updatePropPanels() {
        propPanels.forEach { panel ->
            // fix two-way syncing
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
                panel.field is JComboBox<*> && panel.field.selectedItem != normalizedValue -> tryMutate { panel.field.selectedItem = normalizedValue }
            }

            panel.container.isVisible = panel.data.visible.get()
            panel.field.isEnabled = panel.data.enabled.get()

            panel.validation.textFormatted(panel.data.validation)
            panel.validation.isVisible = !panel.data.valid
        }
    }

    @Suppress("TooGenericExceptionCaught")
    private fun tryMutate(action: () -> Unit) {
        try {
            action()
        } catch (e: Exception) {
            if (e.message != "Attempt to mutate in notification") throw e
        }
    }

    fun render(initial: Boolean = false) {
        if (!initial) updateMalformedData()

        val groupTabsUpdated = updateGroupTabs()
        updatePropPanels()
        updateActionPanel()

        if (initial || groupTabsUpdated) dialog.pack()
        if (initial) dialog.centre()

        dialog.isVisible = true
    }

    private fun updateMalformedData() {
        definition.props.forEach { prop ->
            if (prop is StringProp && prop.options.get().isNotEmpty() && prop.value() !in prop.options.get()) {
                prop.valueSet(prop.options.get().first())
            }
        }
    }

    private fun updateActionPanel() {
        pathButton.isEnabled = (textComponentFocused != null) && propPanels.any { it.field is JTextComponent }
        applyButton.isEnabled = definition.valid
    }

    companion object {

        private const val TROUBLESHOOTING = "Please run 'sh gradlew --stop' then try again.\n" +
            "Ultimately run command with '--no-daemon' option."

        @Suppress("TooGenericExceptionCaught")
        fun render(definition: Definition) {
            var cancelled = false
            try {
                FlatLightLaf.setup()
                val gui = Gui(definition)
                gui.render(initial = true)
                if (gui.cancelled) {
                    cancelled = true
                }
            } catch (e: HeadlessException) {
                throw ConfigException("Config '${definition.name}' GUI dialog cannot be opened in headless mode!\n$TROUBLESHOOTING")
            } catch (e: Exception) {
                throw ConfigException("Config '${definition.name}' GUI dialog error!\n$TROUBLESHOOTING", e)
            }
            if (cancelled) {
                throw CancelException("Config '${definition.name}' GUI dialog has been closed!")
            }
        }
    }
}
