/*
 * sandbox
 *
 * Copyright (c) 2021,today - Brice Dutheil <brice.dutheil@gmail.com>
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */
@file:JvmName("QuickImageViewer")

package sandbox.swing

import java.awt.*
import java.awt.event.*
import java.awt.geom.AffineTransform
import java.awt.image.BufferedImage
import java.awt.image.IndexColorModel
import java.io.File
import java.io.IOException
import java.net.URI
import java.nio.file.Files
import java.nio.file.Path
import java.util.*
import javax.imageio.ImageIO
import javax.swing.*
import kotlin.math.max
import kotlin.math.min
import kotlin.system.exitProcess

@Throws(IOException::class)
fun main(args: Array<String>) {
  val firstArg = args.firstOrNull() ?: "https://jb.gg/badges/official.svg"
  val image = when {
    firstArg.startsWith("http") -> ImageIO.read(URI.create(firstArg).toURL())
    Files.exists(Path.of(firstArg)) -> ImageIO.read(File(firstArg))
    else -> {
      System.err.println("Cannot open '$firstArg'")
      exitProcess(1)
    }
  }

  if (image == null) {
    System.err.println(
      "Supported formats: " + Arrays.stream(ImageIO.getReaderFormatNames())
        .map(String::uppercase)
        .sorted()
        .distinct()
        .toArray()
        .joinToString()
    )
    exitProcess(1)
  }

  val toolkitImage = when {
    firstArg.startsWith("http") -> Toolkit.getDefaultToolkit().createImage(URI.create(firstArg).toURL())
    Files.exists(Path.of(firstArg)) -> Toolkit.getDefaultToolkit().createImage(File(firstArg).toURI().toURL())
    else -> null
  }
  if (toolkitImage != null) {
    Toolkit.getDefaultToolkit().prepareImage(image, -1, -1, null)
  }


  SwingUtilities.invokeAndWait {
    JFrame(firstArg).apply {
      defaultCloseOperation = WindowConstants.EXIT_ON_CLOSE
      rootPane.inputMap.put(
        KeyStroke.getKeyStroke(
          KeyEvent.VK_W,
          Toolkit.getDefaultToolkit().menuShortcutKeyMaskEx
        ),
        "window-close"
      )
      defaultCloseOperation = JFrame.DISPOSE_ON_CLOSE
      isLocationByPlatform = true
      val scroll = JScrollPane(
        JPanel().apply {
          layout = BoxLayout(this, BoxLayout.Y_AXIS)
          add(ImageZoomableView(image))
          add(
            JEditorPane(
              "text/html",
              "<html><body><img src='$firstArg' alt='Image from $firstArg'></body></html>"
            )
          )
        }

      ).apply {
        border = null
      }
      contentPane = JPanel(BorderLayout()).apply {
        add(scroll, BorderLayout.CENTER)
      }
      pack()
      isVisible = true
    }
  }
}

private class ImageZoomableView(private var image: Image) : JLabel(ScaledImageIcon(image)) {
  val checkeredBG: Paint = createTexture()
  val defaultBG: Color? = getDefaultBackground(image)
  var backgroundPaint: Paint = defaultBG ?: checkeredBG

  init {
    isOpaque = false
    cursor = Cursor.getPredefinedCursor(Cursor.CROSSHAIR_CURSOR)
    setupActions()
    componentPopupMenu = createPopupMenu()

    addMouseListener(object : MouseAdapter() {
      override fun mousePressed(e: MouseEvent) {
        if (e.isPopupTrigger) {
          componentPopupMenu.show(this@ImageZoomableView, e.x, e.y)
        }
      }
    })
  }

