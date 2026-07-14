package com.example;

import javax.swing.BorderFactory;
import javax.swing.JLabel;
import javax.swing.JPanel;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.FlowLayout;
import java.awt.Font;

/** Compact semantic status control that communicates state with text and color. */
final class StatusBadge extends JPanel {

    enum Kind { NEUTRAL, INFO, SUCCESS, WARNING, ERROR }

    private final AppThemePalette palette;
    private final JLabel label = new JLabel();

    StatusBadge(String text, Kind kind, AppThemePalette palette) {
        this.palette = palette != null ? palette : AppThemePalette.dark();
        setLayout(new FlowLayout(FlowLayout.LEFT, UiTokens.SPACE_2, 0));
        setOpaque(true);
        setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(this.palette.subtleBorderColor()),
                BorderFactory.createEmptyBorder(3, 8, 3, 8)));
        putClientProperty("JComponent.roundRect", true);

        label.setFont(label.getFont().deriveFont(Font.BOLD, UiTokens.SMALL_CAPTION_FONT_SIZE));
        add(label, BorderLayout.CENTER);
        setStatus(text, kind);
    }

    void setStatus(String text, Kind kind) {
        String value = text == null || text.isBlank() ? "Unknown" : text;
        Kind safeKind = kind != null ? kind : Kind.NEUTRAL;
        label.setText(value);
        label.setForeground(foregroundFor(safeKind));
        setBackground(backgroundFor(safeKind));
        getAccessibleContext().setAccessibleName(value + " status");
        getAccessibleContext().setAccessibleDescription("Current status: " + value);
    }

    String text() {
        return label.getText();
    }

    private Color foregroundFor(Kind kind) {
        return switch (kind) {
            case INFO -> palette.infoColor();
            case SUCCESS -> palette.successColor();
            case WARNING -> palette.warningColor();
            case ERROR -> palette.errorColor();
            case NEUTRAL -> palette.mutedTextColor();
        };
    }

    private Color backgroundFor(Kind kind) {
        Color source = foregroundFor(kind);
        int amount = palette.lightTheme() ? 235 : 45;
        return palette.lightTheme()
                ? new Color(
                        Math.min(255, (source.getRed() + amount) / 2),
                        Math.min(255, (source.getGreen() + amount) / 2),
                        Math.min(255, (source.getBlue() + amount) / 2))
                : new Color(
                        Math.min(255, source.getRed() / 4 + amount),
                        Math.min(255, source.getGreen() / 4 + amount),
                        Math.min(255, source.getBlue() / 4 + amount));
    }
}
