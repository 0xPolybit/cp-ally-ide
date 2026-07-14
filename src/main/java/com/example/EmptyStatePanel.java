package com.example;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import java.awt.Component;
import java.awt.Font;

/** Reusable empty/loading placeholder with optional action. */
final class EmptyStatePanel extends JPanel {

    private final AppThemePalette palette;
    private final JPanel actionPanel = new JPanel();

    EmptyStatePanel(String title, String message, AppThemePalette palette) {
        this.palette = palette != null ? palette : AppThemePalette.dark();
        setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));
        setOpaque(true);
        setBackground(this.palette.frameBackground());
        setBorder(BorderFactory.createEmptyBorder(UiTokens.SPACE_6, UiTokens.SPACE_5, UiTokens.SPACE_6, UiTokens.SPACE_5));

        JLabel titleLabel = new JLabel(title == null ? "Nothing to show" : title);
        titleLabel.setAlignmentX(Component.CENTER_ALIGNMENT);
        titleLabel.setForeground(this.palette.textColor());
        titleLabel.setFont(titleLabel.getFont().deriveFont(Font.BOLD, UiTokens.SECTION_FONT_SIZE));
        add(titleLabel);

        if (message != null && !message.isBlank()) {
            add(Box.createVerticalStrut(UiTokens.SPACE_2));
            JLabel messageLabel = new JLabel("<html><div style='text-align:center;'>" + escape(message) + "</div></html>");
            messageLabel.setAlignmentX(Component.CENTER_ALIGNMENT);
            messageLabel.setForeground(this.palette.mutedTextColor());
            messageLabel.setFont(messageLabel.getFont().deriveFont(Font.PLAIN, UiTokens.BODY_FONT_SIZE));
            add(messageLabel);
        }

        actionPanel.setOpaque(false);
        actionPanel.setAlignmentX(Component.CENTER_ALIGNMENT);
        add(actionPanel);
        getAccessibleContext().setAccessibleName(title == null ? "Empty state" : title);
        getAccessibleContext().setAccessibleDescription(message == null ? "" : message);
    }

    void setAction(String text, Runnable action) {
        actionPanel.removeAll();
        if (text != null && !text.isBlank() && action != null) {
            JButton button = UiComponents.secondaryButton(text, action);
            actionPanel.add(button);
        }
        actionPanel.setVisible(text != null && !text.isBlank() && action != null);
        revalidate();
        repaint();
    }

    private static String escape(String value) {
        return value.replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;")
                .replace("\"", "&quot;");
    }
}
