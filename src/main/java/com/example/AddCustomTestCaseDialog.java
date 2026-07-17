package com.example;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.Frame;
import java.awt.Insets;

/** Compact dialog for entering a custom test case. */
final class AddCustomTestCaseDialog {

    private final JDialog dialog;

    AddCustomTestCaseDialog(Frame owner, AppThemePalette palette, TestCaseModel model) {
        AppThemePalette theme = palette != null ? palette : AppThemePalette.dark();
        dialog = new JDialog(owner, "Add Test Case", true);
        dialog.setLayout(new BorderLayout());
        dialog.getContentPane().setBackground(theme.frameBackground());

        JPanel content = new JPanel();
        content.setLayout(new BoxLayout(content, BoxLayout.Y_AXIS));
        content.setBackground(theme.frameBackground());
        content.setBorder(BorderFactory.createEmptyBorder(
                UiTokens.SPACE_4, UiTokens.SPACE_4, UiTokens.SPACE_4, UiTokens.SPACE_4));

        JLabel inputLabel = new JLabel("Input (required)");
        inputLabel.setForeground(theme.textColor());
        content.add(inputLabel);
        content.add(Box.createVerticalStrut(UiTokens.SPACE_1));

        JTextArea inputArea = new JTextArea(8, 40);
        inputArea.setFont(new Font(Font.MONOSPACED, Font.PLAIN, (int) UiTokens.CAPTION_FONT_SIZE));
        inputArea.setBackground(theme.surfaceBackground());
        inputArea.setForeground(theme.textColor());
        inputArea.setCaretColor(theme.textColor());
        JScrollPane inputScroll = new JScrollPane(inputArea);
        inputScroll.setBorder(BorderFactory.createLineBorder(theme.subtleBorderColor()));
        inputScroll.setAlignmentX(java.awt.Component.LEFT_ALIGNMENT);
        content.add(inputScroll);
        content.add(Box.createVerticalStrut(UiTokens.SPACE_3));

        JLabel outputLabel = new JLabel("Expected Output (optional)");
        outputLabel.setForeground(theme.textColor());
        content.add(outputLabel);
        content.add(Box.createVerticalStrut(UiTokens.SPACE_1));

        JTextArea outputArea = new JTextArea(8, 40);
        outputArea.setFont(new Font(Font.MONOSPACED, Font.PLAIN, (int) UiTokens.CAPTION_FONT_SIZE));
        outputArea.setBackground(theme.surfaceBackground());
        outputArea.setForeground(theme.textColor());
        outputArea.setCaretColor(theme.textColor());
        JScrollPane outputScroll = new JScrollPane(outputArea);
        outputScroll.setBorder(BorderFactory.createLineBorder(theme.subtleBorderColor()));
        outputScroll.setAlignmentX(java.awt.Component.LEFT_ALIGNMENT);
        content.add(outputScroll);

        JButton cancelButton = new JButton("Cancel");
        cancelButton.setFocusable(false);
        cancelButton.addActionListener(event -> dialog.dispose());

        JButton saveButton = new JButton("Add");
        saveButton.setFocusable(false);
        saveButton.addActionListener(event -> {
            String input = inputArea.getText();
            if (input == null || input.isBlank()) {
                JOptionPane.showMessageDialog(
                        dialog,
                        "Input is required.",
                        "Missing Input",
                        JOptionPane.WARNING_MESSAGE);
                return;
            }
            String expected = outputArea.getText();
            boolean hasExpected = expected != null && !expected.isBlank();
            model.addCustom(input, expected, hasExpected);
            dialog.dispose();
        });

        JPanel buttonRow = new JPanel(new FlowLayout(FlowLayout.RIGHT, UiTokens.SPACE_2, 0));
        buttonRow.setOpaque(false);
        buttonRow.add(cancelButton);
        buttonRow.add(saveButton);

        dialog.add(content, BorderLayout.CENTER);
        dialog.add(buttonRow, BorderLayout.SOUTH);
        dialog.setSize(new Dimension(640, 560));
        dialog.setLocationRelativeTo(owner);
    }

    void setVisible(boolean visible) {
        dialog.setVisible(visible);
    }
}
