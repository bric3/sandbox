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

import java.awt.*;
import javax.swing.*;

public class SwingUtils {
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