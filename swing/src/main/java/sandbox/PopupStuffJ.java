package sandbox;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ComponentEvent;

public class PopupStuffJ {
    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            var jFrame = new JFrame();
            {
                jFrame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
                Container contentPane = jFrame.getContentPane();
                {
                    contentPane.setBackground(new Color(0x230B70));
                    contentPane.add(frameContent());
                }
                jFrame.setSize(new Dimension(600, 400));
            }
            jFrame.setVisible(true);
        });
    }

    private static JComponent frameContent() {
        var popup = new Object() {
            Popup ref = null;
        };
        var main = new JPanel(new BorderLayout());
        {
            var button = new JButton("Pop up");
            {
                button.addActionListener(ae -> {
                    if (popup.ref != null) {
                        popup.ref.hide();
                        popup.ref = null;
                        return;
                    }

                    var popupContent = popupContent();
                    var popupContentPrefSize = popupContent.getPreferredSize();
                    var popupLocation = button.getLocationOnScreen();
                    {
                        popupLocation.x = popupLocation.x + button.getWidth() - popupContentPrefSize.width;
                        popupLocation.y += button.getHeight();
                    }

//                    popupContent.addPropertyChangeListener("preferredSize", pce -> {
//                        var w = SwingUtilities.getWindowAncestor(popupContent);
//                        var newPopupLocation = button.getLocationOnScreen();
//                        {
//                            newPopupLocation.x = newPopupLocation.x + button.getWidth() - popupContent.getPreferredSize().width;
//                            newPopupLocation.y += button.getHeight();
//                        }
//                        w.setLocation(newPopupLocation);
//                        w.setSize(popupContent.getPreferredSize());
//                        System.out.println("preferredSize: ${popupContent.preferredSize}");
//                    });

                    popupContent.addComponentListener(new java.awt.event.ComponentAdapter() {
                        @Override
                        public void componentResized(ComponentEvent e) {
                            var w = SwingUtilities.getWindowAncestor(popupContent);
                            var newPopupLocation = button.getLocationOnScreen();
                            {
                                newPopupLocation.x = newPopupLocation.x + button.getWidth() - popupContent.getPreferredSize().width;
                                newPopupLocation.y += button.getHeight();
                            }
                            w.setLocation(newPopupLocation);
                            w.setSize(popupContent.getPreferredSize());
                            w.doLayout();
                            w.repaint(1);
                            System.out.println("componentResized: ${popupContent.preferredSize}");
                        }
                    });

                    popup.ref = PopupFactory.getSharedInstance().getPopup(
                            main,
                            popupContent,
                            popupLocation.x,
                            popupLocation.y
                    );
                    popup.ref.show();
                });
            }
            main.add(button, BorderLayout.NORTH);
            main.setOpaque(false);
        }
        return main;
    }

    private static JComponent popupContent() {
        var secondary = new JPanel();
        {
            secondary.setPreferredSize(new Dimension(100, 100));
            secondary.setVisible(false);
            secondary.setOpaque(false);
        }

        var main = new JPanel();
        {
            main.setLayout(new BoxLayout(main, BoxLayout.Y_AXIS));
            main.setPreferredSize(new Dimension(100, 100));

            var button = new JButton("Click");
            {
                button.addActionListener(ae -> {
                    secondary.setVisible(!secondary.isVisible());

                    var newSize = new Dimension(main.getPreferredSize());
                    {
                        if (secondary.isVisible()) {
                            newSize.width += secondary.getPreferredSize().width;
                        }
                    }
                    secondary.getParent().setSize(newSize);
                });
            }
            main.add(button);
            main.setOpaque(false);
        }

        var content = new JPanel();
        {
            content.setLayout(new BoxLayout(content, BoxLayout.X_AXIS));
            content.add(secondary);
            content.add(main);
        }
        return content;
    }
}
