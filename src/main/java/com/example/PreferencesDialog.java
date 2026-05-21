package com.example;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.DefaultComboBoxModel;
import javax.swing.JComboBox;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JSpinner;
import javax.swing.JTextField;
import javax.swing.SpinnerNumberModel;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.Frame;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.ItemEvent;

final class PreferencesDialog {

    private static final String[] DARK_EDITOR_SCHEMES = {
        "Eclipse Dark",
        "Monokai Dark",
        "Solarized Dark",
        "Dracula Dark",
        "Codeforces Dark"
    };

    private static final String[] LIGHT_EDITOR_SCHEMES = {
        "Eclipse Light",
        "Monokai Light",
        "Solarized Light",
        "Dracula Light",
        "Codeforces Light"
    };

    record PreferencesSelection(int editorFontSize, String editorColorScheme, String appTheme, boolean useTabsAsSpaces, int tabSpacing) {
    }

    private PreferencesDialog() {
    }

    static PreferencesSelection showDialog(Frame owner, PreferencesSelection initialSelection, AppThemePalette theme) {
        JDialog dialog = new JDialog(owner, "Preferences", true);
        dialog.setLayout(new BorderLayout());
        AppThemePalette palette = theme != null ? theme : AppThemePalette.dark();
        dialog.getContentPane().setBackground(palette.frameBackground());

        final PreferencesSelection[] result = new PreferencesSelection[1];

        JPanel content = new JPanel();
        content.setLayout(new BoxLayout(content, BoxLayout.Y_AXIS));
        content.setBackground(palette.panelBackground());
        content.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(palette.borderColor()),
                BorderFactory.createEmptyBorder(16, 16, 16, 16)));

        JLabel titleLabel = new JLabel("Preferences");
        titleLabel.setFont(titleLabel.getFont().deriveFont(Font.BOLD, 16f));
        titleLabel.setForeground(palette.textColor());
        titleLabel.setAlignmentX(Component.LEFT_ALIGNMENT);

        JLabel subtitleLabel = new JLabel("Configure editor appearance and behavior.");
        subtitleLabel.setForeground(palette.mutedTextColor());
        subtitleLabel.setAlignmentX(Component.LEFT_ALIGNMENT);

        JSpinner fontSizeSpinner = createFontSizeSpinner(initialSelection != null ? initialSelection.editorFontSize() : 14);
        JPanel fontSizeRow = createSettingRow("Editor Font Size", fontSizeSpinner, palette);

        JComboBox<String> appThemeCombo = new JComboBox<>(new String[]{
            "Light",
            "Dark",
            "Ultra Dark"
        });
        String initialAppTheme = initialSelection != null ? initialSelection.appTheme() : "Dark";
        appThemeCombo.setSelectedItem(initialAppTheme);
        if (appThemeCombo.getSelectedItem() == null) {
            appThemeCombo.setSelectedItem("Dark");
            initialAppTheme = "Dark";
        }
        JPanel appThemeRow = createSettingRow("App Theme", appThemeCombo, palette);

        JComboBox<String> colorSchemeCombo = new JComboBox<>(editorSchemesForTheme(initialAppTheme));
        colorSchemeCombo.setSelectedItem(mapEditorSchemeForTheme(
                initialSelection != null ? initialSelection.editorColorScheme() : colorSchemeCombo.getItemAt(0),
                initialAppTheme));
        if (colorSchemeCombo.getSelectedItem() == null) {
            colorSchemeCombo.setSelectedIndex(0);
        }
        JPanel colorSchemeRow = createSettingRow("Editor Color Scheme", colorSchemeCombo, palette);

        appThemeCombo.addItemListener(event -> {
            if (event.getStateChange() != ItemEvent.SELECTED) {
                return;
            }
            String selectedAppTheme = event.getItem().toString();
            String currentScheme = colorSchemeCombo.getSelectedItem() != null
                    ? colorSchemeCombo.getSelectedItem().toString()
                    : null;
            colorSchemeCombo.setModel(new DefaultComboBoxModel<>(editorSchemesForTheme(selectedAppTheme)));
            colorSchemeCombo.setSelectedItem(mapEditorSchemeForTheme(currentScheme, selectedAppTheme));
            if (colorSchemeCombo.getSelectedItem() == null && colorSchemeCombo.getItemCount() > 0) {
                colorSchemeCombo.setSelectedIndex(0);
            }
        });

        JCheckBox useTabsAsSpacesCheckbox = new JCheckBox();
        useTabsAsSpacesCheckbox.setSelected(initialSelection != null && initialSelection.useTabsAsSpaces());
        useTabsAsSpacesCheckbox.setFocusable(false);
        useTabsAsSpacesCheckbox.setBackground(palette.panelBackground());
        useTabsAsSpacesCheckbox.setForeground(palette.textColor());
        JPanel useTabsAsSpacesRow = createSettingRow("Use Tabs as Spaces", useTabsAsSpacesCheckbox, palette);

        JSpinner tabSpacingSpinner = createTabSpacingSpinner(initialSelection != null ? initialSelection.tabSpacing() : 4);
        JPanel tabSpacingRow = createSettingRow("Tab Spacing (spaces)", tabSpacingSpinner, palette);

        content.add(titleLabel);
        content.add(Box.createRigidArea(new Dimension(0, 8)));
        content.add(subtitleLabel);
        content.add(Box.createRigidArea(new Dimension(0, 18)));
        content.add(fontSizeRow);
        content.add(Box.createRigidArea(new Dimension(0, 12)));
        content.add(colorSchemeRow);
        content.add(Box.createRigidArea(new Dimension(0, 12)));
        content.add(appThemeRow);
        content.add(Box.createRigidArea(new Dimension(0, 12)));
        content.add(useTabsAsSpacesRow);
        content.add(Box.createRigidArea(new Dimension(0, 12)));
        content.add(tabSpacingRow);

        JButton cancelButton = new JButton("Cancel");
        cancelButton.addActionListener(e -> {
            result[0] = null;
            dialog.dispose();
        });

        JButton saveButton = new JButton("Save");
        saveButton.addActionListener(e -> {
            int fontSize = ((Number) fontSizeSpinner.getValue()).intValue();
            String colorScheme = colorSchemeCombo.getSelectedItem() != null
                    ? colorSchemeCombo.getSelectedItem().toString()
                    : editorSchemesForTheme(appThemeCombo.getSelectedItem() != null ? appThemeCombo.getSelectedItem().toString() : "Dark")[0];
            String appTheme = appThemeCombo.getSelectedItem() != null
                    ? appThemeCombo.getSelectedItem().toString()
                    : "Dark";
            boolean useTabsAsSpaces = useTabsAsSpacesCheckbox.isSelected();
            int tabSpacing = ((Number) tabSpacingSpinner.getValue()).intValue();
            result[0] = new PreferencesSelection(fontSize, colorScheme, appTheme, useTabsAsSpaces, tabSpacing);
            dialog.dispose();
        });

        JPanel footer = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
        footer.setOpaque(false);
        footer.setBorder(BorderFactory.createEmptyBorder(16, 0, 0, 0));
        footer.add(cancelButton);
        footer.add(saveButton);

        content.add(footer);

        JPanel root = new JPanel(new BorderLayout());
        root.setOpaque(false);
        root.setBorder(BorderFactory.createEmptyBorder(14, 14, 14, 14));
        root.add(content, BorderLayout.CENTER);

        dialog.add(root, BorderLayout.CENTER);
        dialog.setSize(500, 500);
        dialog.setMinimumSize(new Dimension(480, 480));
        dialog.setLocationRelativeTo(owner);
        dialog.setVisible(true);
        return result[0];
    }

    private static JSpinner createFontSizeSpinner(int value) {
        JSpinner spinner = new JSpinner(new SpinnerNumberModel(value, 8, 32, 1));
        spinner.setFocusable(false);
        spinner.setPreferredSize(new Dimension(90, 26));
        spinner.setMinimumSize(new Dimension(90, 26));
        spinner.setMaximumSize(new Dimension(90, 26));

        JSpinner.NumberEditor editor = new JSpinner.NumberEditor(spinner, "#");
        spinner.setEditor(editor);
        editor.getTextField().setColumns(3);
        editor.getTextField().setHorizontalAlignment(JTextField.CENTER);
        editor.getTextField().setForeground(new Color(223, 225, 229));
        editor.getTextField().setBackground(new Color(50, 53, 58));
        editor.getTextField().setCaretColor(new Color(223, 225, 229));
        return spinner;
    }

    private static JSpinner createTabSpacingSpinner(int value) {
        JSpinner spinner = new JSpinner(new SpinnerNumberModel(value, 2, 8, 1));
        spinner.setFocusable(false);
        spinner.setPreferredSize(new Dimension(90, 26));
        spinner.setMinimumSize(new Dimension(90, 26));
        spinner.setMaximumSize(new Dimension(90, 26));

        JSpinner.NumberEditor editor = new JSpinner.NumberEditor(spinner, "#");
        spinner.setEditor(editor);
        editor.getTextField().setColumns(3);
        editor.getTextField().setHorizontalAlignment(JTextField.CENTER);
        editor.getTextField().setForeground(new Color(223, 225, 229));
        editor.getTextField().setBackground(new Color(50, 53, 58));
        editor.getTextField().setCaretColor(new Color(223, 225, 229));
        return spinner;
    }

    private static JPanel createSettingRow(String labelText, Component inputComponent, AppThemePalette palette) {
        JPanel row = new JPanel(new GridBagLayout());
        row.setOpaque(false);
        row.setAlignmentX(Component.LEFT_ALIGNMENT);

        JLabel label = new JLabel(labelText + ":");
        label.setForeground(palette.textColor());
        label.setPreferredSize(new Dimension(170, 20));

        inputComponent.setFocusable(false);
        int defaultHeight = inputComponent.getPreferredSize().height;
        inputComponent.setPreferredSize(new Dimension(210, defaultHeight));
        inputComponent.setMinimumSize(new Dimension(210, defaultHeight));
        inputComponent.setMaximumSize(new Dimension(210, defaultHeight));

        GridBagConstraints gbc = new GridBagConstraints();

        gbc.gridx = 0;
        gbc.gridy = 0;
        gbc.anchor = GridBagConstraints.WEST;
        gbc.weightx = 0;
        gbc.insets = new Insets(0, 0, 0, 8);
        row.add(label, gbc);

        gbc.gridx = 1;
        gbc.weightx = 1.0;
        gbc.anchor = GridBagConstraints.EAST;
        gbc.fill = GridBagConstraints.NONE;
        row.add(inputComponent, gbc);

        // Prevent the row from becoming too tall when the dialog resizes
        int rowHeight = Math.max(label.getPreferredSize().height, inputComponent.getPreferredSize().height) + 8;
        row.setMaximumSize(new Dimension(Integer.MAX_VALUE, rowHeight));
        row.setPreferredSize(new Dimension(480, rowHeight));

        return row;
    }

    private static String[] editorSchemesForTheme(String appTheme) {
        return isLightTheme(appTheme) ? LIGHT_EDITOR_SCHEMES : DARK_EDITOR_SCHEMES;
    }

    private static boolean isLightTheme(String appTheme) {
        return appTheme != null && appTheme.trim().equalsIgnoreCase("Light");
    }

    private static String mapEditorSchemeForTheme(String scheme, String appTheme) {
        String[] options = editorSchemesForTheme(appTheme);
        if (scheme == null || scheme.isBlank()) {
            return options[0];
        }

        String normalized = scheme.trim();
        if (isLightTheme(appTheme)) {
            if (normalized.endsWith(" Dark")) {
                normalized = normalized.substring(0, normalized.length() - 5) + " Light";
            } else if (!normalized.endsWith(" Light")) {
                normalized = normalized + " Light";
            }
        } else {
            if (normalized.endsWith(" Light")) {
                normalized = normalized.substring(0, normalized.length() - 6) + " Dark";
            } else if (!normalized.endsWith(" Dark")) {
                normalized = normalized + " Dark";
            }
        }

        for (String option : options) {
            if (option.equalsIgnoreCase(normalized)) {
                return option;
            }
        }
        return options[0];
    }
}
