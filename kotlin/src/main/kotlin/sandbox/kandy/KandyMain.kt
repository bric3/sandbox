package sandbox.kandy

import org.jetbrains.kotlinx.kandy.dsl.continuous
import org.jetbrains.kotlinx.kandy.dsl.plot
import org.jetbrains.kotlinx.kandy.letsplot.feature.layout
import org.jetbrains.kotlinx.kandy.letsplot.layers.area
import org.jetbrains.kotlinx.kandy.letsplot.layers.bars
import org.jetbrains.kotlinx.kandy.letsplot.layers.line
import org.jetbrains.kotlinx.kandy.letsplot.layers.points
import org.jetbrains.kotlinx.kandy.letsplot.translator.toLetsPlot
import org.jetbrains.kotlinx.kandy.util.color.Color
import org.jetbrains.kotlinx.statistics.kandy.layers.heatmap
import org.jetbrains.kotlinx.statistics.kandy.stattransform.statSmooth
import org.jetbrains.kotlinx.statistics.plotting.smooth.SmoothMethod
import org.jetbrains.letsPlot.batik.plot.component.DefaultPlotPanelBatik
import org.jetbrains.letsPlot.commons.registration.Disposable
import org.jetbrains.letsPlot.core.util.MonolithicCommon
import org.jetbrains.letsPlot.geom.geomDensity
import org.jetbrains.letsPlot.geom.geomHistogram
import org.jetbrains.letsPlot.intern.toSpec
import org.jetbrains.letsPlot.letsPlot
import java.awt.Dimension
import java.awt.GridLayout
import javax.swing.BorderFactory
import javax.swing.Box
import javax.swing.BoxLayout
import javax.swing.ButtonGroup
import javax.swing.JFrame
import javax.swing.JFrame.EXIT_ON_CLOSE
import javax.swing.JPanel
import javax.swing.JRadioButton
import javax.swing.SwingUtilities

