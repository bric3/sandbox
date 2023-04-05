package sandbox;

import javax.imageio.ImageIO;
import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import java.awt.*;
import java.awt.geom.AffineTransform;
import java.awt.image.BufferedImage;
import java.net.URL;

public class GraphicsOnly extends JComponent implements ChangeListener {

    JPanel gui;
    /**
     * Displays the image.
     */
    JLabel imageCanvas;
    final Dimension size;
    double scale = 1.0;
    private BufferedImage image;

    public GraphicsOnly() {
        size = new Dimension(10, 10);
        setBackground(Color.black);
        try {
            image = ImageIO.read(new URL("https://i.stack.imgur.com/7bI1Y.jpg"));
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    public void setImage(Image image) {
        imageCanvas.setIcon(new ImageIcon(image));
    }

    public void initComponents() {
        if (gui == null) {
            gui = new JPanel(new BorderLayout());
            gui.setBorder(new EmptyBorder(5, 5, 5, 5));
            imageCanvas = new JLabel();
            JPanel imageCenter = new JPanel(new GridBagLayout());
            imageCenter.add(imageCanvas);
            JScrollPane imageScroll = new JScrollPane(imageCenter);
            imageScroll.setPreferredSize(new Dimension(300, 100));
            gui.add(imageScroll, BorderLayout.CENTER);
        }
    }

    public Container getGui() {
        initComponents();
        return gui;
    }

    public void stateChanged(ChangeEvent e) {
        int value = ((JSlider) e.getSource()).getValue();
        scale = value / 100.0;
        paintImage();
    }

    protected void paintImage() {
        int w = getWidth();
        int h = getHeight();
        int imageWidth = image.getWidth();
        int imageHeight = image.getHeight();
        BufferedImage bi = new BufferedImage(
                (int)(imageWidth*scale), 
                (int)(imageHeight*scale), 
                image.getType());
        Graphics2D g2 = bi.createGraphics();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING,
                RenderingHints.VALUE_ANTIALIAS_ON);
        double x = (w - scale * imageWidth) / 2;
        double y = (h - scale * imageHeight) / 2;
        AffineTransform at = AffineTransform.getTranslateInstance(0, 0);
        at.scale(scale, scale);
        g2.drawRenderedImage(image, at);
        setImage(bi);
    }

    public Dimension getPreferredSize() {
        int w = (int) (scale * size.width);
        int h = (int) (scale * size.height);
        return new Dimension(w, h);
    }

    private JSlider getControl() {
        JSlider slider = new JSlider(JSlider.HORIZONTAL, 0, 500, 50);
        slider.setMajorTickSpacing(50);
        slider.setMinorTickSpacing(25);
        slider.setPaintTicks(true);
        slider.setPaintLabels(true);
        slider.addChangeListener(this);
        return slider;
    }

    public static void main(String[] args) {
        GraphicsOnly app = new GraphicsOnly();
        JFrame frame = new JFrame();
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setContentPane(app.getGui());
        app.setImage(app.image);

        // frame.getContentPane().add(new JScrollPane(app));  
        frame.getContentPane().add(app.getControl(), "Last");
        frame.setSize(700, 500);
        frame.setLocation(200, 200);
        frame.setVisible(true);
    }
}