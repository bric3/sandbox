package sandbox.swing.laf

import java.awt.BorderLayout
import javax.swing.*
import javax.swing.border.EmptyBorder

object VioletMain {

  @JvmStatic
  fun main(args: Array<String>) {
    if (System.getProperty("os.name", "").startsWith("Mac OS")) {
      UIManager.setLookAndFeel("org.violetlib.aqua.AquaLookAndFeel");
    }

    SwingUtilities.invokeLater {
      JFrame("Violet Look and Feel").run {
        defaultCloseOperation = JFrame.EXIT_ON_CLOSE

        rootPane.run {
          putClientProperty("Aqua.backgroundStyle", "vibrantLight")
          putClientProperty("Aqua.windowTopMargin", 0)
          putClientProperty("Aqua.windowStyle", "transparentTitleBar")
          putClientProperty("jetbrains.awt.transparentTitleBarAppearance", true)
        }



        contentPane = JPanel(BorderLayout()).apply {

          add(JButton("Hello, World!"), BorderLayout.LINE_START)
          add(JScrollPane(JTextArea("Hello, World!")))
          border = EmptyBorder(30, 0, 0, 0)
          isOpaque = false
        }

        setSize(400, 200)
        isVisible = true
      }
    }
  }
}