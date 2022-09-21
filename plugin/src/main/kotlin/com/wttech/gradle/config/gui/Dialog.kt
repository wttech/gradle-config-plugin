package com.wttech.gradle.config.gui

import com.wttech.gradle.config.Config
import com.wttech.gradle.config.ConfigException
import net.miginfocom.swing.MigLayout
import java.awt.HeadlessException
import java.awt.Toolkit
import java.awt.event.WindowAdapter
import java.awt.event.WindowEvent
import javax.swing.JComponent
import javax.swing.JDialog
import javax.swing.JLabel
import javax.swing.JPanel
import javax.swing.JTabbedPane
import javax.swing.JTextField
import javax.swing.UIManager

class Dialog(val config: Config) {

    private var cancelled = false

    private val migLayout = MigLayout(
        "insets 10 10 10 10",
        "[fill,grow][fill,grow]",
        "[fill,grow]"
    )

    private val dialog by lazy {
        JDialog().apply {
            title = "Config"
            layout = migLayout
            isAlwaysOnTop = true
            isModal = true
            isResizable = false

            addWindowListener(object : WindowAdapter() {
                override fun windowClosing(e: WindowEvent) {
                    e.window.dispose()
                    cancelled = true
                }
            })
        }
    }

    private val tabPane by lazy {
        JTabbedPane().also { tabs ->
            dialog.add(tabs, "span, wrap")

            config.groups.get().forEach { group ->
                tabs.add(group.name, JPanel().also { tab ->
                    tab.layout = migLayout

                    group.props.get().forEach { prop ->
                        JPanel().also {panel ->
                            panel.layout = migLayout
                            panel.add(JLabel(prop.name))
                            panel.add(JTextField(prop.value.get()))
                        }
                    }
                })
            }
        }
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