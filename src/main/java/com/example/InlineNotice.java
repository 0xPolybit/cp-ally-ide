package com.example;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.awt.Font;

/** Reusable inline information/warning/error notice with an optional action. */
final class InlineNotice extends JPanel {

    private final AppThemePalette palette;
    private final JLabel titleLabel = new JLabel();
    private final JLabel messageLabel = new JLabel();
    private final JPanel actionPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 0, 0));

    InlineNotice(String title, String message, StatusBadge.Kind kind, AppThemePalette palette) {
        this.palette = palette != null ? palette : AppThemePalette.dark();
        StatusBadge.Kind safeKind = kind != null ? kind : StatusBadge.Kind.INFO;
        setLayout(new BorderLayout(UiTokens.SPACE_3, 0));
        setOpaque(true);
        setBackground(this.palette.surfaceRaised());
        setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createMatteBorder(0, 4, 0, 0, colorFor(safeKind)),
                BorderFactory.createEmptyBorder(UiTokens.SPACE_2, UiTokens.SPACE_3, UiTokens.SPACE_2, UiTokens.SPACE_3)));

        titleLabel.setText(title == null ? "Notice" : title);
        titleLabel.setForeground(this.palette.textColor());
        titleLabel.setFont(titleLabel.getFont().deriveFont(Font.BOLD, UiTokens.BODY_FONT_SIZE));

        messageLabel.setText(message == null ? "" : message);
        messageLabel.setForeground(this.palette.mutedTextColor());
        messageLabel.setFont(messageLabel.getFont().deriveFont(Font.PLAIN, UiTokens.CAPTION_FONT_SIZE));

        JPanel textPanel = new JPanel();
        textPanel.setOpaque(false);
        textPanel.setLayout(new javax.swing.BoxLayout(textPanel, javax.swing.BoxLayout.Y_AXIS));
        textPanel.add(titleLabel);
        if (!messageLabel.getText().isBlank()) {
            textPanel.add(javax.swing.Box.createVerticalStrut(UiTokens.SPACE_1));
            textPanel.add(messageLabel);
        }

        actionPanel.setOpaque(false);
        add(textPanel, BorderLayout.CENTER);
        add(actionPanel, BorderLayout.EAST);
        getAccessibleContext().setAccessibleName(titleLabel.getText());
        getAccessibleContext().setAccessibleDescription(messageLabel.getText());
    }

    void setAction(String text, Runnable action) {
        actionPanel.removeAll();
        if (text != null && !text.isBlank() && action != null) {
            JButton button = UiComponents.quietButton(text, action);
            actionPanel.add(button);
        }
        actionPanel.setVisible(text != null && !text.isBlank() && action != null);
        revalidate();
        repaint();
    }

    private java.awt.Color colorFor(StatusBadge.Kind kind) {
        return switch (kind) {
            case SUCCESS -> palette.successColor();
            case WARNING -> palette.warningColor();
            case ERROR -> palette.errorColor();
            case INFO -> palette.infoColor();
            case NEUTRAL -> palette.borderColor();
        };
    }
}
