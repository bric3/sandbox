package sandbox.swing.rounded

import org.intellij.lang.annotations.MagicConstant
import sandbox.swing.rounded.PathBasedBackgroundClippingPanel.Companion.clipBottom
import sandbox.swing.rounded.PathBasedBackgroundClippingPanel.Companion.clipTop
import java.awt.BasicStroke
import java.awt.BorderLayout
import java.awt.Color
import java.awt.Component
import java.awt.Cursor
import java.awt.Graphics
import java.awt.Graphics2D
import java.awt.Insets
import java.awt.LayoutManager
import java.awt.Point
import java.awt.Rectangle
import java.awt.RenderingHints
import java.awt.Shape
import java.awt.geom.Area
import java.awt.geom.Path2D
import java.awt.geom.Rectangle2D
import java.awt.geom.RoundRectangle2D
import javax.swing.*
import javax.swing.border.Border
import javax.swing.border.EmptyBorder
import javax.swing.border.LineBorder
import kotlin.math.max
import kotlin.properties.Delegates


///////////////////////////////////////////////////////////////
//// DISCLAIMER: Clipping code does not yield good results ////
///////////////////////////////////////////////////////////////


fun main() {
  SwingUtilities.invokeLater {
    val content = JPanel().apply {
      layout = BoxLayout(this, BoxLayout.X_AXIS)
      background = Color(0xBBE1EC)
      border = BorderFactory.createEmptyBorder(30, 30, 30, 30)
      add(usingPathBasedClipping())
      add(Box.createHorizontalStrut(15))
      add(usingCombinedDiffRoundedPanel())
      add(Box.createHorizontalStrut(15))
      add(usingRoundedPanel())
      add(Box.createHorizontalStrut(15))
      add(usingCombinedDiffContainerPanel())
    }

    val jScrollPane = JScrollPane(content).apply {
      border = BorderFactory.createEmptyBorder()
      horizontalScrollBarPolicy = JScrollPane.HORIZONTAL_SCROLLBAR_ALWAYS
      verticalScrollBarPolicy = JScrollPane.VERTICAL_SCROLLBAR_NEVER
    }

    JFrame("Clipping").run {
      defaultCloseOperation = JFrame.EXIT_ON_CLOSE
      setSize(400, 400)
      contentPane = jScrollPane
      isVisible = true
    }
  }
}

