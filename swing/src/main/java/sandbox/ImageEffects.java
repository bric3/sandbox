/*
 * sandbox
 *
 * Copyright (c) 2021,today - Brice Dutheil <brice.dutheil@gmail.com>
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

package sandbox;

import com.jhlabs.image.GaussianFilter;

import java.awt.*;
import java.awt.image.BufferedImage;

import static sandbox.GraphicsUtilities.createCompatibleImage;

public class ImageEffects {
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

    public static BufferedImage applyMask(BufferedImage sourceImage, BufferedImage maskImage, int method) {
        BufferedImage maskedImage = null;
        if (sourceImage != null) {
            int width = maskImage.getWidth(null);
            int height = maskImage.getHeight(null);

            maskedImage = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
            Graphics2D mg = maskedImage.createGraphics();

            int x = (width - sourceImage.getWidth(null)) / 2;
            int y = (height - sourceImage.getHeight(null)) / 2;

            mg.drawImage(sourceImage, x, y, null);
            mg.setComposite(AlphaComposite.getInstance(method));

            mg.drawImage(maskImage, 0, 0, null);

            mg.dispose();
        }

        return maskedImage;
    }

    public static BufferedImage generateBlur(BufferedImage imgSource, int size, Color color, float alpha) {
        GaussianFilter filter = new GaussianFilter(size);

        int imgWidth = imgSource.getWidth();
        int imgHeight = imgSource.getHeight();

        BufferedImage imgBlur = GraphicsUtilities.createCompatibleImage(imgWidth, imgHeight);
        Graphics2D g2 = imgBlur.createGraphics();

        g2.drawImage(imgSource, 0, 0, null);
        g2.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_IN, alpha));
        g2.setColor(color);

        g2.fillRect(0, 0, imgSource.getWidth(), imgSource.getHeight());
        g2.dispose();

        imgBlur = filter.filter(imgBlur, null);

        return imgBlur;

    }

    public static BufferedImage generateBlur(BufferedImage imgSource, int size) {
        GaussianFilter filter = new GaussianFilter(size);

        int imgWidth = imgSource.getWidth();
        int imgHeight = imgSource.getHeight();

        BufferedImage imgBlur = GraphicsUtilities.createCompatibleImage(imgWidth, imgHeight);
        Graphics2D g2 = imgBlur.createGraphics();

        g2.drawImage(imgSource, 0, 0, null);
        g2.dispose();

        imgBlur = filter.filter(imgBlur, null);

        return imgBlur;

    }

    public static BufferedImage generateGlow(BufferedImage imgSource, int size, Color color, float alpha) {
        int imgWidth = imgSource.getWidth() + (size * 2);
        int imgHeight = imgSource.getHeight() + (size * 2);

        BufferedImage imgMask = GraphicsUtilities.createCompatibleImage(imgWidth, imgHeight);
        Graphics2D g2 = imgMask.createGraphics();

        int x = Math.round((imgWidth - imgSource.getWidth()) / 2f);
        int y = Math.round((imgHeight - imgSource.getHeight()) / 2f);
        g2.drawImage(imgSource, x, y, null);
        g2.dispose();

        // ---- Blur here ---
        BufferedImage imgGlow = generateBlur(imgMask, size, color, alpha);

        // ---- Blur here ----
        imgGlow = applyMask(imgGlow, imgMask, AlphaComposite.DST_OUT);

        return imgGlow;
    }
}