  fun setupActions() {
    bindAction(
      ZoomAction("Zoom in", 2.0), ZOOM_IN,
      KeyStroke.getKeyStroke('+'),
      KeyStroke.getKeyStroke(KeyEvent.VK_PLUS, Toolkit.getDefaultToolkit().menuShortcutKeyMaskEx),
      KeyStroke.getKeyStroke(KeyEvent.VK_ADD, Toolkit.getDefaultToolkit().menuShortcutKeyMaskEx)
    )
    bindAction(
      ZoomAction("Zoom out", .5), ZOOM_OUT,
      KeyStroke.getKeyStroke('-'),
      KeyStroke.getKeyStroke(KeyEvent.VK_MINUS, Toolkit.getDefaultToolkit().menuShortcutKeyMaskEx),
      KeyStroke.getKeyStroke(KeyEvent.VK_SUBTRACT, Toolkit.getDefaultToolkit().menuShortcutKeyMaskEx)
    )
    bindAction(
      ZoomAction("Zoom actual"), ZOOM_ACTUAL,
      KeyStroke.getKeyStroke('0'),
      KeyStroke.getKeyStroke(KeyEvent.VK_0, Toolkit.getDefaultToolkit().menuShortcutKeyMaskEx)
    )
    bindAction(
      ZoomToFitAction("Zoom fit"), ZOOM_FIT,
      KeyStroke.getKeyStroke('9'),
      KeyStroke.getKeyStroke(KeyEvent.VK_9, Toolkit.getDefaultToolkit().menuShortcutKeyMaskEx)
    )
  }

  fun bindAction(action: Action?, key: String?, vararg keyStrokes: KeyStroke?) {
    keyStrokes.forEach { getInputMap(WHEN_IN_FOCUSED_WINDOW).put(it, key) }
    actionMap.put(key, action)
  }

  fun createPopupMenu(): JPopupMenu {
    val popup = JPopupMenu()

    popup.add(actionMap[ZOOM_FIT])
    popup.add(actionMap[ZOOM_ACTUAL])
    popup.add(actionMap[ZOOM_IN])
    popup.add(actionMap[ZOOM_OUT])
    popup.addSeparator()

    val group = ButtonGroup()

    val background = JMenu("Background")
    popup.add(background)

    val checkered = ChangeBackgroundAction("Checkered", checkeredBG)
    checkered.putValue(Action.SELECTED_KEY, backgroundPaint === checkeredBG)
    addCheckBoxItem(checkered, background, group)
    background.addSeparator()
    addCheckBoxItem(ChangeBackgroundAction("White", Color.WHITE), background, group)
    addCheckBoxItem(ChangeBackgroundAction("Light", Color.LIGHT_GRAY), background, group)
    addCheckBoxItem(ChangeBackgroundAction("Gray", Color.GRAY), background, group)
    addCheckBoxItem(ChangeBackgroundAction("Dark", Color.DARK_GRAY), background, group)
    addCheckBoxItem(ChangeBackgroundAction("Black", Color.BLACK), background, group)
    background.addSeparator()
    val chooseBackgroundAction =
      ChooseBackgroundAction("Choose...", defaultBG ?: Color(0xFF6600))
    chooseBackgroundAction.putValue(Action.SELECTED_KEY, backgroundPaint === defaultBG)
    addCheckBoxItem(chooseBackgroundAction, background, group)

    return popup
  }

  fun addCheckBoxItem(pAction: Action?, pPopup: JMenu, pGroup: ButtonGroup) {
    val item = JCheckBoxMenuItem(pAction)
    pGroup.add(item)
    pPopup.add(item)
  }

  override fun paintComponent(g: Graphics) {
    val gr = g as Graphics2D
    gr.paint = backgroundPaint
    gr.fillRect(0, 0, width, height)
    super.paintComponent(g)
  }

  private open inner class ChangeBackgroundAction(pName: String?, protected var paint: Paint) : AbstractAction(pName) {
    override fun actionPerformed(e: ActionEvent) {
      backgroundPaint = paint
      repaint()
    }
  }

  private inner class ChooseBackgroundAction(name: String?, color: Color) : ChangeBackgroundAction(name, color) {
    init {
      putValue(SMALL_ICON, object : Icon {
        override fun paintIcon(c: Component, pGraphics: Graphics, x: Int, y: Int) {
          val g = pGraphics.create()
          g.color = paint as Color
          g.fillRect(x, y, 16, 16)
          g.dispose()
        }

        override fun getIconWidth(): Int = 16
        override fun getIconHeight(): Int = 16
      })
    }

    override fun actionPerformed(e: ActionEvent) {
      val selected = JColorChooser.showDialog(this@ImageZoomableView, "Choose background", paint as Color)
      if (selected != null) {
        paint = selected
        super.actionPerformed(e)
      }
    }
  }