private fun usingPathBasedClipping(): JComponent {
  val contentThatNeedToBeClipped = JPanel(BorderLayout()).apply {
    add(
      JLabel("PathBasedBackgroundClippingPanel::clipTop").apply {
        background = Color(0xFFC400)
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
      JLabel("PathBasedBackgroundClippingPanel::clipBottom").apply {
        background = Color(0x83D42D)
        isOpaque = true
        border = BorderFactory.createEmptyBorder(2, 5, 2, 5)
      }.clipBottom(),
      BorderLayout.SOUTH
    )
  }.apply {
    isOpaque = false // otherwise, the background color will be painted on below the clipped region
  }
  return contentThatNeedToBeClipped
}

fun usingCombinedDiffRoundedPanel(): JComponent {
  val contentThatNeedToBeClipped = CombinedDiffRoundedPanel(BorderLayout(), 20).apply {
    add(
      JLabel("Top").apply {
        background = Color(0xFFC400)
        isOpaque = true
        border = BorderFactory.createEmptyBorder(2, 5, 2, 5)
      },
      BorderLayout.NORTH
    )
    add(
      JLabel("CombinedDiffRoundedPanel").apply {
        background = Color(0xF1F1F1)
        isOpaque = true // Somewhat replaces the container background
        border = BorderFactory.createEmptyBorder(2, 5, 2, 5)
      },
      BorderLayout.CENTER
    )
    add(
      JLabel("Bottom").apply {
        background = Color(0x83D42D)
        isOpaque = true
        border = BorderFactory.createEmptyBorder(2, 5, 2, 5)
      },
      BorderLayout.SOUTH
    )
  }
  return contentThatNeedToBeClipped
}

fun usingCombinedDiffContainerPanel(): JComponent {
  val contentThatNeedToBeClipped = CombinedDiffContainerPanel(BorderLayout(), true).apply {
    add(
      JLabel("Top").apply {
        background = Color(0xFFC400)
        isOpaque = true
        border = BorderFactory.createEmptyBorder(2, 5, 2, 5)
      },
      BorderLayout.NORTH
    )
    add(
      JLabel("CombinedDiffContainerPanel").apply {
        background = Color(0xF1F1F1)
        isOpaque = true // Somewhat replaces the container background
        border = BorderFactory.createEmptyBorder(2, 5, 2, 5)
      },
      BorderLayout.CENTER
    )
    add(
      JLabel("Bottom").apply {
        background = Color(0x83D42D)
        isOpaque = true
        border = BorderFactory.createEmptyBorder(2, 5, 2, 5)
      },
      BorderLayout.SOUTH
    )
    arcRadius = 16
  }
  return contentThatNeedToBeClipped
}

fun usingRoundedPanel(): JComponent {
  val contentThatNeedToBeClipped = RoundedPanel.createRoundedPane().apply {
    contentPanel.apply {
      layout = BorderLayout()
      add(
        JLabel("Top").apply {
          background = Color(0xFFC400)
          isOpaque = true
          border = BorderFactory.createEmptyBorder(2, 5, 2, 5)
        },
        BorderLayout.NORTH
      )
      add(
        JLabel("RoundedPanel").apply {
          background = Color(0xF1F1F1)
          isOpaque = true // Somewhat replaces the container background
          border = BorderFactory.createEmptyBorder(2, 5, 2, 5)
        },
        BorderLayout.CENTER
      )
      add(
        JLabel("Bottom").apply {
          background = Color(0x83D42D)
          isOpaque = true
          border = BorderFactory.createEmptyBorder(2, 5, 2, 5)
        },
        BorderLayout.SOUTH
      )
    }

    border = RoundedBorder(
      RoundedPanel.ACTIVE_THICKNESS, RoundedPanel.ACTIVE_THICKNESS, RoundedPanel.SELECTED_BORDER_COLOR,
      RoundedPanel.RADIUS
    )
  }


  return contentThatNeedToBeClipped

}

/**
 * A simple wrapper panel.
 */
@Suppress("LeakingThis")
open class Wrapper(content: JComponent) : JPanel(BorderLayout()) {
  init {
    add(content, BorderLayout.CENTER)
  }
}

/**
 * A panel that clips its background (capture from its content component) to the top or bottom.
 *
 * To properly show clipped background, it disables the opacity
 * on the `content` component and captures its background color.
 *
 * Note about clip Antialiasing.
 * > Antialiasing the result of a clip operation may be more challenging if not impossible.
 * > The clip operation is very hard "by nature" (it might be handled by something like
 * > a [Stencil Buffer](https://en.wikipedia.org/wiki/Stencil_buffer) in hardware).
 */
private class PathBasedBackgroundClippingPanel(
  content: JComponent,
  @MagicConstant(intValues = [CLIP_TOP.toLong(), CLIP_BOTTOM.toLong()]) private val cornerClipping: Int,
  var arc: Float = 20f,
) : Wrapper(content) {
  companion object {
    const val CLIP_TOP = 0b01
    const val CLIP_BOTTOM = 0b10

    fun <T : JComponent> T.clipTop() = PathBasedBackgroundClippingPanel(
      this,
      CLIP_TOP,
    )

    fun <T : JComponent> T.clipBottom() = PathBasedBackgroundClippingPanel(
      this,
      CLIP_BOTTOM,
    )
  }


  init {
    content.isOpaque = false
    background = content.background
    size = content.size
    preferredSize = content.preferredSize
    minimumSize = content.minimumSize
    maximumSize = content.maximumSize
    isOpaque = true
  }

  override fun paintComponent(g: Graphics) {
    val g2 = g.create() as Graphics2D
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
    g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON)
    g2.setRenderingHint(RenderingHints.KEY_STROKE_CONTROL, RenderingHints.VALUE_STROKE_NORMALIZE)
    g2.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR)
    val oldClip = g2.clip
    g2.clip = clipRegion
    super.paintComponent(g2)
    g2.clip = oldClip

    g2.color = Color.GRAY
    g2.draw(clipRegion)
    g2.dispose()
  }

  private fun Path2D.Float.curveTo(
    controlPointX: Float,
    controlPointY: Float,
    endPointX: Float,
    endPointY: Float
  ) = curveTo(controlPointX, controlPointY, controlPointX, controlPointY, endPointX, endPointY)
}

