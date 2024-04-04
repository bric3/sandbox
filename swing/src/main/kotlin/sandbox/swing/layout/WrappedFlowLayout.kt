/*
 * Copyright (C) 2022 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package sandbox.swing.layout

import org.intellij.lang.annotations.MagicConstant
import java.awt.Component
import java.awt.Container
import java.awt.Dimension
import java.awt.FlowLayout
import java.util.function.Function
import javax.swing.*
import kotlin.math.max

/**
 * A [FlowLayout] that supports wrapping of its components when contained in a container too small
 * to display all components in a single row.  Although [FlowLayout] supports wrapping components,
 * the default implementation does not return a preferred size reflecting the wrapping behavior. This results
 * in containers typically not growing appropriately when they contain a [FlowLayout] based panel.
 *
 *
 * The code below illustrates the issue: when the frame is resized, the buttons in the panel are wrapped,
 * but the [GridBagLayout] panel does not grow, resulting in buttons disappearing.
 *
 * ```java
 * public class Main {
 *   public static void main(String[] args) {
 *     JPanel panel = new JPanel();
 *     panel.add(new JButton("Button"));
 *     panel.add(new JButton("Button"));
 *     panel.add(new JButton("Button"));
 *     panel.add(new JButton("Button"));
 *     panel.add(new JButton("Button"));
 *     panel.add(new JButton("Button"));
 *     panel.add(new JButton("Button"));
 *
 *     JPanel rootPanel = new JPanel(new GridBagLayout());
 *     rootPanel.add(panel);
 *
 *     JFrame frame = new JFrame("Test");
 *     frame.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
 *     frame.setContentPane(rootPanel);
 *     frame.pack();
 *     frame.setVisible(true);
 *   }
 * }
 * ```
 *
 *
 * Note: This is a modified copy of `com.intellij.vcs.log.ui.frame.WrappedFlowLayout` with
 * the following modification:
 *
 *  * Made constructors to be compatible with [FlowLayout]
 *  * Fixed support of minimum size (instead of always preferred)
 *  * Fixed so that wrapping works when [getAlignOnBaseline] is `true`
 *
 */
class WrappedFlowLayout : FlowLayout {
    @Suppress("unused")
    constructor()

    constructor(
        @MagicConstant(
            intValues = [LEFT.toLong(), CENTER.toLong(), RIGHT.toLong(), LEADING.toLong(), TRAILING.toLong()]
        ) align: Int
    ) : super(align)

    @Suppress("unused")
    constructor(
        @MagicConstant(
            intValues = [LEFT.toLong(), CENTER.toLong(), RIGHT.toLong(), LEADING.toLong(), TRAILING.toLong()]
        ) align: Int, hgap: Int, vgap: Int
    ) : super(align, hgap, vgap)

    override fun preferredLayoutSize(target: Container): Dimension {
        return getWrappedSize(target) { obj: Component -> obj.preferredSize }
    }

    override fun minimumLayoutSize(target: Container): Dimension {
        return getWrappedSize(target) { obj: Component -> obj.minimumSize }
    }

    private fun getWrappedSize(target: Container, sizeGetter: Function<Component, Dimension>): Dimension {
        val maxWidth = getParentMaxWidth(target)
        val insets = target.insets
        var height = insets.top + insets.bottom
        var width = insets.left + insets.right

        var rowHeight = 0
        var rowWidth = insets.left + insets.right

        var isVisible = false
        var start = true

        synchronized(target.treeLock) {
            for (i in 0 until target.componentCount) {
                val component = target.getComponent(i)
                if (component.isVisible) {
                    isVisible = true
                    val size = sizeGetter.apply(component)

                    if (rowWidth + hgap + size.width > maxWidth && !start) {
                        height += vgap + rowHeight
                        width = max(width.toDouble(), rowWidth.toDouble()).toInt()

                        rowWidth = insets.left + insets.right
                        rowHeight = 0
                    }

                    rowWidth += hgap + size.width
                    rowHeight = max(rowHeight.toDouble(), size.height.toDouble()).toInt()

                    start = false
                }
            }
            height += vgap + rowHeight
            width = max(width.toDouble(), rowWidth.toDouble()).toInt()
            return if (!isVisible) {
                super.preferredLayoutSize(target)
            } else {
                Dimension(width, height)
            }
        }
    }

    companion object {
        private fun getParentMaxWidth(target: Container): Int {
            val parent = SwingUtilities.getUnwrappedParent(target) ?: return 0

            return parent.width - (parent.insets.left + parent.insets.right)
        }
    }
}