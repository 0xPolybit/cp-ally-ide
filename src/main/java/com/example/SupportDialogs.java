package com.example;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.KeyStroke;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Cursor;
import java.awt.Desktop;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Frame;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.net.URI;

final class SupportDialogs {

    private SupportDialogs() {
    }

    static void showRuntimeSupportDialog(Frame owner, String language, String supportInfo, AppThemePalette theme) {
        AppThemePalette palette = theme != null ? theme : AppThemePalette.dark();
        JDialog dialog = new JDialog(owner, "Language Support Details", true);
        dialog.setDefaultCloseOperation(JDialog.DISPOSE_ON_CLOSE);
        dialog.setLayout(new BorderLayout());
        dialog.setSize(new Dimension(500, 350));
        dialog.setLocationRelativeTo(owner);

        JScrollPane scrollPane = new JScrollPane(createRuntimeSupportPanel(supportInfo, language, palette));
        scrollPane.setBorder(BorderFactory.createLineBorder(palette.borderColor()));
        scrollPane.getViewport().setBackground(palette.panelBackground());
        scrollPane.getVerticalScrollBar().setUnitIncrement(16);

        JButton closeButton = new JButton("Close");
        closeButton.addActionListener(e -> dialog.dispose());
        JPanel buttonPanel = new JPanel();
        buttonPanel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
        buttonPanel.add(closeButton);

        dialog.getRootPane().registerKeyboardAction(
                e -> dialog.dispose(),
                KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0),
                JComponent.WHEN_IN_FOCUSED_WINDOW);