  private open inner class ZoomAction(
    name: String?,
    private val zoomFactor: Double = 0.0,
  ) : AbstractAction(name) {
    override fun actionPerformed(e: ActionEvent) {
      if (zoomFactor <= 0) {
        icon = ScaledImageIcon(image)
      } else {
        val current = icon
        val w = max(
          min((current.iconWidth * zoomFactor).toInt().toDouble(), (image.getWidth(null) * 16).toDouble()),
          (image.getWidth(null) / 16).toDouble()
        ).toInt()
        val h = max(
          min((current.iconHeight * zoomFactor).toInt().toDouble(), (image.getHeight(null) * 16).toDouble()),
          (image.getHeight(null) / 16).toDouble()
        ).toInt()

        icon = ScaledImageIcon(
          image, max(w.toDouble(), 2.0).toInt(), max(h.toDouble(), 2.0).toInt()
        )
      }
    }
  }

  private inner class ZoomToFitAction(
    name: String?,
  ) : ZoomAction(name, -1.0) {
    override fun actionPerformed(e: ActionEvent) {
      var source = e.source as JComponent

      if (source is JMenuItem) {
        val menu = SwingUtilities.getAncestorOfClass(JPopupMenu::class.java, source) as JPopupMenu
        source = menu.invoker as JComponent
      }

      val container = SwingUtilities.getAncestorOfClass(JViewport::class.java, source)

      val ratioX = container.width / image.getWidth(null).toDouble()
      val ratioY = container.height / image.getHeight(null).toDouble()

      val zoomFactor = min(ratioX, ratioY)

      val w = max(
        min((image.getWidth(null) * zoomFactor).toInt().toDouble(), (image.getWidth(null) * 16).toDouble()),
        (image.getWidth(null) / 16).toDouble()
      ).toInt()
      val h = max(
        min((image.getHeight(null) * zoomFactor).toInt().toDouble(), (image.getHeight(null) * 16).toDouble()),
        (image.getHeight(null) / 16).toDouble()
      ).toInt()

      icon = ScaledImageIcon(image, w, h)
    }
  }

  companion object {
    const val ZOOM_IN: String = "zoom-in"
    const val ZOOM_OUT: String = "zoom-out"
    const val ZOOM_ACTUAL: String = "zoom-actual"
    const val ZOOM_FIT: String = "zoom-fit"

    private fun getDefaultBackground(img: Image): Color? {
      if (img is BufferedImage && img.colorModel is IndexColorModel) {
        val cm = img.colorModel as IndexColorModel
        val transparent = cm.transparentPixel
        if (transparent >= 0) {
          return Color(cm.getRGB(transparent), false)
        }
      }

      return null
    }

    private fun createTexture(): Paint {
      val graphicsConfiguration = GraphicsEnvironment.getLocalGraphicsEnvironment()
        .defaultScreenDevice
        .defaultConfiguration
      val pattern = graphicsConfiguration.createCompatibleImage(20, 20)
      val g = pattern.createGraphics()
      try {
        g.color = Color.WHITE
        g.fillRect(0, 0, pattern.width, pattern.height)
        g.color = Color.LIGHT_GRAY
        g.fillRect(0, 0, pattern.width / 2, pattern.height / 2)
        g.fillRect(pattern.width / 2, pattern.height / 2, pattern.width / 2, pattern.height / 2)
      } finally {
        g.dispose()
      }

      return TexturePaint(pattern, Rectangle(pattern.width, pattern.height))
    }
  }
}

class ScaledImageIcon(
  private val image: Image,
  private val width: Int,
  private val height: Int,
) : Icon {
  constructor(image: Image) : this(image, image.getWidth(null), image.getHeight(null))

  init {
    require(!(width <= 0 || height <= 0)) { String.format("Invalid size: %dx%d", width, height) }
  }

  override fun getIconHeight(): Int = height

  override fun getIconWidth(): Int = width

  override fun paintIcon(c: Component, g: Graphics, x: Int, y: Int) {
    g as Graphics2D
    val imageWidth = image.getWidth(null)
    val imageHeight = image.getHeight(null)
    if (imageWidth == width && imageHeight == height) {
      g.drawImage(image, x, y, width, height, null)
    } else {
      val transform = AffineTransform.getTranslateInstance(x.toDouble(), y.toDouble()).apply {
        scale(
          width / imageWidth.toDouble(),
          height / imageHeight.toDouble()
        )
      }
      g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR)
      g.drawImage(image, transform, null)
    }
  }
}