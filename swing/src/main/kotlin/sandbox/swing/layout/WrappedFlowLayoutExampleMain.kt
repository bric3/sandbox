package sandbox.swing.layout

import java.awt.BorderLayout
import java.awt.event.ComponentAdapter
import java.awt.event.ComponentEvent
import javax.swing.*

fun main() {
  val panel = JPanel().apply {
    layout = WrappedFlowLayout().apply {
      alignOnBaseline = true
    }
    add(JButton("Button"))
    add(JButton("Button"))
    add(JButton("Button"))
    add(JButton("Button"))
    add(JButton("Button"))
    add(JButton("Button"))
    add(JButton("Button"))
  }

  val scrollPane = JScrollPane(panel).apply {
    border = BorderFactory.createEmptyBorder()
    // Relayout the WrappedFlowLayout when JScrollPane gets resized
    addComponentListener(object : ComponentAdapter() {
      override fun componentResized(event: ComponentEvent) {
        panel.revalidate()
      }
    })
  }

  val rootPanel = JPanel(BorderLayout()).apply {
    add(scrollPane)
  }

  val frame = JFrame("Test").apply {
    defaultCloseOperation = JFrame.DISPOSE_ON_CLOSE
    contentPane = rootPanel
    pack()
    isVisible = true
  }
}