        dialog.add(scrollPane, BorderLayout.CENTER);
        dialog.add(buttonPanel, BorderLayout.SOUTH);
        dialog.setVisible(true);
    }

    static void showCreditsDialog(Frame owner, AppThemePalette theme) {
        AppThemePalette palette = theme != null ? theme : AppThemePalette.dark();
        JDialog dialog = new JDialog(owner, "Credits", true);
        dialog.setLayout(new BorderLayout());
        dialog.getContentPane().setBackground(palette.frameBackground());

        JPanel content = createCreditsContentPanel(palette);
        JScrollPane scrollPane = new JScrollPane(content);
        scrollPane.setBorder(BorderFactory.createLineBorder(palette.borderColor()));
        scrollPane.getViewport().setBackground(palette.frameBackground());
        scrollPane.getVerticalScrollBar().setUnitIncrement(16);

        JButton closeButton = new JButton("Close");
        closeButton.addActionListener(e -> dialog.dispose());

        JPanel footerPanel = new JPanel();
        footerPanel.setBackground(palette.frameBackground());
        footerPanel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
        footerPanel.add(closeButton);

        dialog.getRootPane().registerKeyboardAction(
                e -> dialog.dispose(),
                KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0),
                JComponent.WHEN_IN_FOCUSED_WINDOW);

        dialog.add(scrollPane, BorderLayout.CENTER);
        dialog.add(footerPanel, BorderLayout.SOUTH);
        dialog.setSize(760, 560);
        dialog.setLocationRelativeTo(owner);
        dialog.setVisible(true);
    }

    private static JPanel createRuntimeSupportPanel(String rawInfo, String language, AppThemePalette palette) {
        JPanel panel = new JPanel();
        panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
        panel.setBackground(palette.panelBackground());
        panel.setBorder(BorderFactory.createEmptyBorder(12, 12, 12, 12));

        if (language != null && !language.isBlank()) {
            JLabel label = new JLabel("Language: " + language);
            label.setFont(label.getFont().deriveFont(Font.BOLD, 13f));
            label.setForeground(palette.textColor());
            label.setAlignmentX(Component.LEFT_ALIGNMENT);
            panel.add(label);
            panel.add(Box.createRigidArea(new Dimension(0, 8)));
        }

        String[] lines = rawInfo == null ? new String[0] : rawInfo.split("\n");
        boolean inToolsSection = false;

        for (String line : lines) {
            if (line.isBlank()) {
                panel.add(Box.createRigidArea(new Dimension(0, 6)));
                continue;
            }

            if (line.equals("Required Tools:")) {
                JLabel label = new JLabel(line);
                label.setFont(label.getFont().deriveFont(Font.BOLD, 12f));
                label.setForeground(palette.textColor());
                label.setAlignmentX(Component.LEFT_ALIGNMENT);
                panel.add(label);
                inToolsSection = true;
            } else if (line.startsWith("Status:")) {
                JLabel label = new JLabel(line);
                label.setFont(label.getFont().deriveFont(Font.BOLD, 12f));
                label.setForeground(line.contains("Ready") ? palette.successColor() : palette.errorColor());
                label.setAlignmentX(Component.LEFT_ALIGNMENT);
                panel.add(label);
                inToolsSection = false;
            } else if (inToolsSection && (line.startsWith("  ✓") || line.startsWith("  ✗"))) {
                JLabel label = new JLabel(line);
                label.setFont(label.getFont().deriveFont(Font.PLAIN, 11f));
                label.setForeground(line.startsWith("  ✓") ? palette.successColor() : palette.errorColor());
                label.setAlignmentX(Component.LEFT_ALIGNMENT);
                panel.add(label);
            } else {
                JLabel label = new JLabel(line);
                label.setFont(label.getFont().deriveFont(Font.PLAIN, 11f));
                label.setForeground(palette.mutedTextColor());
                label.setAlignmentX(Component.LEFT_ALIGNMENT);
                panel.add(label);
            }
        }

        panel.add(Box.createVerticalGlue());
        return panel;
    }

    private static JPanel createCreditsContentPanel(AppThemePalette palette) {
        JPanel panel = new JPanel();
        panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
        panel.setBackground(palette.panelBackground());
        panel.setBorder(BorderFactory.createEmptyBorder(18, 18, 18, 18));

        JLabel title = new JLabel("Credits");
        title.setFont(title.getFont().deriveFont(Font.BOLD, 22f));
        title.setForeground(palette.textColor());
        title.setAlignmentX(Component.LEFT_ALIGNMENT);

        JLabel subtitle = new JLabel("Project author and public profiles");
        subtitle.setFont(subtitle.getFont().deriveFont(Font.PLAIN, 12f));
        subtitle.setForeground(palette.mutedTextColor());
        subtitle.setAlignmentX(Component.LEFT_ALIGNMENT);

        JPanel header = new JPanel();
        header.setLayout(new BoxLayout(header, BoxLayout.Y_AXIS));
        header.setOpaque(false);
        header.setAlignmentX(Component.LEFT_ALIGNMENT);
        header.add(title);
        header.add(Box.createRigidArea(new Dimension(0, 4)));
        header.add(subtitle);

        panel.add(header);
        panel.add(Box.createRigidArea(new Dimension(0, 14)));
        panel.add(createCreditsCard(
                "Personal",
                new String[][]{
                        {"Name", "Swastik Biswas"},
                        {"College", "Kalinga Institute for Industrial Technology"},
                        {"Nationality", "United States of America"}
            },
            palette
        ));
        panel.add(Box.createRigidArea(new Dimension(0, 14)));
        panel.add(createCreditsCard(
                "Links",
                new String[][]{
                        {"GitHub", "https://github.com/0xPolybit"},
                        {"Instagram", "https://www.instagram.com/swastikbiswas1776/"},
                        {"X", "https://x.com/0xSwastikBiswas"},
                        {"LinkedIn", "https://www.linkedin.com/in/polybit/"},
                        {"CodeForces", "https://codeforces.com/profile/swastikpolybitbiswas"},
                        {"LeetCode", "https://leetcode.com/u/swastikbiswas/"}
            },
            palette
        ));

        panel.add(Box.createVerticalGlue());
        return panel;
    }

    private static JPanel createCreditsCard(String heading, String[][] rows, AppThemePalette palette) {
        JPanel card = new JPanel();
        card.setLayout(new BoxLayout(card, BoxLayout.Y_AXIS));
        card.setBackground(palette.surfaceBackground());
        card.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(palette.borderColor()),
                BorderFactory.createEmptyBorder(14, 14, 14, 14)));
        card.setAlignmentX(Component.LEFT_ALIGNMENT);

        JLabel headingLabel = new JLabel(heading);
        headingLabel.setFont(headingLabel.getFont().deriveFont(Font.BOLD, 14f));
        headingLabel.setForeground(palette.successColor());
        headingLabel.setAlignmentX(Component.LEFT_ALIGNMENT);
        card.add(headingLabel);
        card.add(Box.createRigidArea(new Dimension(0, 10)));

        for (String[] row : rows) {
            card.add(createCreditsRow(row[0], row[1], palette));
            card.add(Box.createRigidArea(new Dimension(0, 8)));
        }

        return card;
    }

    private static JPanel createCreditsRow(String labelText, String valueText, AppThemePalette palette) {
        JPanel row = new JPanel(new BorderLayout(10, 0));
        row.setOpaque(false);
        row.setAlignmentX(Component.LEFT_ALIGNMENT);

        JLabel label = new JLabel(labelText + ":");
        label.setFont(label.getFont().deriveFont(Font.BOLD, 12f));
        label.setForeground(palette.mutedTextColor());

        JLabel value = createCreditsValueLabel(valueText, palette);
        value.setFont(value.getFont().deriveFont(Font.PLAIN, 12f));
        if (value.getForeground() == null || value.getForeground().equals(javax.swing.UIManager.getColor("Label.foreground"))) {
            value.setForeground(palette.textColor());
        }

        row.add(label, BorderLayout.WEST);
        row.add(value, BorderLayout.CENTER);
        return row;
    }

    private static JLabel createCreditsValueLabel(String valueText, AppThemePalette palette) {
        if (valueText != null && valueText.startsWith("http")) {
            JLabel link = new JLabel("<html><span style='color:" + toHex(palette.successColor()) + ";text-decoration:underline;'>" + valueText + "</span></html>");
            link.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
            link.addMouseListener(new MouseAdapter() {
                @Override
                public void mouseClicked(MouseEvent e) {
                    openExternalUrl(valueText);
                }
            });
            return link;
        }

        JLabel value = new JLabel(valueText);
        if ("Swastik Biswas".equals(valueText)) {
            value.setForeground(palette.successColor());
        } else if ("United States of America".equals(valueText)) {
            value.setForeground(palette.warningColor());
        }
        return value;
    }

    private static String toHex(Color color) {
        return String.format("#%02x%02x%02x", color.getRed(), color.getGreen(), color.getBlue());
    }

    private static void openExternalUrl(String url) {
        try {
            if (!Desktop.isDesktopSupported() || url == null || url.isBlank()) {
                return;
            }
            Desktop.getDesktop().browse(URI.create(url));
        } catch (Exception ignored) {
        }
    }
}