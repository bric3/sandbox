package sandbox;

import com.jhlabs.image.GaussianFilter;

import java.awt.*;
import java.awt.image.BufferedImage;
import javax.swing.*;

public class ImageEffectUtilities {

    public static BufferedImage applyDropShadow(BufferedImage imgMaster, int size, Color color, float opactity) {
        return applyEffect(imgMaster, 0, 0, size, color, opactity);
    }

    protected static BufferedImage applyEffect(BufferedImage imgMaster, int xOffset, int yOffset, int size, Color color, float opactity) {

        BufferedImage imgShadow = generateShadow(imgMaster, size, color, opactity);

        BufferedImage imgCombined = createCompatibleImage(imgShadow);
        Graphics2D g2d = imgCombined.createGraphics();
        GraphicsUtilities.applyQualityRenderingHints(g2d);

        g2d.drawImage(imgShadow, -(size / 2), -(size / 2), null);
        g2d.drawImage(imgMaster, xOffset, yOffset, null);

        g2d.dispose();

        return imgCombined;

    }

    public static BufferedImage generateShadow(BufferedImage imgSource, int size, Color color, float alpha) {

        int imgWidth = imgSource.getWidth() + (size * 2);
        int imgHeight = imgSource.getHeight() + (size * 2);

        BufferedImage imgMask = createCompatibleImage(imgWidth, imgHeight);
        Graphics2D g2 = imgMask.createGraphics();

        int x = Math.round((imgWidth - imgSource.getWidth()) / 2f);
        int y = Math.round((imgHeight - imgSource.getHeight()) / 2f);
        g2.drawImage(imgSource, x, y, null);
        g2.dispose();

        // ---- Blur here ---

        BufferedImage imgGlow = generateBlur(imgMask, (size * 2), color, alpha);

        // ---- Blur here ----

        return imgGlow;

    }


    public static BufferedImage generateBlur(BufferedImage imgSource, int size, Color color, float alpha) {

        GaussianFilter filter = new GaussianFilter(size);

        int imgWidth = imgSource.getWidth();
        int imgHeight = imgSource.getHeight();

        BufferedImage imgBlur = createCompatibleImage(imgWidth, imgHeight);
        Graphics2D g2 = imgBlur.createGraphics();

        g2.drawImage(imgSource, 0, 0, null);
        g2.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_IN, alpha));
        g2.setColor(color);

        g2.fillRect(0, 0, imgSource.getWidth(), imgSource.getHeight());
        g2.dispose();

        imgBlur = filter.filter(imgBlur, null);

        return imgBlur;

    }

    public static BufferedImage createCompatibleImage(final BufferedImage bufferedImage) {
        return createCompatibleImage(bufferedImage.getWidth(), bufferedImage.getHeight(), bufferedImage.getTransparency());
    }

    public static BufferedImage createCompatibleImage(int width, int height) {
        return createCompatibleImage(width, height, Transparency.TRANSLUCENT);
    }

    public static BufferedImage createCompatibleImage(int width, int height, int transparency) {
        BufferedImage image = getGraphicsConfiguration().createCompatibleImage(width, height, transparency);
        image.coerceData(true);
        return image;
    }

    public static GraphicsConfiguration getGraphicsConfiguration() {
        return GraphicsEnvironment.getLocalGraphicsEnvironment().getDefaultScreenDevice().getDefaultConfiguration();
    }

    public static Rectangle getSafeBounds(JComponent comp) {
        Insets insets = comp.getInsets();

        return getSafeBounds(insets, comp.getBounds());
    }


    public static Rectangle getSafeBounds(Insets insets, Rectangle bounds) {
        int x = insets.left;
        int y = insets.top;
        int width = bounds.width - (insets.left + insets.right);
        int height = bounds.height - (insets.top + insets.bottom);

        return new Rectangle(x, y, width, height);
    }
}