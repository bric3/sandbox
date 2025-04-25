package sandbox.swing

import java.awt.BorderLayout
import java.awt.Component
import java.awt.Shape
import java.io.StringWriter
import java.lang.System.Logger.Level.ERROR
import javax.swing.JEditorPane
import javax.swing.JFrame
import javax.swing.JScrollPane
import javax.swing.JViewport
import javax.swing.SwingUtilities
import javax.swing.event.DocumentEvent
import javax.swing.text.ComponentView
import javax.swing.text.EditorKit
import javax.swing.text.Element
import javax.swing.text.StyleConstants
import javax.swing.text.View
import javax.swing.text.ViewFactory
import javax.swing.text.html.HTML
import javax.swing.text.html.HTMLEditorKit

object JEditorPaneScrollableExample {
  val LOGGER: System.Logger = System.getLogger("JEditorPaneScrollableExample")

  @JvmStatic
  fun main(args: Array<String>) {
    LOGGER.log(System.Logger.Level.INFO, "JEditorPaneScrollableExample started")
    SwingUtilities.invokeLater {
      val frame = JFrame("JEditorPane with JScrollPane Inside")
      frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE)

      val editorPane = JEditorPane()
      editorPane.setContentType("text/html")
      editorPane.isEditable = false

      // Use a custom EditorKit to handle <custom>
      val editorKit = object : HTMLEditorKit() {
        private val delegateViewFactory = HTMLEditorKit().viewFactory as HTMLFactory

        override fun getViewFactory(): ViewFactory {
          return object : HTMLFactory() {
            override fun create(elem: Element): View? {
              val defaultView = delegateViewFactory.create(elem)
              val tagName = elem.attributes.getAttribute(StyleConstants.NameAttribute)
              if (tagName == HTML.Tag.DIV) {
                val attrs = elem.attributes

                // This doesn't work because the style attribute is not parsed by the default HTMLFactory,
                // it is unknown in the CSS attributes, and it is not easily extendable.
                // see javax.swing.text.html.StyleSheet.CssParser.handleValue
                // val style = attrs.getAttribute(HTML.Attribute.STYLE) as String?
                // if (containsOverflowXScroll(style)) {
                //   return ScrollableView(elem);
                // }

                if ("custom".equals(attrs.getAttribute(HTML.Attribute.NAME) as String?, ignoreCase = true)) {
                  return ScrollableView(elem)
                }
              }
              return defaultView
            }

            // private fun containsOverflowXScroll(style: String?): Boolean {
            //   style ?: return false
            //   // style.matches(".*overflow-x: *scroll.*".toRegex())
            //   val tokenizer = StringTokenizer(style, ";")
            //   while (tokenizer.hasMoreTokens()) {
            //     val token = tokenizer.nextToken().trim().lowercase()
            //     if (token.startsWith("overflow-x") && token.contains("scroll")) {
            //       return true
            //     }
            //   }
            //   return false
            // }
          }
        }
      }
      editorPane.setEditorKit(editorKit)

      // language=HTML
      val content = """
                 <html>
                 <body>
                 <p>Above the scrollable section</p>
                 <div name="custom" style="overflow-x: scroll;"> <!-- overflow-x not supported by Swing CSS -->
                     <table border='1'>
                         <thead>
                         <tr>
                             <th>Header 1</th>
                             <th>Header 2</th>
                             <th>Header 3</th>
                             <th>Header 4</th>
                             <th>Header 5</th>
                             <th>Header 6</th>
                             <th>Header 7</th>
                             <th>Header 8</th>
                         </tr>
                         </thead>
                         <tbody>
                         <tr>
                             <td>Data 1</td>
                             <td>Data 2</td>
                             <td>Data 3</td>
                             <td>Data 4</td>
                             <td>Data 5</td>
                             <td>Data 6</td>
                             <td>Data 7</td>
                             <td>Data 8</td>
                         </tr>
                         <tr>
                             <td>Data 9</td>
                             <td>Data 10</td>
                             <td>Data 11</td>
                             <td>Data 12</td>
                             <td>Data 13</td>
                             <td>Data 14</td>
                             <td>Data 15</td>
                             <td>Data 16</td>
                         </tr>
                         </tbody>
                     </table>
                 </div>
                 <p>Below the scrollable section</p>
                 </body>
                 </html>
                 
                 """.trimIndent()
      editorPane.text = content

      frame.contentPane.add(editorPane, BorderLayout.CENTER)
      frame.setSize(500, 300)
      frame.isVisible = true
    }
  }

  internal fun Element.text(): String =
    this.document.getText(this.startOffset, this.endOffset - this.startOffset)

  // Custom view to wrap HTML content inside a JScrollPane
  internal class ScrollableView(elem: Element?) : ComponentView(elem) {
    private lateinit var innerPane: JEditorPane

    override fun createComponent(): Component {
      val hostPane = this.hostPane
      innerPane = JEditorPane()
      val contentType = hostPane.contentType
      val kit = hostPane.getEditorKitForContentType(contentType).clone() as EditorKit
      innerPane.setEditorKitForContentType(contentType, kit)
      innerPane.setContentType(contentType)
      innerPane.isEditable = hostPane.isEditable

      innerPane.text = getInnerHTML(element, kit)

      val scrollPane = JScrollPane(
        JScrollPane.VERTICAL_SCROLLBAR_NEVER,
        JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED
      )
      scrollPane.setBorder(null)
      scrollPane.setViewportView(innerPane)
      scrollPane.viewport.setScrollMode(JViewport.BACKINGSTORE_SCROLL_MODE)
      return scrollPane
    }

    override fun insertUpdate(e: DocumentEvent?, a: Shape?, f: ViewFactory?) {
      // When the document is too big, the document is not completely parsed, so we need to listen to insert updates
      // and then update the inner pane content.
      // See:
      // HTMLEditorKit::read -> HTMLDocument$HTMLReader::flush -> HTMLDocument$HTMLReader::adjustEndElement -> HTMLDocument::fireChangedUpdate

      innerPane.text = getInnerHTML(element, innerPane.getEditorKit())
    }

    private fun getInnerHTML(elem: Element, kit: EditorKit): String = try {
      val writer = StringWriter()
      kit.write(
        writer,
        elem.document,
        elem.startOffset,
        elem.endOffset - elem.startOffset
      )
      writer.toString()
    } catch (e: Exception) {
      LOGGER.log(ERROR, "Could not render inner HTML: " + e.message)
      "Error loading content"
    }

    private val hostPane: JEditorPane
      get() {
        var c = getContainer()
        while ((c != null) && c !is JEditorPane) {
          c = c.parent
        }
        checkNotNull(c) { "No JEditorPane found in the hierarchy" }
        return c as JEditorPane
      }
  }
}