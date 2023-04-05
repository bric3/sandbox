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

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.util.regex.Pattern;

public class CodeLabel extends JLabel {
  private final Rectangle rv = new Rectangle();

  @Override
  public void updateUI() {
    super.updateUI();
    setBackground(Color.decode("#131313"));
    setForeground(Color.decode("#FFA500"));
    setFont(Font.getFont(Font.MONOSPACED, getFont()));

    setBorder(new EmptyBorder(1, 3, 1, 3));
  }

  @Override
  protected void paintComponent(Graphics g) {
    getBounds(rv);
    var g2 = (Graphics2D) g;
    g2.setColor(getBackground());
    g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
    g2.fillRoundRect(rv.x, rv.y, rv.width, rv.height, 8, 8);
    super.paintComponent(g);
  }

  public static String replaceCode(String text, String bgHex, String fgHex) {
    return Pattern.compile("<code>(.*?)</code>").matcher(text)
                  .replaceAll(match -> {
                                     var group = match.group(1);
                                     // Note the leading space as to be kept n case the previous character
                                     // is a special HTML character (like &nbsp;) the label isn't displayed
                                     var processedGroup = group.replace("\"", "&quot;");
                                     return " <object classid=\"javax.swing.JLabel\"><param name=\"text\" value=\"" +
                                            "&lt;html&gt;&lt;div style=&quot;padding: 1 3 1 3; background-color: " +
                                            bgHex +
                                            "; color: " +
                                            fgHex +
                                            "&quot;&gt;&lt;code&gt;" +
                                            processedGroup +
                                            "&lt;/code&gt;&lt;/div&gt;&lt;/html&gt;" +
                                            "\"></object>";
                                   }
                       );
  }
}
