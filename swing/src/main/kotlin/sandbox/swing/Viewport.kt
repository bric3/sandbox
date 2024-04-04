package sandbox.swing
import java.awt.BorderLayout
import java.awt.Color
import java.awt.Dimension
import java.awt.Point
import javax.swing.*


object Viewport {
    @JvmStatic
    fun main(args: Array<String>) {
        SwingUtilities.invokeLater {
            val left = JPanel(BorderLayout()).apply {
                background = Color(0xE3E3E3)
                isOpaque = true
                size = Dimension(200, 200)
            }

            val right = JPanel(BorderLayout()).apply {
                background = Color(0xF333F3)
                isOpaque = true
                size = Dimension(200, 200)
            }

            val container = JViewport().apply {
                view = JPanel(BorderLayout()).apply {
                    add(left, BorderLayout.WEST)
                    add(right, BorderLayout.EAST)
                }
                viewPosition = Point(0, 0)
            }

            JFrame("Viewport").run {
                defaultCloseOperation = JFrame.EXIT_ON_CLOSE
                contentPane = container
                size = Dimension(300, 200)
                isVisible = true
            }
        }
    }
}

