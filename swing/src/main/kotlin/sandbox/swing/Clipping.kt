package sandbox.swing

import org.intellij.lang.annotations.MagicConstant
import sandbox.swing.BackgroundClippingPanel.Companion.clipBottom
import sandbox.swing.BackgroundClippingPanel.Companion.clipTop
import java.awt.BorderLayout
import java.awt.Color
import java.awt.Graphics
import java.awt.Graphics2D
import java.awt.RenderingHints
import java.awt.geom.Path2D
import javax.swing.*

///////////////////////////////////////////////////////////////
//// DISCLAIMER: Clipping code does not yield good results ////
///////////////////////////////////////////////////////////////


open class Wrapper(content: JComponent) : JPanel(BorderLayout()) {
    init {
        add(content, BorderLayout.CENTER)
    }
}

/**
 * A panel that clips its background (capture from its content component) to the top or bottom.
 *
 * In order to properly show clipped background, it disables the opacity
 * on the `content` component and captures its background color.
 */
private class BackgroundClippingPanel(
    @MagicConstant(intValues = [CLIP_TOP.toLong(), CLIP_BOTTOM.toLong()]) private val cornerClipping: Int,
    content: JComponent
) : Wrapper(content) {
    companion object {
        const val CLIP_TOP = 0b01
        const val CLIP_BOTTOM = 0b10

        fun <T : JComponent> T.clipTop() = BackgroundClippingPanel(
            CLIP_TOP,
            this
        )

        fun <T : JComponent> T.clipBottom() = BackgroundClippingPanel(
            CLIP_BOTTOM,
            this
        )
    }

    private val arc = 20f

    init {
        content.isOpaque = false
        background = content.background
        size = content.size
        preferredSize = content.preferredSize
        minimumSize = content.minimumSize
        maximumSize = content.maximumSize
        isOpaque = true
    }

    override fun paintComponent(g: Graphics?) {
        val g2 = g as Graphics2D
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON)
        g2.setRenderingHint(RenderingHints.KEY_STROKE_CONTROL, RenderingHints.VALUE_STROKE_NORMALIZE)

        val clipRegion = Path2D.Float().apply {
            val adj = .5f
            val w = width.toFloat() - adj
            val h = height.toFloat() - adj

            if (cornerClipping and CLIP_TOP == CLIP_TOP) {
                moveTo(0f, arc)
                // top left corner
                curveTo(0f, 0f, arc, 0f)
                lineTo(w - arc, 0f)
                // top right corner
                curveTo(w, 0f, w, arc)
            } else {
                moveTo(0f, 0f)
                lineTo(w, 0f)
            }

            if (cornerClipping and CLIP_BOTTOM == CLIP_BOTTOM) {
                lineTo(w, h - arc)
                // bottom right corner
                curveTo(w, h, w - arc, h)
                lineTo(arc, h)
                // bottom left corner
                curveTo(0f, h, 0f, h - arc)
            } else {
                lineTo(w, h)
                lineTo(0f, h)
            }
            closePath()
        }
        val oldClip = g2.clip
        g2.clip = clipRegion
        super.paintComponent(g)
        g2.clip = oldClip
    }

    private fun Path2D.Float.curveTo(
        controlPointX: Float,
        controlPointY: Float,
        endPointX: Float,
        endPointY: Float
    ) {
        curveTo(controlPointX, controlPointY, controlPointX, controlPointY, endPointX, endPointY)
    }
}

fun main() {
    SwingUtilities.invokeLater {
        val contentThatNeedToBeClipped = JPanel(BorderLayout()).apply {
            add(
                JLabel("Should be clipped on top").apply {
                    background = Color(0xD4D4D4)
                    isOpaque = true
                    border = BorderFactory.createEmptyBorder(2, 5, 2, 5)
                }.clipTop(),
                BorderLayout.NORTH
            )
            add(
                JLabel("Main content").apply {
                    background = Color(0xF1F1F1)
                    isOpaque = true // Somewhat replaces the container background
                    border = BorderFactory.createEmptyBorder(2, 5, 2, 5)
                },
                BorderLayout.CENTER
            )
            add(
                JLabel("Should be clipped on bottom").apply {
                    background = Color(0xD4D4D4)
                    isOpaque = true
                    border = BorderFactory.createEmptyBorder(2, 5, 2, 5)
                }.clipBottom(),
                BorderLayout.SOUTH
            )
        }.apply {
            isOpaque = false // otherwise the background color will be painted on below the clipped region
        }

        val frameContent = JPanel(BorderLayout()).apply {
            background = Color(0x333333)
            border = BorderFactory.createEmptyBorder(30, 30, 30, 30)
            add(contentThatNeedToBeClipped)
        }

        JFrame("Clipping").run {
            defaultCloseOperation = JFrame.EXIT_ON_CLOSE
            setSize(400, 400)
            contentPane = frameContent
            isVisible = true
        }
    }
}
