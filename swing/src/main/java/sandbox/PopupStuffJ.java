package sandbox;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;

public class PopupStuffJ {
    /**
     * Resize and relocate the popup window, according to the preferred size of the popup content.
     */
    private static void resizeAndRelocatePopup(JComponent popupContent, JButton dropdownButton) {
        var popupWindow = SwingUtilities.getWindowAncestor(popupContent);
        var newPopupLocation = locationAtBottomRight(dropdownButton, popupContent);
        popupWindow.setLocation(newPopupLocation);
        popupWindow.setSize(popupContent.getPreferredSize());

        // failed attempt to avoid the popup getting painted too early
        popupWindow.revalidate();
    }

    /**
     * Compute location at bottom right of owner component, using the preferred width of the popup content.
     */
    private static Point locationAtBottomRight(JComponent owner, JComponent popupContent) {
        var popupLocation = owner.getLocationOnScreen();
        popupLocation.x = popupLocation.x + owner.getWidth() - popupContent.getPreferredSize().width;
        popupLocation.y += owner.getHeight();
        return popupLocation;
    }

    private static JButton buttonWithDropDown() {
        var popup = new Object() {
            Popup ref = null;
        };

        // This button is merely here to trigger the popup
        // In actual code, this triggered by a third party component
        var dropdownButton = new JButton("Dropdown Menu");
        dropdownButton.addActionListener(ae -> {
            if (popup.ref != null) {
                popup.ref.hide();
                popup.ref = null;
                return;
            }

            // Computes the location of the popup according to the button and the popup content
            var popupContent = popupContent();

            // This code that relocates and resizes the popup
            popupContent.addComponentListener(new ComponentAdapter() {
                @Override
                public void componentResized(ComponentEvent e) {
                    resizeAndRelocatePopup(popupContent, dropdownButton);
                    System.out.println("componentResized: " +popupContent.getPreferredSize());
                }
            });
            // When first displayed show popup at the right location
            var popupLocation = locationAtBottomRight(dropdownButton, popupContent);
            popup.ref = PopupFactory.getSharedInstance().getPopup(
                    dropdownButton,
                    popupContent,
                    popupLocation.x,
                    popupLocation.y
            );

            popup.ref.show();
        });
        return dropdownButton;
    }

    private static JComponent popupContent() {
        var secondary = new JPanel(new BorderLayout());
        secondary.setPreferredSize(new Dimension(100, 100));
        secondary.setVisible(false);
        secondary.setOpaque(false);
        secondary.add(new JTextArea("Expanded"), BorderLayout.CENTER);

        var main = new JPanel();
        main.setLayout(new BoxLayout(main, BoxLayout.Y_AXIS));
        main.setPreferredSize(new Dimension(100, 100));

        var textArea = new JTextArea("Click more to expand on the left");
        textArea.setLineWrap(true);
        textArea.setWrapStyleWord(true);
        main.add(textArea);

        var moreButton = new JButton("More...");
        main.add(moreButton);
        main.setOpaque(false);

        var popupContent = new JPanel();
        popupContent.setLayout(new BoxLayout(popupContent, BoxLayout.X_AXIS));
        popupContent.add(secondary);
        popupContent.add(main);

        // update the size of the popupContent panel,
        // to either expand or shrink the popup
        // when button More... is clicked
        moreButton.addActionListener(ae -> {
            secondary.setVisible(!secondary.isVisible());
            moreButton.setText(secondary.isVisible() ? "Less..." : "More...");

            var newSize = new Dimension(main.getPreferredSize());
            if (secondary.isVisible()) {
                newSize.width += secondary.getPreferredSize().width;
            }
            popupContent.setSize(newSize);
        });
        return popupContent;
    }

    private static JComponent frameContent() {
        var toolbar = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        toolbar.setComponentOrientation(ComponentOrientation.RIGHT_TO_LEFT);
        toolbar.add(new JLabel("xxxx"));
        toolbar.add(buttonWithDropDown());
        toolbar.setOpaque(false);

        var frameContent = new JPanel(new BorderLayout());
        frameContent.add(toolbar, BorderLayout.NORTH);
        frameContent.setOpaque(false);
        return frameContent;
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            var jFrame = new JFrame();
            jFrame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
            var contentPane = jFrame.getContentPane();
            contentPane.setBackground(new Color(0xFFC499));
            contentPane.add(frameContent());
            jFrame.setSize(new Dimension(600, 400));
            jFrame.setVisible(true);
        });
    }
}