// Copyright 2000-2024 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
/**
 * Slightly modified version of [CombinedDiffRoundedPanel] from IntelliJ IDEA
 * to round the whole component, (this panel has been removed upstream 9c06ed5895107511b79f9bce7a5b4aac877867b9).
 * Also fix some rendering issues with children components and border
 * if the children have a different BG color than this component.
 */
internal open class CombinedDiffRoundedPanel(
  layout: LayoutManager?,
  private val arc: Int = 8,
) : JPanel(layout) {

  init {
    isOpaque = false
    cursor = Cursor.getDefaultCursor()
    // border = if (!roundOnlyTopCorners) RoundedLineBorder(CombinedDiffUI.EDITOR_BORDER_COLOR, arc + 2)
    // else MyShiftedBorder(Color.GRAY, arc + 2)
    border = MyShiftedBorder(Color.GRAY, arc + 2)
  }

  override fun setOpaque(isOpaque: Boolean) {} // Disable opaque

  override fun paintChildren(g: Graphics) {
    val g2 = g.create() as Graphics2D
    try {
      g2.clip(getShape())
      super.paintChildren(g2)
      paintBorder(g2) // repaint the border after painting the children, because children are "hard" clipped
    } finally {
      g2.dispose()
    }
  }

  override fun paintComponent(g: Graphics) {
    val g2 = g.create() as Graphics2D
    try {
      g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON)
      g2.setRenderingHint(RenderingHints.KEY_STROKE_CONTROL, RenderingHints.VALUE_STROKE_NORMALIZE)
      g2.stroke = BasicStroke(1.0f, BasicStroke.CAP_BUTT, BasicStroke.JOIN_BEVEL)

      g2.clip(getShape())
      if (!isOpaque && isBackgroundSet) {
        g.color = background
        g.fillRoundRect(0, 0, width - 1, height - 1, arc, arc)
      }
      super.paintComponent(g)
    } finally {
      g2.dispose()
    }
  }

  private fun getShape(): Shape {
    val rect = Rectangle(size).apply {
      x += insets.left
      y += insets.top
      width -= insets.left + insets.right
      height -= insets.top + insets.bottom
    }

    return RoundRectangle2D.Float(
      /* x = */ rect.x.toFloat(),
      /* y = */ rect.y.toFloat(),
      /* w = */ rect.width.toFloat(),
      /* h = */ rect.height.toFloat() + 0.0f,
      /* arcw = */ arc.toFloat(),
      /* arch = */ arc.toFloat()
    )
  }

  private class MyShiftedBorder(
    color: Color,
    private val arc: Int,
  ) : LineBorder(color, 1) {
    override fun paintBorder(c: Component, g: Graphics, x: Int, y: Int, width: Int, height: Int) {
      val g2 = g as Graphics2D

      val oldAntialiasing = g2.getRenderingHint(RenderingHints.KEY_ANTIALIASING)
      g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON)
      g2.setRenderingHint(RenderingHints.KEY_STROKE_CONTROL, RenderingHints.VALUE_STROKE_NORMALIZE)
      g2.stroke = BasicStroke(1.1f, BasicStroke.CAP_BUTT, BasicStroke.JOIN_BEVEL)
      val oldColor = g2.color
      g2.color = lineColor

      g2.drawRoundRect(x, y, width - 1, height - 1, arc, arc)
      // g2.drawLine(x, height, width, height)

      g2.color = oldColor
      g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, oldAntialiasing)
    }
  }
}


/**
 * A very simple panel that paints a rounded rectangle as its background.
 */
