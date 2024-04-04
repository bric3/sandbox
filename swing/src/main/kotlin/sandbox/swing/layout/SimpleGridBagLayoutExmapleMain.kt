package sandbox.swing.layout

import java.awt.GridBagConstraints
import java.awt.GridBagLayout
import java.awt.Insets
import java.awt.event.MouseAdapter
import java.awt.event.MouseEvent
import javax.swing.*

/**
 * This shows an expandable panel using a GridBagLayout with a header that can be clicked to show or hide the content.
 * The same could be achieved with a BoxLayout for simple cases.
 */
fun main() {
  val constraints = GridBagConstraints()

  // Create the expandable panel
  val expandablePanel = JPanel(GridBagLayout())
  expandablePanel.isVisible = false

  // Add a label and text field to the expandable panel
  constraints.insets = Insets(5, 5, 5, 5)
  expandablePanel.add(JLabel("Expandable Label:"), constraints)
  constraints.gridx = 1
  constraints.weightx = 1.0
  constraints.fill = GridBagConstraints.BOTH
  expandablePanel.add(JTextField(20), constraints)

  // Create a panel for the header
  val headerPanel = JPanel(GridBagLayout())
  val chevronLabel = JLabel(">") // replace ">" with your chevron icon
  constraints.gridx = 0
  constraints.gridy = 0
  constraints.weightx = 0.0
  headerPanel.add(chevronLabel, constraints)
  constraints.gridx = 1
  constraints.weightx = 1.0
  constraints.fill = GridBagConstraints.HORIZONTAL
  headerPanel.add(JLabel("Title"), constraints)

  // Make the header panel respond to mouse clicks
  headerPanel.addMouseListener(object : MouseAdapter() {
    override fun mouseClicked(e: MouseEvent?) {
      expandablePanel.isVisible = !expandablePanel.isVisible
      chevronLabel.text = if (expandablePanel.isVisible) "v" else ">" // replace "v" and ">" with your chevron icons
    }
  })

  // Add the header panel and expandable panel to the frame
  val contentPanel = JPanel(GridBagLayout())
  constraints.gridx = 0
  constraints.gridy = 0
  constraints.anchor = GridBagConstraints.NORTHWEST
  contentPanel.add(headerPanel, constraints)
  constraints.gridy = 1
  constraints.weightx = 1.0
  constraints.weighty = 1.0
  contentPanel.add(expandablePanel, constraints)

  val frame = JFrame("Expandable Panel Example")
  frame.defaultCloseOperation = WindowConstants.EXIT_ON_CLOSE
  frame.contentPane.add(JPanel(GridBagLayout()).apply {
    constraints.gridx = 0
    constraints.gridy = 0
    constraints.anchor = GridBagConstraints.NORTHWEST
    add(contentPanel, constraints)
  })
  frame.setSize(300, 200)
  frame.isVisible = true
}