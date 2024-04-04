package sandbox.swing;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.border.TitledBorder;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import java.awt.*;

public class Test100 {

    public static void main(String[] args) {
        new Test100();
    }

    public Test100() {
        EventQueue.invokeLater(() -> {
            try {
                UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
            } catch (ClassNotFoundException | InstantiationException | IllegalAccessException | UnsupportedLookAndFeelException ex) {
                ex.printStackTrace();
            }

            JFrame frame = new JFrame("Testing");
            frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
            frame.add(new SearchPane());
            frame.pack();
            frame.setLocationRelativeTo(null);
            frame.setVisible(true);
        });
    }

    public class SearchPane extends JPanel {

        private ObjectsPane objectsPane;
        private AdvanceSettingsPane advanceSettingsPane;

        public SearchPane() {
            setBorder(new EmptyBorder(8, 8, 8, 8));
            setLayout(new GridBagLayout());
            GridBagConstraints gbc = new GridBagConstraints();
            gbc.gridx = 0;
            gbc.gridy = 0;
            gbc.weightx = 1;
            gbc.weighty = 1;
            gbc.fill = GridBagConstraints.BOTH;


            objectsPane = new ObjectsPane();
            add(objectsPane, gbc);

            gbc.gridy++;
            gbc.weighty = 0;

            advanceSettingsPane = new AdvanceSettingsPane();
            advanceSettingsPane.setVisible(false);
            add(advanceSettingsPane, gbc);

            objectsPane.addExpandCollapseListener(e -> {
                System.out.println(objectsPane.isExpanded());
                advanceSettingsPane.setVisible(objectsPane.isExpanded());
                Window window = SwingUtilities.windowForComponent(SearchPane.this);
                window.pack();
            });
        }

        public class ObjectsPane extends JPanel {

            private JSpinner findField;
            private JTextField replaceField;

            private JButton expandButton;
            private JButton replaceButton;
            private JButton replaceAllButton;

            private boolean expanded = false;

            public ObjectsPane() {
                setLayout(new GridBagLayout());

                findField = new JSpinner(new AbstractSpinnerModel() {

                    @Override
                    public Object getValue() {
                        return "";
                    }

                    @Override
                    public void setValue(Object value) {
                    }

                    @Override
                    public Object getNextValue() {
                        return "";
                    }

                    @Override
                    public Object getPreviousValue() {
                        return "";
                    }
                });
                replaceField = new JTextField(10);

                replaceButton = new JButton("Replace");
                replaceAllButton = new JButton("Replace All");
                expandButton = new JButton("+");

                GridBagConstraints gbc = new GridBagConstraints();
                gbc.insets = new Insets(4, 4, 4, 4);
                gbc.gridx = 0;
                gbc.gridy = 0;
                gbc.gridwidth = GridBagConstraints.REMAINDER;
                gbc.weightx = 1;
                gbc.anchor = GridBagConstraints.WEST;
                add(new JLabel("Objects found:"), gbc);

                gbc.gridx = 0;
                gbc.gridy = 1;
                gbc.gridwidth = 1;
                gbc.weightx = 0;
                add(new JLabel("Find:"), gbc);

                gbc.gridy = 2;
                add(new JLabel("Replace:"), gbc);

                gbc.gridx = 1;
                gbc.gridy = 1;
                gbc.weightx = 1;
                gbc.fill = GridBagConstraints.HORIZONTAL;
                gbc.gridwidth = GridBagConstraints.REMAINDER;
                add(findField, gbc);

                gbc.gridy = 2;
                add(replaceField, gbc);

                gbc.anchor = GridBagConstraints.WEST;
                gbc.gridwidth = 1;
                gbc.weightx = 0;
                gbc.gridx = 0;
                gbc.gridy = 3;
                gbc.fill = GridBagConstraints.NONE;
                add(expandButton, gbc);

                JPanel pnlButtons = new JPanel(new GridLayout(1, 2));
                pnlButtons.add(replaceButton);
                pnlButtons.add(replaceAllButton);

                gbc.gridx = 1;
                gbc.gridy = 3;
                gbc.fill = GridBagConstraints.HORIZONTAL;
                gbc.weightx = 1;
                gbc.gridwidth = GridBagConstraints.REMAINDER;
                add(pnlButtons, gbc);

                expandButton.addActionListener(e -> {
                    expanded = !expanded;
                    if (expanded) {
                        expandButton.setText("-");
                    } else {
                        expandButton.setText("+");
                    }
                    fireStateChanged();
                });
            }

            public boolean isExpanded() {
                return expanded;
            }

            public void addExpandCollapseListener(ChangeListener listener) {
                listenerList.add(ChangeListener.class, listener);
            }

            public void removeExpandCollapseListener(ChangeListener listener) {
                listenerList.remove(ChangeListener.class, listener);
            }

            protected void fireStateChanged() {
                ChangeListener[] listeners = listenerList.getListeners(ChangeListener.class);
                if (listeners.length > 0) {

                    ChangeEvent evt = new ChangeEvent(this);
                    for (ChangeListener listener : listeners) {
                        listener.stateChanged(evt);
                    }

                }
            }

        }

        public class AdvanceSettingsPane extends JPanel {

            public AdvanceSettingsPane() {
                setBorder(new TitledBorder("Advance Settings"));
                setLayout(new GridBagLayout());
                GridBagConstraints gbc = new GridBagConstraints();
                gbc.gridx = 0;
//              gbc.gridy = 0;
                gbc.weightx = 1;
                gbc.anchor = GridBagConstraints.WEST;
                gbc.gridwidth = GridBagConstraints.REMAINDER;

                add(new JCheckBox("Live Update"), gbc);
                add(new JCheckBox("Word search"), gbc);
                add(new JCheckBox("Ignore Case"), gbc);
            }

        }

    }

}