open class SimpleBackgroundRoundedPanel(private val arcSize: Int) : JPanel() {
  override fun paintComponent(g: Graphics?) {
    if (!isOpaque) {
      return
    }
    // java.awt.Graphics2D#clip provides worse painting result than explicit rounded rectangle painting
    (g as? Graphics2D)?.let { g2 ->
      g2.color = background
      g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON)
      g2.setRenderingHint(RenderingHints.KEY_STROKE_CONTROL, RenderingHints.VALUE_STROKE_NORMALIZE)
      g2.fillRoundRect(0, 0, width, height, arcSize, arcSize)
    }
  }
}

// Copyright 2000-2024 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
/**
 * Simplified rounded panel, that should be used in combination with [RoundedBorder] to have the rounded effect.
 * Don't work well if content has a different background color.
 */
class RoundedPanel private constructor(unscaledRadius: Int = RADIUS) : JPanel(BorderLayout()) {
  companion object {
    const val RADIUS = 20
    const val THICKNESS = 1
    const val ACTIVE_THICKNESS = 2
    val SELECTED_BORDER_COLOR = Color(0x3574F0)
    val BORDER_COLOR = Color(0xD3D5DB)

    fun createRoundedPane(): RoundedPanel {
      return RoundedPanel(RADIUS)
    }
  }

  val contentPanel: JPanel = RoundedJPanel(unscaledRadius)

  init {
    add(contentPanel, BorderLayout.CENTER)
    isOpaque = false
  }

  private class RoundedJPanel(radius: Int) : JPanel() {

    private val radius = radius

    override fun paintComponent(g: Graphics) {
      val g2d = g as Graphics2D
      val oldAntiAliasing = g.getRenderingHint(RenderingHints.KEY_ANTIALIASING)
      g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON)
      val oldStrokeControl = g.getRenderingHint(RenderingHints.KEY_STROKE_CONTROL)
      g.setRenderingHint(RenderingHints.KEY_STROKE_CONTROL, RenderingHints.VALUE_STROKE_NORMALIZE)

      val ins = insets

      g2d.clip(
        RoundRectangle2D.Double(
          ins.left.toDouble(),
          ins.top.toDouble(),
          (width - ins.left - ins.right).toDouble(),
          (height - ins.left - ins.right).toDouble(),
          radius.toDouble(),
          radius.toDouble()
        )
      )
      super.paintComponent(g2d)

      g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, oldAntiAliasing)
      g.setRenderingHint(RenderingHints.KEY_STROKE_CONTROL, oldStrokeControl)
    }
  }
}

/**
 * A border that paints a rounded rectangle as its border.
 * Also fills the "background" of the component with the color of the content panel.
 */
open class RoundedBorder(
  unscaledAreaThickness: Int,
  unscaledThickness: Int,
  private val color: Color,
  unscaledRadius: Int
) : Border {
  private val areaThickness = unscaledAreaThickness
  private val thickness = unscaledThickness
  private val arcSize = unscaledRadius

  override fun paintBorder(c: Component, g: Graphics, x: Int, y: Int, width: Int, height: Int) {
    val g2 = g.create() as Graphics2D

    g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON)
    g2.setRenderingHint(RenderingHints.KEY_STROKE_CONTROL, RenderingHints.VALUE_STROKE_NORMALIZE)

    g2.color = c.background
    g2.fillRect(x, y, width, height)

    val gap = max(areaThickness - thickness, 0).toDouble()

    val area = createArea(x, y, width, height, arcSize, gap)
    if (c is RoundedPanel) {
      g2.color = c.contentPanel.background
      g2.fill(area)
    }

    val innerArea = createArea(x, y, width, height, arcSize, areaThickness.toDouble())

    g2.color = color
    area.subtract(innerArea)
    g2.fill(area)

    g2.dispose()
  }

  private fun createArea(x: Int, y: Int, width: Int, height: Int, arcSize: Int, th: Double): Area {
    val innerArc = max((arcSize - th), 0.0).toInt()
    return Area(
      RoundRectangle2D.Double(
        (x + th), (y + th),
        (width - (2 * th)), (height - (2 * th)),
        innerArc.toDouble(), innerArc.toDouble()
      )
    )
  }

  override fun getBorderInsets(c: Component?): Insets {
    return Insets(areaThickness, areaThickness, areaThickness, areaThickness)
  }

  override fun isBorderOpaque(): Boolean {
    return false
  }
}

