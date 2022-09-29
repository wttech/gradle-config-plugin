package com.wttech.gradle.config.gui

import java.awt.Font
import java.awt.Image
import java.awt.Toolkit
import javax.swing.ImageIcon
import javax.swing.JDialog
import javax.swing.JLabel

fun JDialog.centre() {
    val dimension = Toolkit.getDefaultToolkit().screenSize
    val x = ((dimension.getWidth() - width) / 2).toInt()
    val y = ((dimension.getHeight() - height) / 2).toInt()

    setLocation(x, y)
}

fun JLabel.textFormatted(text: String?) {
    this.text = text?.let { "<html>${text.replace("\n", "<br/>")}</html>" }
}

fun ImageIcon.scaleSize(width: Int, height: Int) = ImageIcon(this.image.getScaledInstance(width, height, Image.SCALE_SMOOTH))

fun JLabel.scaleFont(scale: Double = 0.75) = Font(font.name, Font.PLAIN, (font.size.toDouble() * scale).toInt())
