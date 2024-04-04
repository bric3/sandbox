package sandbox.swing;

import javax.imageio.ImageIO;
import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.awt.geom.RoundRectangle2D;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.net.URISyntaxException;
import java.nio.file.Path;
import java.text.DateFormat;
import java.util.Date;
import java.util.Objects;

/**
 * From <a href="https://stackoverflow.com/a/23232618/48136">MadProgrammer's answer on SO</a>
 */
public class TransparentTest {

    public static void main(String[] args) {
        new TransparentTest();
    }

    public TransparentTest() {
        EventQueue.invokeLater(() -> {
            try {
                UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
            } catch (ClassNotFoundException | InstantiationException | IllegalAccessException | UnsupportedLookAndFeelException ignored) {
            }

            JFrame frame = new JFrame("Testing");
            frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
            frame.setLayout(new BorderLayout());
            frame.add(new TestPane());
            frame.pack();
            frame.setLocationRelativeTo(null);
            frame.setVisible(true);
        });
    }

    public class TestPane extends JPanel {
        private final BufferedImage background;
        private final BlurredGlassPane blurredGlassPane;

        public TestPane() {
            try {
                background = ImageIO.read(Objects.requireNonNull(getClass().getResourceAsStream("/pexels-erik-mclean-4061662.jpg")));
            } catch (IOException ex) {
                throw new UncheckedIOException(ex);
            }

            blurredGlassPane = new BlurredGlassPane();
            blurredGlassPane.setLayout(new GridBagLayout());
            InfoPane infoPane = new InfoPane();
            try {
                infoPane.setFile(Path.of(Objects.requireNonNull(getClass().getResource("/pexels-erik-mclean-4061662.jpg")).toURI()).toFile());
            } catch (URISyntaxException e) {
                throw new RuntimeException(e);
            }
            blurredGlassPane.add(infoPane);

            JButton click = new JButton("Click");
            click.addActionListener(e -> {
                Window win = SwingUtilities.getWindowAncestor(TestPane.this);
                if (win instanceof JFrame frame) {
                    frame.setGlassPane(blurredGlassPane);
                    blurredGlassPane.setVisible(true);
                }
            });

            setLayout(new GridBagLayout());
            add(click);
        }

        @Override
        public Dimension getPreferredSize() {
            return background == null ? new Dimension(200, 200) : new Dimension(background.getWidth(), background.getHeight());
        }

        @Override
        protected void paintComponent(Graphics g) {
            super.paintComponent(g);
            if (background != null) {
                Graphics2D g2d = (Graphics2D) g.create();
                int x = (getWidth() - background.getWidth()) / 2;
                int y = (getHeight() - background.getHeight()) / 2;
                g2d.drawImage(background, x, y, this);
                g2d.dispose();
            }
        }
    }

    public static class InfoPane extends JPanel {
        protected static final int RADIUS = 20;
        protected static final int FRAME = 4;
        protected static final int INSET = RADIUS + FRAME;
        protected static final DateFormat DATE_FORMAT = DateFormat.getDateTimeInstance();

        private final JLabel name;
        private final JLabel path;
        private JLabel length;
        private final JLabel lastModified;

        public InfoPane() {
            setBorder(new EmptyBorder(INSET, INSET, INSET, INSET));
            setOpaque(false);
            setLayout(new GridBagLayout());
            GridBagConstraints gbc = new GridBagConstraints();
            gbc.gridx = 0;
            gbc.gridy = 0;
            gbc.weightx = 1;
            gbc.anchor = GridBagConstraints.WEST;
            gbc.gridwidth = GridBagConstraints.REMAINDER;

            name = createLabel(Font.BOLD, 48);
            add(name, gbc);

            gbc.gridy++;
            path = createLabel();
            add(path, gbc);

            gbc = new GridBagConstraints();
            gbc.gridx = 0;
            gbc.gridy = 2;
            gbc.anchor = GridBagConstraints.WEST;

            length = createLabel();
            lastModified = createLabel();
            add(createLabel("Size: "), gbc);

            gbc.gridx++;
            gbc.insets = new Insets(0, 0, 0, 10);
            add(length, gbc);

            gbc.insets = new Insets(0, 0, 0, 0);
            gbc.gridx++;
            add(createLabel("Last Modified: "), gbc);

            gbc.gridx++;
            add(lastModified, gbc);
        }

        public JLabel createLabel(String text) {
            JLabel label = new JLabel(text);
            label.setForeground(Color.WHITE);
            return label;

        }

        public JLabel createLabel() {
            return createLabel("");
        }

        public JLabel createLabel(int style, float size) {
            JLabel label = createLabel();
            label.setFont(label.getFont().deriveFont(style, size));
            return label;
        }

        public void setFile(File file) {
            name.setText(file.getName());
            try {
                path.setText(file.getParentFile().getCanonicalPath());
            } catch (IOException ex) {
                ex.printStackTrace();
            }
            length.setText(HumanUnitsKt.formatBytes(file.length()));
            lastModified.setText(DATE_FORMAT.format(new Date(file.lastModified())));
        }

        @Override
        protected void paintComponent(Graphics g) {
            super.paintComponent(g); 
            Graphics2D g2d = (Graphics2D) g.create();
            GraphicsUtilities.applyQualityRenderingHints(g2d);
            int width = getWidth() - 1;
            int height = getHeight() - 1;
            int buffer = FRAME / 2;
            RoundRectangle2D base = new RoundRectangle2D.Double(buffer, buffer, width - FRAME, height - FRAME, RADIUS, RADIUS);
            g2d.setColor(new Color(0, 0, 0, 128));
            g2d.fill(base);
            g2d.setColor(Color.WHITE);
            g2d.setStroke(new BasicStroke(FRAME, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
            g2d.draw(base);
            g2d.dispose();
        }
    }

    public class BlurredGlassPane extends JPanel {

        private BufferedImage background;

        @Override
        public void setVisible(boolean visible) {
            if (visible) {
                Container parent = SwingUtilities.getAncestorOfClass(JRootPane.class, this);
                if (parent != null) {
                    JRootPane rootPane = (JRootPane) parent;

                    BufferedImage img = new BufferedImage(rootPane.getWidth(), rootPane.getHeight(), BufferedImage.TYPE_INT_ARGB);
                    Graphics2D g2d = img.createGraphics();
                    rootPane.printAll(g2d);
                    g2d.dispose();

                    background = ImageEffects.generateBlur(img, 40);
                }
            }
            super.setVisible(visible);
        }

        @Override
        protected void paintComponent(Graphics g) {
            super.paintComponent(g);
            g.drawImage(background, 0, 0, this);
        }
    }
}