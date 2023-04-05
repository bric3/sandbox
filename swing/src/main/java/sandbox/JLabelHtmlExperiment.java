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
import java.awt.*;

public class JLabelHtmlExperiment {
  public static void main(String[] args) {
    SwingUtilities.invokeLater(() -> {
      var label = new JLabel();
      label.setText(
              CodeLabel.replaceCode(
                      """
                      <html>
                      <body  style='vertical-align:bottom;'>
                      Relative
                      <div>&rarr;<code>code 1</code>&nbsp;<code>code "2"</code></div>
                      </body>
                      </html>
                      """, "#131313", "#ffa500"
              )
      );
      // label.setText(
      //         """
      //         <html>
      //         <body  style='vertical-align:bottom;'>
      //         Relative
      //         <div>&rArr;<object classid='javax.swing.JLabel'><param name="text" value="&lt;html&gt;&lt;div style=&quot;padding:2px; background-color: #131313; color: #ffa500&quot;&gt;&lt;code&gt;code 1&lt;/code&gt;&lt;/div&gt;&lt;/html&gt;"></object>&nbsp;<code>code "2"</code></div>
      //         </body>
      //         </html>
      //         """
      // );

      var main = new JPanel(new BorderLayout());
      main.add(label, BorderLayout.CENTER);

      var jFrame = new JFrame("JLabel HTML Experiment");
      jFrame.setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
      jFrame.getContentPane().add(main);
      jFrame.setSize(300, 200);
      jFrame.setVisible(true);
    });
  }
}
