package sandbox;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;

class ResizableSplitPane extends JSplitPane {
    private boolean painted;
    private double defaultDividerLocation;
    private final ResizableSplitPane resizableSplitPane = this;
    private double currentDividerLocation;
    private final Component first;
    private final Component second;
    private boolean dividerPositionCaptured = false;


    public ResizableSplitPane(int splitType, Component first, Component second, Component parent) {
        this(splitType, first, second, parent, 0.5);
    }

    public ResizableSplitPane(int splitType, Component first, Component second, Component parent, double defaultDividerLocation) {
        super(splitType, first, second);
        this.defaultDividerLocation = defaultDividerLocation;
        this.currentDividerLocation = defaultDividerLocation;
        this.setResizeWeight(defaultDividerLocation);
        this.first = first;
        this.second = second;
        parent.addComponentListener(new DividerLocator());
        first.addComponentListener(new DividerMovedByUserComponentAdapter());
    }

    public double getDefaultDividerLocation() {
        return defaultDividerLocation;
    }

    public void setDefaultDividerLocation(double defaultDividerLocation) {
        this.defaultDividerLocation = defaultDividerLocation;
    }


    @Override
    public void paint(Graphics g) {
        super.paint(g);
        if (!painted) {
            painted = true;
            this.setDividerLocation(currentDividerLocation);
        }
        dividerPositionCaptured = false;
    }

    private class DividerLocator extends ComponentAdapter {
        @Override
        public void componentResized(ComponentEvent e) {
            setDividerLocation(currentDividerLocation);
        }
    }

    private class DividerMovedByUserComponentAdapter extends ComponentAdapter {
        @Override
        public void componentResized(ComponentEvent e) {
            if (!dividerPositionCaptured) {
                dividerPositionCaptured = true;
                currentDividerLocation = (double) first.getWidth() / (double) (first.getWidth() + second.getWidth());
                System.out.println(currentDividerLocation);
            }
        }
    }
}