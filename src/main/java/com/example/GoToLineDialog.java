package com.example;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.KeyStroke;
import java.awt.BorderLayout;
import java.awt.Frame;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.KeyEvent;

/**
 * A small modal "Go to Line" dialog. The user types a 1-based line
 * number; the dialog closes and the parent moves the caret.
 *
 * <p>The dialog is intentionally minimal: a label, an input field,
 * OK and Cancel. Esc closes; Enter accepts; the OK button is the
 * default so pressing Enter on the input also accepts.</p>
 */
final class GoToLineDialog {

    private final int maxLine;
    private Integer chosenLine;

    private GoToLineLineDialog dialog;

    GoToLineDialog(int maxLine) {
        this.maxLine = Math.max(1, maxLine);
    }

    Integer show(Frame owner) {
        dialog = new GoToLineLineDialog(owner);
        dialog.setVisible(true);
        return chosenLine;
    }

    private class GoToLineLineDialog extends JDialog {
        GoToLineLineDialog(Frame owner) {
            super(owner, "Go to Line", true);
            setDefaultCloseOperation(JDialog.DISPOSE_ON_CLOSE);
            setLayout(new BorderLayout());
            setSize(320, 130);
            setLocationRelativeTo(owner);

            JPanel content = new JPanel(new GridBagLayout());
            content.setBorder(BorderFactory.createEmptyBorder(12, 12, 12, 12));
            GridBagConstraints gbc = new GridBagConstraints();
            gbc.insets = new Insets(4, 4, 4, 4);
            gbc.anchor = GridBagConstraints.WEST;

            JLabel label = new JLabel("Line (1\u2013" + maxLine + "):");
            gbc.gridx = 0; gbc.gridy = 0;
            content.add(label, gbc);

            JTextField field = new JTextField(10);
            gbc.gridx = 1; gbc.gridy = 0;
            gbc.fill = GridBagConstraints.HORIZONTAL;
            gbc.weightx = 1.0;
            content.add(field, gbc);

            JButton okButton = new JButton("Go");
            okButton.setMnemonic(KeyEvent.VK_G);
            JButton cancelButton = new JButton("Cancel");
            cancelButton.setMnemonic(KeyEvent.VK_C);
            JPanel buttons = new JPanel();
            buttons.add(okButton);
            buttons.add(cancelButton);
            gbc.gridx = 0; gbc.gridy = 1; gbc.gridwidth = 2;
            gbc.anchor = GridBagConstraints.CENTER;
            content.add(buttons, gbc);

            getRootPane().setDefaultButton(okButton);
            getRootPane().registerKeyboardAction(
                    e -> dispose(),
                    KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0),
                    JComponent.WHEN_IN_FOCUSED_WINDOW);

            okButton.addActionListener(e -> accept(field.getText()));
            cancelButton.addActionListener(e -> {
                chosenLine = null;
                dispose();
            });

            add(content, BorderLayout.CENTER);
            field.requestFocusInWindow();
        }

        private void accept(String text) {
            if (text == null) {
                chosenLine = null;
                dispose();
                return;
            }
            try {
                int line = Integer.parseInt(text.trim());
                if (line < 1 || line > maxLine) {
                    javax.swing.JOptionPane.showMessageDialog(this,
                            "Line must be between 1 and " + maxLine + ".",
                            "Out of range",
                            javax.swing.JOptionPane.WARNING_MESSAGE);
                    return;
                }
                chosenLine = line;
                dispose();
            } catch (NumberFormatException nfe) {
                javax.swing.JOptionPane.showMessageDialog(this,
                        "Please enter a valid line number.",
                        "Invalid input",
                        javax.swing.JOptionPane.WARNING_MESSAGE);
            }
        }
    }

    // Suppress unused-import warning for JComponent in case future
    // enhancements (e.g. focus traversal) need it.
    @SuppressWarnings("unused")
    private static final Class<?> UNUSED = javax.swing.JComponent.class;
}
