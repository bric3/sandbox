package sandbox.swing

import java.awt.BorderLayout
import java.awt.Color
import java.awt.Dimension
import java.awt.Point
import java.awt.event.ComponentEvent
import javax.swing.*


object PopupStuffK {
    @JvmStatic
    fun main(args: Array<String>) {
        SwingUtilities.invokeLater {
            JFrame().apply {
                defaultCloseOperation = JFrame.EXIT_ON_CLOSE
                contentPane.apply {
                    background = Color(0x230B70)
                    add(frameContent())
                }
                size = Dimension(600, 400)

            }.isVisible = true
        }
    }

    private fun frameContent(): JComponent {
        var popup: Popup? = null
        val main = JPanel(BorderLayout()).apply main@{
            add(JButton("Pop up").apply button@{
                addActionListener { _ ->
                    if (popup != null) {
                        popup?.hide()
                        popup = null
                        return@addActionListener
                    }

                    val popupContent = popupContent()
                    val popupContentPrefSize = popupContent.preferredSize
                    val popupLocation: Point = this@button.locationOnScreen.apply {
                        x = x + this@button.width - popupContentPrefSize.width
                        y += this@button.height
                    }

                    popupContent.addPropertyChangeListener("preferredSize") { _ ->
                        // val w = SwingUtilities.getWindowAncestor(popupContent)
                        // w.location = this@button.locationOnScreen.apply {
                        //     x = x + this@button.width - popupContent.preferredSize.width
                        //     y += this@button.height
                        // }
                        // w.size = popupContent.preferredSize
                        println("preferredSize: ${popupContent.preferredSize}")
                    }

                    popupContent.addComponentListener(object : java.awt.event.ComponentAdapter() {
                        override fun componentResized(e: ComponentEvent?) {
                            val w = SwingUtilities.getWindowAncestor(popupContent)
                            w.location = this@button.locationOnScreen.apply {
                                x = x + this@button.width - popupContent.preferredSize.width
                                y += this@button.height
                            }
                            w.size = popupContent.preferredSize
                            w.doLayout()
                            w.repaint(1)
                            println("componentResized: ${popupContent.preferredSize}")
                        }
                    })

                    popup = PopupFactory.getSharedInstance().getPopup(
                        this@main,
                        popupContent,
                        popupLocation.x,
                        popupLocation.y
                    ).apply {
                        show()
                    }
                }
            }, BorderLayout.NORTH)
            isOpaque = false
        }
        return main
    }

    private fun popupContent(): JComponent {
        val secondary = JPanel().apply secondary@{
            preferredSize = Dimension(100, 100)
            isVisible = false
            isOpaque = false
        }

        val main = JPanel().apply main@{
            preferredSize = Dimension(100, 100)
            layout = BoxLayout(this, BoxLayout.Y_AXIS)

            add(JButton("Click").apply {
                addActionListener { _ ->
                    secondary.isVisible = !secondary.isVisible
                    secondary.parent.size = Dimension(this@main.preferredSize).apply {
                        if (secondary.isVisible) {
                            width += secondary.preferredSize.width
                        }
                    }
                }
            })
            isOpaque = false
        }

        return JPanel().apply {
            layout = BoxLayout(this, BoxLayout.X_AXIS)
            add(secondary)
            add(main)
            isOpaque = false
        }
    }
}
