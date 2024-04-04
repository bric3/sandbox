/*
 * Copyright (c) 2011, Oracle and/or its affiliates. All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions
 * are met:
 *
 *   - Redistributions of source code must retain the above copyright
 *     notice, this list of conditions and the following disclaimer.
 *
 *   - Redistributions in binary form must reproduce the above copyright
 *     notice, this list of conditions and the following disclaimer in the
 *     documentation and/or other materials provided with the distribution.
 *
 *   - Neither the name of Oracle or the names of its
 *     contributors may be used to endorse or promote products derived
 *     from this software without specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS
 * IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO,
 * THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR
 * PURPOSE ARE DISCLAIMED.  IN NO EVENT SHALL THE COPYRIGHT OWNER OR
 * CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL,
 * EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO,
 * PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR
 * PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF
 * LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING
 * NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
 * SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */
package sandbox.swing

import java.awt.Graphics
import java.awt.Graphics2D
import java.awt.image.BufferedImage
import java.awt.image.BufferedImageOp
import java.awt.image.ConvolveOp
import java.awt.image.Kernel
import javax.swing.*
import javax.swing.plaf.LayerUI

/**
 * Apply a JLayer on top of a component.
 * Converted to Kotlin from [JLayer Myopia example](https://docs.oracle.com/javase/tutorial/displayCode.html?code=https://docs.oracle.com/javase/tutorial/uiswing/examples/misc/MyopiaProject/src/Myopia.java),
 * hence the license header.
 * Also see [JLayer tutorial](https://docs.oracle.com/javase/tutorial/uiswing/misc/jlayer.html).
 */
fun main() {
  SwingUtilities.invokeLater {
    val jlayer = createPanel().let {
      val layerUI: LayerUI<JComponent> = BlurLayerUI()
      JLayer(it, layerUI)
    }
    with(JFrame("Myopia")) {
      add(jlayer)
      setSize(300, 200)
      defaultCloseOperation = JFrame.EXIT_ON_CLOSE
      setLocationRelativeTo(null)
      isVisible = true
    }
  }
}

private fun createPanel(): JPanel {
  return JPanel().apply {
    val entreeGroup = ButtonGroup()
    JRadioButton("Beef", true).also {
      add(it)
      entreeGroup.add(it)
    }
    JRadioButton("Chicken", true).also {
      add(it)
      entreeGroup.add(it)
    }
    JRadioButton("Vegetable", true).also {
      add(it)
      entreeGroup.add(it)
    }
    add(JCheckBox("Ketchup"))
    add(JCheckBox("Mustard"))
    add(JCheckBox("Pickles"))
    add(JLabel("Special requests:"))
    add(JTextField(20))
    add(JButton("Place Order"))
  }
}

internal class BlurLayerUI : LayerUI<JComponent>() {
  private var mOffscreenImage: BufferedImage? = null
  private val mOperation: BufferedImageOp

  init {
    val ninth = 1.0f / 9.0f
    val blurKernel = floatArrayOf(
      ninth, ninth, ninth,
      ninth, ninth, ninth,
      ninth, ninth, ninth
    )
    mOperation = ConvolveOp(
      Kernel(3, 3, blurKernel),
      ConvolveOp.EDGE_NO_OP, null
    )
  }

  override fun paint(g: Graphics, c: JComponent) {
    val w = c.width
    val h = c.height
    if (w == 0 || h == 0) {
      return
    }

    // Only create the offscreen image if the one we have
    // is the wrong size.
    if (mOffscreenImage == null || mOffscreenImage!!.width != w || mOffscreenImage!!.height != h) {
      mOffscreenImage = BufferedImage(w, h, BufferedImage.TYPE_INT_RGB)
    }
    val ig2 = mOffscreenImage!!.createGraphics()
    ig2.clip = g.clip
    super.paint(ig2, c)
    ig2.dispose()
    val g2 = g as Graphics2D
    g2.drawImage(mOffscreenImage, mOperation, 0, 0)
  }
}
