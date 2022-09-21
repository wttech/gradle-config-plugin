package com.wttech.gradle.config.gui

import com.wttech.gradle.config.Config
import com.wttech.gradle.config.ConfigException
import net.miginfocom.swing.MigLayout
import java.awt.HeadlessException
import java.awt.Toolkit
import java.awt.event.WindowAdapter
import java.awt.event.WindowEvent
import javax.swing.*

class Dialog(val config: Config) {

    private var cancelled = false

    val layoutDebug = false

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
            tabs.addTab(group.name, JPanel(MigLayout("$layoutConstraints, insets 0")).also { tab ->
                group.props.get().forEach { prop ->
                    tab.add(JPanel(MigLayout("$layoutConstraints, insets 5")).also { propPanel ->
                        propPanel.add(JLabel(prop.name), "wrap")
                        propPanel.add(JTextField(prop.value.orNull), " w 300::, growx, wrap")
                    }, "growx, wrap")
                }

            })
        }
    }

    private val closeButton = JButton("Apply").apply {
        addActionListener { dialog.dispose() }
        dialog.add(this, "span, wrap")
    }

    private val actionsPanel = JPanel(MigLayout(layoutConstraints)).apply {
        add(closeButton, "align center")
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
        fun make(config: Config) = try {
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