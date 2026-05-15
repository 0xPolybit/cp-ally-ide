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

    private static final Color SURFACE = new Color(30, 31, 34);
    private static final Color PANEL = new Color(43, 45, 48);
    private static final Color BORDER = new Color(67, 71, 76);
    private static final Color TEXT = new Color(223, 225, 229);
    private static final Color MUTED = new Color(169, 176, 188);
    private static final Color GREEN = new Color(97, 214, 110);
    private static final Color RED = new Color(246, 86, 86);
    private static final Color GOLD = new Color(246, 198, 67);

    private SupportDialogs() {
    }

    static void showRuntimeSupportDialog(Frame owner, String language, String supportInfo) {
        JDialog dialog = new JDialog(owner, "Language Support Details", true);
        dialog.setDefaultCloseOperation(JDialog.DISPOSE_ON_CLOSE);
        dialog.setLayout(new BorderLayout());
        dialog.setSize(new Dimension(500, 350));
        dialog.setLocationRelativeTo(owner);

        JScrollPane scrollPane = new JScrollPane(createRuntimeSupportPanel(supportInfo, language));
        scrollPane.setBorder(BorderFactory.createLineBorder(BORDER));
        scrollPane.getViewport().setBackground(PANEL);
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

    static void showCreditsDialog(Frame owner) {
        JDialog dialog = new JDialog(owner, "Credits", true);
        dialog.setLayout(new BorderLayout());
        dialog.getContentPane().setBackground(SURFACE);

        JPanel content = createCreditsContentPanel();
        JScrollPane scrollPane = new JScrollPane(content);
        scrollPane.setBorder(BorderFactory.createLineBorder(BORDER));
        scrollPane.getViewport().setBackground(SURFACE);
        scrollPane.getVerticalScrollBar().setUnitIncrement(16);

        JButton closeButton = new JButton("Close");
        closeButton.addActionListener(e -> dialog.dispose());

        JPanel footerPanel = new JPanel();
        footerPanel.setBackground(SURFACE);
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

    private static JPanel createRuntimeSupportPanel(String rawInfo, String language) {
        JPanel panel = new JPanel();
        panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
        panel.setBackground(PANEL);
        panel.setBorder(BorderFactory.createEmptyBorder(12, 12, 12, 12));

        if (language != null && !language.isBlank()) {
            JLabel label = new JLabel("Language: " + language);
            label.setFont(label.getFont().deriveFont(Font.BOLD, 13f));
            label.setForeground(TEXT);
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
                label.setForeground(TEXT);
                label.setAlignmentX(Component.LEFT_ALIGNMENT);
                panel.add(label);
                inToolsSection = true;
            } else if (line.startsWith("Status:")) {
                JLabel label = new JLabel(line);
                label.setFont(label.getFont().deriveFont(Font.BOLD, 12f));
                label.setForeground(line.contains("Ready") ? GREEN : RED);
                label.setAlignmentX(Component.LEFT_ALIGNMENT);
                panel.add(label);
                inToolsSection = false;
            } else if (inToolsSection && (line.startsWith("  ✓") || line.startsWith("  ✗"))) {
                JLabel label = new JLabel(line);
                label.setFont(label.getFont().deriveFont(Font.PLAIN, 11f));
                label.setForeground(line.startsWith("  ✓") ? GREEN : RED);
                label.setAlignmentX(Component.LEFT_ALIGNMENT);
                panel.add(label);
            } else {
                JLabel label = new JLabel(line);
                label.setFont(label.getFont().deriveFont(Font.PLAIN, 11f));
                label.setForeground(MUTED);
                label.setAlignmentX(Component.LEFT_ALIGNMENT);
                panel.add(label);
            }
        }

        panel.add(Box.createVerticalGlue());
        return panel;
    }

    private static JPanel createCreditsContentPanel() {
        JPanel panel = new JPanel();
        panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
        panel.setBackground(PANEL);
        panel.setBorder(BorderFactory.createEmptyBorder(18, 18, 18, 18));

        JLabel title = new JLabel("Credits");
        title.setFont(title.getFont().deriveFont(Font.BOLD, 22f));
        title.setForeground(TEXT);
        title.setAlignmentX(Component.LEFT_ALIGNMENT);

        JLabel subtitle = new JLabel("Project author and public profiles");
        subtitle.setFont(subtitle.getFont().deriveFont(Font.PLAIN, 12f));
        subtitle.setForeground(MUTED);
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
                }
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
                }
        ));

        panel.add(Box.createVerticalGlue());
        return panel;
    }

    private static JPanel createCreditsCard(String heading, String[][] rows) {
        JPanel card = new JPanel();
        card.setLayout(new BoxLayout(card, BoxLayout.Y_AXIS));
        card.setBackground(SURFACE);
        card.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(BORDER),
                BorderFactory.createEmptyBorder(14, 14, 14, 14)));
        card.setAlignmentX(Component.LEFT_ALIGNMENT);

        JLabel headingLabel = new JLabel(heading);
        headingLabel.setFont(headingLabel.getFont().deriveFont(Font.BOLD, 14f));
        headingLabel.setForeground(GREEN);
        headingLabel.setAlignmentX(Component.LEFT_ALIGNMENT);
        card.add(headingLabel);
        card.add(Box.createRigidArea(new Dimension(0, 10)));

        for (String[] row : rows) {
            card.add(createCreditsRow(row[0], row[1]));
            card.add(Box.createRigidArea(new Dimension(0, 8)));
        }

        return card;
    }

    private static JPanel createCreditsRow(String labelText, String valueText) {
        JPanel row = new JPanel(new BorderLayout(10, 0));
        row.setOpaque(false);
        row.setAlignmentX(Component.LEFT_ALIGNMENT);

        JLabel label = new JLabel(labelText + ":");
        label.setFont(label.getFont().deriveFont(Font.BOLD, 12f));
        label.setForeground(MUTED);

        JLabel value = createCreditsValueLabel(valueText);
        value.setFont(value.getFont().deriveFont(Font.PLAIN, 12f));
        if (value.getForeground() == null || value.getForeground().equals(javax.swing.UIManager.getColor("Label.foreground"))) {
            value.setForeground(TEXT);
        }

        row.add(label, BorderLayout.WEST);
        row.add(value, BorderLayout.CENTER);
        return row;
    }

    private static JLabel createCreditsValueLabel(String valueText) {
        if (valueText != null && valueText.startsWith("http")) {
            JLabel link = new JLabel("<html><span style='color:#61d66e;text-decoration:underline;'>" + valueText + "</span></html>");
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
            value.setForeground(GREEN);
        } else if ("United States of America".equals(valueText)) {
            value.setForeground(GOLD);
        }
        return value;
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