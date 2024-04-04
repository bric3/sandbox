package sandbox.swing;

import org.jetbrains.annotations.NotNull;

import javax.swing.*;
import javax.swing.text.Document;
import javax.swing.text.Element;
import javax.swing.text.JTextComponent;
import javax.swing.text.ParagraphView;
import javax.swing.text.PlainDocument;
import javax.swing.text.View;
import javax.swing.text.ViewFactory;
import javax.swing.text.html.HTMLEditorKit;
import java.awt.*;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Supplier;


public class JEditorPaneBasedListCellRendererApp {
  public JComponent content() {
    var splitPane = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT);
    splitPane.setContinuousLayout(true);
    splitPane.setResizeWeight(0.5);
    splitPane.setDividerSize(2);

    var withJTextArea = makeList(() -> {
      var textComp = new JTextArea();
      textComp.setLineWrap(true);
      textComp.setWrapStyleWord(true);
      return textComp;
    });
    splitPane.setLeftComponent(withJTextArea);

    var withJEditorPane = makeList(() -> {
      var textComp = new JEditorPane();
      WrappingHTMLEditorKit kit = new WrappingHTMLEditorKit();
      kit.getStyleSheet().addRule(
              // language=CSS
              """
              body { font-family: 'Arial'; font-size: 12pt; }
              strong { color: #ffc400; }
              """
      );
      textComp.setEditorKit(kit);
      textComp.setEditable(false);
      return textComp;
    });
    splitPane.setRightComponent(withJEditorPane);
    return splitPane;
  }

  private @NotNull JScrollPane makeList(Supplier<JTextComponent> textComponentSupplier) {
    final var list = makeVariableRowHeightList();
    list.setCellRenderer(new JEditorPaneBasedListCellRenderer(textComponentSupplier));

    var scrollPane = new JScrollPane(list);
    scrollPane.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
    return scrollPane;
  }

  private @NotNull JList<String> makeVariableRowHeightList() {
    return new JList<>(items.toArray(String[]::new)) {
      {
        var list = this;
        addComponentListener(new ComponentAdapter() {
          @Override
          public void componentResized(ComponentEvent e) {
            // force cache invalidation by temporarily setting fixed height
            list.setFixedCellHeight(10);
            list.setFixedCellHeight(-1);
          }
        });
      }

      @Override
      public boolean getScrollableTracksViewportWidth() {
        return true;
      }
    };
  }

  static class JEditorPaneBasedListCellRenderer implements ListCellRenderer<String> {

    private final JTextComponent textComp;
    private final JPanel panel;

    private final Map<String, Document> panelCache = new IdentityHashMap<>();

    public JEditorPaneBasedListCellRenderer(Supplier<JTextComponent> textComponentSupplier) {
      this.textComp = textComponentSupplier.get();
      panel = new JPanel(new BorderLayout());
      panel.add(textComp);
      panel.add(new JLabel("GRAPH"), BorderLayout.WEST);
      panel.add(new JLabel("Some comment, eg length: " + 24), BorderLayout.SOUTH);
    }

    @Override
    public JComponent getListCellRendererComponent(JList<? extends String> list, String value, int index, boolean isSelected, boolean cellHasFocus) {
      switch (textComp) {
        case JEditorPane editorPane -> {
          var document = panelCache.computeIfAbsent(value, k -> {
            var doc = editorPane.getEditorKit().createDefaultDocument();
            editorPane.setDocument(doc);
            editorPane.setText(value);
            var document1 = editorPane.getDocument();

            assert doc == document1 : "Document should be the same";
            return doc;
          });
          editorPane.setDocument(document);
        }
        case JTextArea textArea -> {
          var document = panelCache.computeIfAbsent(value, k -> {
            var doc = new PlainDocument();
            textArea.setDocument(doc);
            textArea.setText(value);
            return doc;
          });
          textComp.setDocument(document);
        }
        default -> textComp.setText(value);
      }

      forceReshape(textComp, list.getWidth());
      return panel;
    }

    private void forceReshape(JTextComponent editorPane, int listCurrentWidth) {
      var parent = editorPane.getParent();
      if (parent == null) return;
      int othersWidth = parent.getWidth() - editorPane.getWidth();

      if (listCurrentWidth <= 0 || othersWidth < 0) return;

      int targetWidth = listCurrentWidth - othersWidth;
      editorPane.setSize(targetWidth, Short.MAX_VALUE);
      // editorPane.invalidate();
    }

  }

  public static void main() {
    var app = new JEditorPaneBasedListCellRendererApp();
    SwingUtilities.invokeLater(() -> {
      var frame = new JFrame();
      frame.setContentPane(app.content());
      frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
      frame.setSize(800, 300);
      frame.setVisible(true);
    });
  }

  static class WrappingHTMLEditorKit extends HTMLEditorKit {
    private final HTMLFactory factory = new HTMLFactory() {
      @Override
      public View create(Element elem) {
        View view = super.create(elem);

        return switch (view) {
          case ParagraphView ignored -> new ParagraphView(elem) {
            @Override
            protected SizeRequirements calculateMinorAxisRequirements(int axis, SizeRequirements r) {
              var req = Objects.requireNonNullElse(r, new SizeRequirements());
              req.minimum = (int) layoutPool.getMinimumSpan(axis);
              req.preferred = Math.max(req.minimum, (int) layoutPool.getPreferredSpan(axis));
              req.maximum = Integer.MAX_VALUE;
              req.alignment = 0.5f;
              return req;
            }
          };
          default -> view;
        };
      }
    };

    @Override
    public ViewFactory getViewFactory() {
      return factory;
    }

  }

  private static final List<String> items = List.of(
          "<html><body><strong>Area Map:</strong> A form of geospatial visualization, area maps are used to show specific values set over a map of a country, state, county, or any other geographic location. Two common types of area maps are choropleths and isopleths.</body></html>",
          "<html><body><strong>Bar Chart:</strong> Bar charts represent numerical values compared to each other. The length of the bar represents the value of each variable.</body></html>",
          "<html><body><strong>Heat Map:</strong> A type of geospatial visualization in map form which displays specific data values as different colors (this doesn’t need to be temperatures, but that is a common use).</body></html>",
          "<html><body><strong>Treemap:</strong> A type of chart that shows different, related values in the form of rectangles nested together.</body></html>",
          "<html><body><strong>Bullet Graph:</strong> A bar marked against a background to show progress or performance against a goal, denoted by a line on the graph.</body></html>",
          "<html><body><strong>Gantt Chart:</strong> Typically used in project management, Gantt charts are a bar chart depiction of timelines and tasks.</body></html>",
          "<html><body><strong>Pie Chart:</strong> A circular chart with triangular segments that shows data as a percentage of a whole.</body></html>",
          "<html><body><strong>Histogram:</strong> A bar chart that shows the frequency of data within a range.</body></html>",
          "<html><body><strong>Scatterplot:</strong> A chart that uses dots to represent values for two different variables. The position of the dot represents the value of the two variables.</body></html>",
          "<html><body><strong>Streamgraph:</strong> A type of stacked area graph which is displaced around a central axis, resulting in a flowing, organic shape.</body></html>",
          "<html><body><strong>Bax and whisker plot:</strong> A chart that shows the distribution of values in a dataset. It shows the minimum, first quartile, median, third quartile, and maximum of a dataset.</body></html>"
  );
}