// Copyright 2000-2024 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
// Old version was FillingRoundedRectanglePanel
// see
// * 9c06ed5895107511b79f9bce7a5b4aac877867b9
// * 9a2b498c38e0d20f45dc8cda788aa6e9d99d8d76
/**
 * A very special panel that does optimized painting for rounded combined diff panels.
 * Slightly modified to be able to tweak the arc radius, background color is null by default,
 * and no adherence to ij platform code.
 */
internal class CombinedDiffContainerPanel(
  layout: LayoutManager?, private val roundedBottom: Boolean
) : JPanel(layout) {
  var arcRadius: Int by Delegates.observable(12) { _, oldValue, newValue ->
    if (oldValue != newValue) repaint()
  }

  private val borderThickness: Int = 1
  var borderColor: Color by Delegates.observable(Color.GRAY) { _, oldValue, newValue ->
    if (oldValue != newValue) repaint()
  }

  var bottomBorderColor: Color by Delegates.observable(Color.GRAY) { _, oldValue, newValue ->
    if (oldValue != newValue) repaint()
  }

  init {
    // unscaled border for correct insets
    @Suppress("UseDPIAwareBorders")
    border = EmptyBorder(borderThickness, borderThickness, borderThickness, borderThickness)

    // By default, we don't want to paint the background of this panel
    background = null
  }

  override fun paint(g: Graphics) {
    super.paint(g)

    val outerBounds = Rectangle(Point(), size)
    val innerBounds = Rectangle(Point(), size).removeInsets(insets)

    val clipBounds = g.clipBounds
    val paintTopCap = intersectsTopCap(clipBounds, outerBounds)
    val paintBottomCap = intersectsBottomCap(clipBounds, outerBounds)

    val g2 = g.create() as Graphics2D
    try {
      if (paintTopCap) {
        paintCap(g2, innerBounds, outerBounds, true)
      }

      paintBody(g2, clipBounds, outerBounds)

      if (paintBottomCap) {
        if (roundedBottom) {
          paintCap(g2, innerBounds, outerBounds, false)
        }
        else {
          paintBottomBoxOutline(g2, outerBounds)
        }
      }
    }
    finally {
      g2.dispose()
    }
  }

  private fun paintCap(g: Graphics2D, innerBounds: Rectangle, outerBounds: Rectangle, top: Boolean) {
    GraphicsUtil.setupAAPainting(g)
    val arcRadius2D = arcRadius.toDouble()
    val fillColor = background
    if (fillColor != null) smoothCorners(g, innerBounds, arcRadius2D, fillColor, top)
    paintRoundedOutline(g, innerBounds, outerBounds, arcRadius2D, top)
  }

  private fun paintRoundedOutline(g: Graphics2D, innerBounds: Rectangle, outerBounds: Rectangle, arcRadius2D: Double, top: Boolean) {
    // we do fill here, because draw with AA screws us over and puts the lines outside the paint box
    val border = Path2D.Double(Path2D.WIND_EVEN_ODD).apply {
      appendRoundedBoxOutline(innerBounds, arcRadius2D, top)
      appendRoundedBoxOutline(outerBounds, arcRadius2D + borderThickness, top)
    }
    g.color = borderColor
    g.fill(border)
  }

  private fun smoothCorners(g: Graphics2D, bounds: Rectangle, arcRadius2D: Double, fillColor: Color, top: Boolean) {
    val cap = Path2D.Double(Path2D.WIND_EVEN_ODD).apply {
      appendRoundedBoxOutline(bounds, arcRadius2D, top)
      appendBoxOutline(bounds, arcRadius2D, top)
    }
    g.color = fillColor
    g.fill(cap)
  }

  private fun paintBody(g: Graphics, clipBounds: Rectangle, outerBounds: Rectangle) {
    val bodyBounds = Rectangle(outerBounds.x, outerBounds.y + arcRadius, outerBounds.width, outerBounds.height - arcRadius * 2)
    val toPaint = bodyBounds.intersection(Rectangle(outerBounds.x, clipBounds.y, outerBounds.width, clipBounds.height))
    if (toPaint.height <= 0) return

    GraphicsUtil.disableAAPainting(g)
    g.color = borderColor
    // left
    g.fillRect(toPaint.x, toPaint.y, borderThickness, toPaint.height)
    // right
    g.fillRect(toPaint.x + toPaint.width - borderThickness, toPaint.y, borderThickness, toPaint.height)
  }

  private fun paintBottomBoxOutline(g: Graphics2D, outerBounds: Rectangle) {
    GraphicsUtil.disableAAPainting(g)

    g.color = bottomBorderColor // sticky header bottom border color

    // bottom
    g.fillRect(outerBounds.x, outerBounds.y + height - borderThickness, outerBounds.width, borderThickness)

    g.color = borderColor

    val sideY = outerBounds.y + height - arcRadius
    // left
    g.fillRect(outerBounds.x, sideY, borderThickness, arcRadius)
    // right
    g.fillRect(outerBounds.x + outerBounds.width - borderThickness, sideY, borderThickness, arcRadius)
  }

  private fun intersectsTopCap(clipBounds: Rectangle, outerBounds: Rectangle) =
    clipBounds.y in outerBounds.y..arcRadius

  private fun intersectsBottomCap(clipBounds: Rectangle, outerBounds: Rectangle): Boolean {
    return clipBounds.y + clipBounds.height in outerBounds.height - arcRadius..outerBounds.height
  }

  private fun Path2D.appendBoxOutline(rect2D: Rectangle2D, outlineHeight: Double, top: Boolean) {
    val x = rect2D.x
    val y = rect2D.y
    val width = rect2D.width
    val height = rect2D.height

    val yShift = if (top) 0.0 else height
    val yArcShift = if (top) outlineHeight else height - outlineHeight

    moveTo(x, y + yArcShift)
    lineTo(x, y + yShift)
    lineTo(x + width, y + yShift)
    lineTo(x + width, y + yArcShift)
  }

  private fun Path2D.appendRoundedBoxOutline(rect2D: Rectangle2D, arcRadius2D: Double, top: Boolean) {
    val x = rect2D.x
    val y = rect2D.y
    val width = rect2D.width
    val height = rect2D.height

    val yShift = if (top) 0.0 else height
    val yArcShift = if (top) arcRadius2D else height - arcRadius2D

    moveTo(x, y + yArcShift)
    quadTo(x, y + yShift, x + arcRadius2D, y + yShift)
    lineTo(x + width - arcRadius2D, y + yShift)
    quadTo(x + width, y + yShift, x + width, y + yArcShift)
  }

  // From Swing's point of view children are painted in a rectangular box,
  // so when a repaint happens on a child, this panel will not clip the corners.
  // This property causes repaint of a child to trigger repaint of this panel.
  override fun isPaintingOrigin(): Boolean = true

  companion object {
    /**
     * @receiver rectangle whose size is to decrease and whose location is to move
     * @param insets the insets to remove
     */
    private fun Rectangle.removeInsets(insets: Insets): Rectangle {
      insets ?: return this
      this.x += insets.left
      this.y += insets.top
      this.width -= insets.left + insets.right
      this.height -= insets.top + insets.bottom
      return this
    }
  }
}

object GraphicsUtil {
  fun disableAAPainting(g: Graphics) {
    g as Graphics2D
    g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_OFF)
    g.setRenderingHint(RenderingHints.KEY_STROKE_CONTROL, RenderingHints.VALUE_STROKE_DEFAULT)
  }

  fun setupAAPainting(g: Graphics2D) {
    g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON)
    g.setRenderingHint(RenderingHints.KEY_STROKE_CONTROL, RenderingHints.VALUE_STROKE_NORMALIZE)
  }
}
