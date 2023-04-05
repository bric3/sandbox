package sandbox;
/*
 * sandbox
 *
 * Copyright (c) 2021,today - Brice Dutheil <brice.dutheil@gmail.com>
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */
import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.util.LinkedList;
import java.util.Queue;
import java.util.Random;

public class Baubles extends JPanel implements ActionListener {
    private static final int MAX = 64;
    private static final String TITLE = "Baubles";
    private static final Random rnd = new Random();
    private static final AlphaComposite ac =
        AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 0.75f);
    private final Timer timer = new Timer(100, this);
    private final Queue<Bauble> queue = new LinkedList<Bauble>();

    public static void main(String[] args) {
        EventQueue.invokeLater(() -> {
            JFrame f = new JFrame(TITLE);
            f.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
            Baubles dt = new Baubles();
            f.add(dt);
            f.pack();
            f.setLocationRelativeTo(null);
            f.setVisible(true);
        });
    }

    public Baubles() {
        this.setPreferredSize(new Dimension(8 * MAX, 8 * MAX));
        this.addComponentListener(new ComponentAdapter() {

            @Override // relocate any outliers
            public void componentResized(ComponentEvent e) {
                for (Bauble bauble : queue) {
                    if (bauble.x > getWidth() || bauble.y > getHeight()) {
                        bauble.x = rnd.nextInt(getWidth());
                        bauble.y = rnd.nextInt(getHeight());
                    }
                }
            }
        });
        timer.start();
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        if (queue.isEmpty()) {
            for (int i = 0; i < MAX; i++) {
                queue.add(randomBauble());
            }
        }
        queue.add(randomBauble());
        queue.remove();
        repaint();
    }

    @Override
    public void paintComponent(Graphics g) {
        var g2d = (Graphics2D) g;
        g2d.setRenderingHint(
            RenderingHints.KEY_ANTIALIASING,
            RenderingHints.VALUE_ANTIALIAS_ON
        );
        g2d.setColor(Color.black);
        g2d.fillRect(0, 0, getWidth(), this.getHeight());
        g2d.setComposite(ac);
        for (Bauble b : queue) {
            g2d.setColor(b.c);
            g2d.fillOval(b.x, b.y, b.d, b.d);
        }
    }

    private Bauble randomBauble() {
        return new Bauble(
                rnd.nextInt(getWidth()),
                rnd.nextInt(getHeight()),
                rnd.nextInt(MAX) + MAX / 2,
                new Color(rnd.nextInt())
        );
    }

    private static class Bauble {
        private int x, y, d;
        private Color c;

        public Bauble(int x, int y, int r, Color c) {
            this.x = x - r;
            this.y = y - r;
            this.d = 2 * r;
            this.c = c;
        }
    }
}
