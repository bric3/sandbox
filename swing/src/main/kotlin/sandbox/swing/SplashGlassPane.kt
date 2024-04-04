package sandbox.swing

import com.jhlabs.image.BoxBlurFilter
import java.awt.BorderLayout
import java.awt.Color
import java.awt.Container
import java.awt.Graphics
import java.awt.Graphics2D
import java.awt.event.FocusEvent
import java.awt.event.FocusListener
import java.awt.event.MouseAdapter
import java.awt.image.BufferedImage
import java.awt.image.BufferedImageOp
import javax.swing.*
import kotlin.properties.Delegates

/**
 * Glass pane used to blur the content of the window.
 */
class SplashGlassPane(private val container: Container) : JPanel(BorderLayout()), FocusListener {
  private var blurRadius: Float by Delegates.observable(2f) { _, _, newValue ->
    mOperation = BoxBlurFilter(newValue, newValue, 4)
  }
  private val blurRangeModel = DefaultBoundedRangeModel().apply {
    minimum = 1
    value = (blurRadius * 10).toInt()
    maximum = 100
    addChangeListener {
      blurRadius = value.toFloat() / 10.toFloat()
      repaint()
    }
  }
  init {
    addMouseListener(object : MouseAdapter() {})
    addMouseMotionListener(object : MouseAdapter() {})
    addFocusListener(this)
    isOpaque = false
    isFocusable = true
    background = Color(0, 0, 0, 190)

    add(JLabel("<html><font size=+4>Inactive</font></html>").apply { foreground = Color.WHITE }, BorderLayout.CENTER)
    add(JSlider(blurRangeModel), BorderLayout.SOUTH)
  }

  override fun setVisible(v: Boolean) {
    // Make sure we grab the focus so that key events don't go astray.
    if (v) {
      requestFocus()
    }
    super.setVisible(v)
  }

  // Once we have focus, keep it if we're visible
  override fun focusLost(fe: FocusEvent) {
    if (isVisible) {
      requestFocus()
    }
  }

  private lateinit var mOffscreenImage: BufferedImage
  /**
   * https://www.jhlabs.com/ip/blurring.html
   */
  private var mOperation: BufferedImageOp = BoxBlurFilter(blurRadius, blurRadius, 4)

  override fun paintComponent(g: Graphics) {
    if (container.width == 0 || container.height == 0) {
      return
    }
    val g2 = g as Graphics2D

    if (!this::mOffscreenImage.isInitialized
      || mOffscreenImage.width != container.width
      || mOffscreenImage.height != container.height
    ) {
      mOffscreenImage = BufferedImage(container.width, container.height, BufferedImage.TYPE_INT_ARGB)
    }
    val captureG2 = mOffscreenImage.createGraphics()
    captureG2.clip = g2.clip
    container.paint(captureG2)
    captureG2.dispose()

    g2.drawImage(mOffscreenImage, mOperation, 0, 0)

    g2.color = background
    g2.fillRect(0, 0, width, height)
  }

  override fun focusGained(fe: FocusEvent) {
    // nothing to do
  }

  companion object {
    @JvmStatic
    fun main(args: Array<String>) {
      with(JFrame("Test blurring")) {
        contentPane = JPanel(BorderLayout()).apply {
          add(JTextField("It's first component"), BorderLayout.NORTH)
          add(JTextField("It's second component"), BorderLayout.SOUTH)

          val btn = JButton("Start blur")
          btn.addActionListener {
            glassPane.isVisible = true
            val t = Timer(10_000) { glassPane.isVisible = false }
            t.isRepeats = false
            t.start()
          }
          add(btn)
        }
        setSize(500, 400)
        glassPane = SplashGlassPane(contentPane)

        defaultCloseOperation = WindowConstants.EXIT_ON_CLOSE
        isVisible = true
      }
    }
  }
}
