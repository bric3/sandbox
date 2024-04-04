package sandbox.swing;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.util.function.Function;

/**
 * Reproducer for an issue with flickering popup when expanded to the left
 */
public class PopupStuffJ {

    public static final String OWNER_KEY = "popup.owner";
    public static final String LOCATION_PROVIDER_KEY = "popup.location.provider";

    private static Popup popup = null;


    /**
     * Resize and relocate the popup window, according to the preferred size of the popup content.
     */
    private static void resizeAndRelocatePopup(Function<JComponent, Point> locationProvider, JComponent popupContent) {
        var oldPopup = popup;
        // var popupWindow = SwingUtilities.getWindowAncestor(popupContent);
        var newSize = popupContent.getPreferredSize();
        popupContent.setSize(newSize);
        //
        // var rootPane = popupContent.getRootPane();
        // rootPane.setSize(newSize);
        // rootPane.getContentPane().setSize(newSize);
        // Container rootPaneParent = rootPane.getParent();
        //
        // // popupContent.validate();
        // // popupContent.paintImmediately(
        // //        0,
        // //        0,
        // //        popupContentPrefSize.width,
        // //        popupContentPrefSize.height
        // // );
        //
        var newPopupLocation = locationProvider.apply(popupContent);
        // popupWindow.setBounds(
        //         newPopupLocation.x,
        //         newPopupLocation.y,
        //         newSize.width,
        //         newSize.height
        // );

        popup = PopupFactory.getSharedInstance().getPopup(
                (JComponent) popupContent.getClientProperty(OWNER_KEY),
                popupContent,
                newPopupLocation.x,
                newPopupLocation.y
        );
        popup.show();
        oldPopup.hide();
    }

    /**
     * Compute location at bottom right of owner component, using the preferred width of the popup content.
     */
    private static Point locationAtBottomRight(JComponent owner, Dimension newSize) {
        var popupLocation = owner.getLocationOnScreen();
        popupLocation.x = popupLocation.x + owner.getWidth() - newSize.width;
        popupLocation.y += owner.getHeight();
        return popupLocation;
    }

    private static JButton buttonWithDropDown() {
        // This button is merely here to trigger the popup
        // In actual code, this triggered by a third party component
        var dropdownButton = new JButton("Dropdown Menu");
        dropdownButton.addActionListener(ae -> {
            if (popup != null) {
                popup.hide();
                popup = null;
                return;
            }

            // Computes the location of the popup according to the button and the popup content
            var popupContent = popupContent();
            popupContent.putClientProperty(OWNER_KEY, dropdownButton);
            Function<JComponent, Point> locationProvider = (JComponent c) -> locationAtBottomRight(dropdownButton, c.getPreferredSize());
            popupContent.putClientProperty(LOCATION_PROVIDER_KEY, locationProvider);

            // // This code that relocates and resizes the popup
            // popupContent.addComponentListener(new ComponentAdapter() {
            //     @Override
            //     public void componentResized(ComponentEvent e) {
            //         // resizeAndRelocatePopup(dropdownButton, popupContent);
            //
            //
            //         // System.out.println("popupContent.componentResized: " + popupContent.getPreferredSize() + " at " + popupContent.getLocationOnScreen());
            //         new Throwable().printStackTrace(System.out);
            //     }
            // });
            // When first displayed show popup at the right location
            var popupLocation = locationAtBottomRight(dropdownButton, popupContent.getPreferredSize());
            popup = PopupFactory.getSharedInstance().getPopup(
                    dropdownButton,
                    popupContent,
                    popupLocation.x,
                    popupLocation.y
            );
            var popupWindow = SwingUtilities.getWindowAncestor(popupContent);
            popupWindow.addComponentListener(new ComponentAdapter() {
                @Override
                public void componentResized(ComponentEvent e) {
                    System.out.println("popupWindow.componentResized: " + popupWindow.getSize() + " at " + popupWindow.getLocation());
//                    RepaintManager.currentManager(popupWindow).markCompletelyDirty(popupWindow);
                }

                @Override
                public void componentMoved(ComponentEvent e) {
                    System.out.println("popupWindow.componentMoved: " + popupWindow.getLocation() + " at " + popupWindow.getLocation());
                }
            });
            popupWindow.setName("popup");

            popup.show();

            setupAutoFollowParent(dropdownButton, popupContent);
        });
        return dropdownButton;
    }

    private static void setupAutoFollowParent(JComponent owner, JComponent popupContent) {
        var parentWindow = SwingUtilities.getWindowAncestor(owner);
        if (parentWindow != null) {
            final Point[] lastOwnerPoint = { parentWindow.getLocationOnScreen() };
            parentWindow.addComponentListener(new ComponentAdapter() {
                @Override
                public void componentMoved(ComponentEvent e) {
                    final Point newOwnerPoint = parentWindow.getLocationOnScreen();

                    int deltaX = lastOwnerPoint[0].x - newOwnerPoint.x;
                    int deltaY = lastOwnerPoint[0].y - newOwnerPoint.y;

                    lastOwnerPoint[0] = newOwnerPoint;

                    final Window popupWindow = SwingUtilities.getWindowAncestor(popupContent);
                    if (!popupWindow.isShowing()) return;

                    final Point current = popupWindow.getLocationOnScreen();

                    popupWindow.setLocation(new Point(current.x - deltaX, current.y - deltaY));
                }
            });
        }
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
        popupContent.setName("popupContent");

        // update the size of the popupContent panel,
        // to either expand or shrink the popup
        // when button More... is clicked
        moreButton.addActionListener(ae -> {
            System.out.println("\n\nmoreButton.actionPerformed: " + moreButton.getText());

            secondary.setVisible(!secondary.isVisible());
            moreButton.setText(secondary.isVisible() ? "Less..." : "More...");

            var newSize = new Dimension(main.getPreferredSize());
            if (secondary.isVisible()) {
                newSize.width += secondary.getPreferredSize().width;
            }
            secondary.setSize(secondary.getPreferredSize());
            popupContent.setPreferredSize(newSize);

            var locationProvider = (Function<JComponent, Point>) popupContent.getClientProperty(LOCATION_PROVIDER_KEY);
            resizeAndRelocatePopup(locationProvider, popupContent);
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
