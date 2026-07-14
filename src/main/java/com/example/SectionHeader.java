package com.example;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JLabel;
import javax.swing.JPanel;
import java.awt.BorderLayout;
import java.awt.Font;

/** Consistent title/subtitle header with an optional trailing action area. */
final class SectionHeader extends JPanel {

    private final AppThemePalette palette;
    private final JPanel trailingPanel = new JPanel(new java.awt.FlowLayout(java.awt.FlowLayout.RIGHT, 0, 0));

    SectionHeader(String title, String subtitle, AppThemePalette palette) {
        this.palette = palette != null ? palette : AppThemePalette.dark();
        setLayout(new BorderLayout(UiTokens.SPACE_3, 0));
        setOpaque(false);
        setBorder(BorderFactory.createEmptyBorder(UiTokens.SPACE_2, UiTokens.SPACE_3, UiTokens.SPACE_2, UiTokens.SPACE_3));

        JPanel textPanel = new JPanel();
        textPanel.setOpaque(false);
        textPanel.setLayout(new BoxLayout(textPanel, BoxLayout.Y_AXIS));

        JLabel titleLabel = new JLabel(title == null ? "" : title);
        titleLabel.setForeground(this.palette.textColor());
        titleLabel.setFont(titleLabel.getFont().deriveFont(Font.BOLD, UiTokens.SECTION_FONT_SIZE));
        textPanel.add(titleLabel);

        if (subtitle != null && !subtitle.isBlank()) {
            textPanel.add(Box.createVerticalStrut(UiTokens.SPACE_1));
            JLabel subtitleLabel = new JLabel(subtitle);
            subtitleLabel.setForeground(this.palette.mutedTextColor());
            subtitleLabel.setFont(subtitleLabel.getFont().deriveFont(Font.PLAIN, UiTokens.CAPTION_FONT_SIZE));
            textPanel.add(subtitleLabel);
        }

        trailingPanel.setOpaque(false);
        add(textPanel, BorderLayout.CENTER);
        add(trailingPanel, BorderLayout.EAST);
        getAccessibleContext().setAccessibleName(title == null ? "Section" : title);
    }

    void setTrailingComponent(java.awt.Component component) {
        trailingPanel.removeAll();
        if (component != null) {
            trailingPanel.add(component);
        }
        revalidate();
        repaint();
    }
}