fun main() {
  val rand = java.util.Random()
  val n = 200
  val data = mapOf<String, Any>(
      "x" to List(n) { rand.nextGaussian() }
  )

  val plots = mapOf(
      "Plot1" to plot {
        bars {
          x(listOf(1, 2, 3))
          y(listOf(4, 5, 6))
        }
      }.toLetsPlot(),
      "Plot2" to plot {
        line {
          x(listOf(1, 2, 3))
          y(listOf(4, 5, 6))
        }
        layout {
          title = "Plot title"
          // size = 800 to 300
        }
      }.toLetsPlot(),
      "Density" to letsPlot(data) + geomDensity(
          color = "dark-green",
          fill = "green",
          alpha = .3,
          size = 2.0
      ) { x = "x" },
      "Count" to letsPlot(data) + geomHistogram(
          color = "dark-green",
          fill = "green",
          alpha = .3,
          size = 2.0
      ) { x = "x" },
      "Points gradient" to plot {
        val random = kotlin.random.Random(42)
        val xs = List(100) { random.nextDouble(0.0, 10.0) }
        val ys = List(100) { random.nextDouble(0.0, 10.0) }
        val gradient = List(100) { random.nextDouble(0.0, 100.0) }
        points {
          x(xs)
          y(ys)
          size = 7.5
          color(gradient) {
            scale = continuous(Color.LIGHT_BLUE..Color.PURPLE, domain = 0.0..100.0)
          }
        }
        layout.title = "Gradients"
      }.toLetsPlot(),
      // https://kotlin.github.io/kandy/heatmap-simple.html
      "Heatmap" to plot {
        val days = listOf(
            "Mon", "Tue", "Wed", "Thu", "Fri", "Sat", "Sun", "Sun",
            "Sat", "Thu", "Fri", "Tue", "Wed", "Sun", "Mon", "Thu",
            "Sun", "Sat", "Tue", "Mon", "Thu", "Wed", "Fri", "Sat",
            "Tue", "Sun", "Fri", "Sat", "Thu", "Mon", "Wed", "Tue",
            "Thu", "Mon", "Sun", "Fri", "Wed", "Sat", "Tue", "Thu",
            "Sat", "Tue", "Sun", "Mon", "Wed", "Fri", "Thu", "Sat",
            "Thu", "Fri", "Sun", "Tue", "Sat", "Wed", "Mon", "Thu",
            "Wed", "Tue", "Sat", "Fri", "Sun", "Thu", "Mon", "Tue",
            "Fri", "Thu", "Wed", "Sun", "Sat", "Mon", "Tue", "Thu",
            "Tue", "Wed", "Sun", "Mon", "Thu", "Sat", "Fri", "Tue",
            "Thu", "Sun", "Fri", "Sat", "Mon", "Wed", "Tue", "Thu",
            "Sat", "Mon", "Tue", "Thu", "Fri", "Sun", "Wed", "Sat",
            "Sun", "Fri", "Tue", "Thu", "Sat", "Mon", "Wed", "Sun",
            "Mon", "Wed", "Sat", "Fri", "Thu", "Tue", "Sun", "Sat",
        )
        val drinks = listOf(
            "soda", "tea", "coffee", "tea", "soda", "tea", "coffee", "soda",
            "coffee", "soda", "tea", "coffee", "soda", "tea", "coffee", "tea",
            "coffee", "soda", "tea", "soda", "coffee", "tea", "soda", "coffee",
            "soda", "tea", "coffee", "tea", "soda", "coffee", "tea", "soda",
            "tea", "soda", "coffee", "tea", "soda", "coffee", "soda", "tea",
            "coffee", "soda", "tea", "soda", "coffee", "tea", "soda", "coffee",
            "soda", "coffee", "tea", "soda", "coffee", "soda", "tea", "coffee",
            "soda", "coffee", "tea", "soda", "tea", "soda", "coffee", "tea",
            "tea", "coffee", "soda", "tea", "coffee", "soda", "tea", "soda",
            "tea", "soda", "coffee", "soda", "tea", "coffee", "soda", "coffee",
            "tea", "coffee", "soda", "tea", "soda", "coffee", "soda", "tea",
            "coffee", "soda", "tea", "coffee", "tea", "soda", "coffee", "soda",
            "soda", "tea", "coffee", "soda", "tea", "coffee", "soda", "tea",
            "coffee", "tea", "soda", "coffee", "tea", "soda", "coffee", "soda"
        )
        heatmap(days, drinks)
      }.toLetsPlot(),
      // https://kotlin.github.io/kandy/smoothed-area.html
      "Smoothed Area with Points" to plot {
        val xs = listOf(-3.0, -2.5, -2.0, -1.5, -1.0, 0.0, 1.0, 1.5, 2.0, 2.5, 3.0)
        val ys = listOf(5.4, 1.2, 3.4, 0.7, 0.8, 2.1, 0.6, 2.2, 3.4, 4.5, 6.7)

        statSmooth(xs, ys, method = SmoothMethod.LOESS(span = 0.3)) {
          area {
            x(Stat.x)
            y(Stat.y)
            alpha = 0.75
            fillColor = Color.LIGHT_GREEN
            borderLine.color = Color.LIGHT_PURPLE
          }
        }
        points {
          size = 4.0
          color = Color.ORANGE
          x(xs)
          y(ys)
        }
      }.toLetsPlot()
  )

  val selectedPlotKey = plots.keys.first()
  val controller = Controller(
      plots,
      selectedPlotKey,
      false
  )

  val window = JFrame("Example App (Swing-Batik)")
  window.defaultCloseOperation = EXIT_ON_CLOSE
  window.contentPane.layout = BoxLayout(window.contentPane, BoxLayout.Y_AXIS)

  // Add controls
  val controlsPanel = Box.createHorizontalBox().apply {
    // Plot selector
    val plotButtonGroup = ButtonGroup()
    for (key in plots.keys) {
      plotButtonGroup.add(
          JRadioButton(key, key == selectedPlotKey).apply {
            addActionListener {
              controller.plotKey = this.text
            }
          }
      )
    }

    this.add(Box.createHorizontalBox().apply {
      border = BorderFactory.createTitledBorder("Plot")
      for (elem in plotButtonGroup.elements) {
        add(elem)
      }
    })

    // Preserve aspect ratio selector
    val aspectRadioButtonGroup = ButtonGroup()
    aspectRadioButtonGroup.add(JRadioButton("Original", false).apply {
      addActionListener {
        controller.preserveAspectRadio = true
      }
    })
    aspectRadioButtonGroup.add(JRadioButton("Fit container", true).apply {
      addActionListener {
        controller.preserveAspectRadio = false
      }
    })

    this.add(Box.createHorizontalBox().apply {
      border = BorderFactory.createTitledBorder("Aspect ratio")
      for (elem in aspectRadioButtonGroup.elements) {
        add(elem)
      }
    })
  }
  window.contentPane.add(controlsPanel)

  // Add plot panel
  val plotContainerPanel = JPanel(GridLayout())
  window.contentPane.add(plotContainerPanel)

  controller.plotContainerPanel = plotContainerPanel
  controller.rebuildPlotComponent()

  SwingUtilities.invokeLater {
    window.pack()
    window.size = Dimension(850, 400)
    window.setLocationRelativeTo(null)
    window.isVisible = true
  }
}

private class Controller(
    private val plots: Map<String, org.jetbrains.letsPlot.intern.Plot>,
    initialPlotKey: String,
    initialPreserveAspectRadio: Boolean
) {
  var plotContainerPanel: JPanel? = null
  var plotKey: String = initialPlotKey
    set(value) {
      field = value
      rebuildPlotComponent()
    }
  var preserveAspectRadio: Boolean = initialPreserveAspectRadio
    set(value) {
      field = value
      rebuildPlotComponent()
    }

  fun rebuildPlotComponent() {
    plotContainerPanel?.let {
      val container = plotContainerPanel!!
      // cleanup
      for (component in container.components) {
        if (component is Disposable) {
          component.dispose()
        }
      }
      container.removeAll()

      // build
      container.add(createPlotPanel())
      container.revalidate()
    }
  }

  fun createPlotPanel(): JPanel {
    val rawSpec = plots[plotKey]!!.toSpec()
    val processedSpec = MonolithicCommon.processRawSpecs(rawSpec, frontendOnly = false)

    return DefaultPlotPanelBatik(
        processedSpec = processedSpec,
        preserveAspectRatio = preserveAspectRadio,
        preferredSizeFromPlot = false,
        repaintDelay = 10,
    ) { messages ->
      for (message in messages) {
        println("[Example App] $message")
      }
    }
  }
}