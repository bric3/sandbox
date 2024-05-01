/*
 * sandbox
 *
 * Copyright (c) 2021,today - Brice Dutheil <brice.dutheil@gmail.com>
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */
package sandbox.swing

import java.awt.AlphaComposite
import java.awt.BorderLayout
import java.awt.Color
import java.awt.GradientPaint
import java.awt.Graphics
import java.awt.Graphics2D
import java.awt.GridBagLayout
import java.awt.RenderingHints
import java.awt.Transparency
import java.awt.event.ActionEvent
import java.awt.image.BufferedImage
import javax.swing.*
import kotlin.system.exitProcess

class SoftClippedWindow : JPanel() {
  init {
    layout = GridBagLayout()
    add(JButton(object : AbstractAction("Close") {
      override fun actionPerformed(e: ActionEvent) {
        exitProcess(0)
      }
    }).apply { isOpaque = false })
  }

  public override fun paintComponent(g: Graphics) {
    val g2d = g.create() as Graphics2D

    val width = width
    val height = height

    // Create a soft clipped image for the background
    val img = java_2d_tricker(g2d, width, height)
    // Copy our intermediate image to the screen
    g2d.drawImage(img, 0, 0, null)

    g2d.dispose()
  }

  /**
   * Trick to perform soft clipping
   *
   * This code is taken from
   * https://web.archive.org/web/20120603053853/http://weblogs.java.net/blog/campbell/archive/2006/07/java_2d_tricker.html
   */
  private fun java_2d_tricker(g2d: Graphics2D, width: Int, height: Int): BufferedImage {
    // Create a translucent intermediate image in which we can perform
    // the soft clipping
    val gc = g2d.deviceConfiguration
    val img = gc.createCompatibleImage(width, height, Transparency.TRANSLUCENT)
    val g2 = img.createGraphics()
    // Clear the image so all pixels have zero alpha
    g2.composite = AlphaComposite.Clear
    g2.fillRect(0, 0, width, height)

    // Render our clip shape into the image.  Note that we enable
    // antialiasing to achieve the soft clipping effect.  Try
    // commenting out the line that enables antialiasing, and
    // you will see that you end up with the usual hard clipping.
    g2.composite = AlphaComposite.Src
    g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON)
    g2.color = Color.WHITE
    g2.fillOval(width / 4, height / 4, width / 2, height / 2)

    // Here's the trick... We use SrcAtop, which effectively uses the
    // alpha value as a coverage value for each pixel stored in the
    // destination.  For the areas outside our clip shape, the destination
    // alpha will be zero, so nothing is rendered in those areas.  For
    // the areas inside our clip shape, the destination alpha will be fully
    // opaque, so the full color is rendered.  At the edges, the original
    // antialiasing is carried over to give us the desired soft clipping
    // effect.
    g2.composite = AlphaComposite.SrcAtop
    g2.paint = GradientPaint(0f, 0f, Color.RED, 0f, height.toFloat(), Color.YELLOW)
    g2.fillRect(0, 0, width, height)
    g2.dispose()
    return img
  }
}

fun main() {
  SwingUtilities.invokeLater {
    JWindow().run {
      contentPane.apply {
        layout = BorderLayout()
        add(SoftClippedWindow())
      }
      isAlwaysOnTop = true
      // Keep window opaque but transparent
      background = Color(0, true)
      setSize(200, 200)
      isVisible = true
    }
  }
}
