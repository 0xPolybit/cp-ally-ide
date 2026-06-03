package com.example;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.DefaultComboBoxModel;
import javax.swing.JComboBox;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JScrollPane;
import javax.swing.JPanel;
import javax.swing.JSpinner;
import javax.swing.JTextField;
import javax.swing.SpinnerNumberModel;
import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.FlowLayout;
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

    record PreferencesSelection(int editorFontSize, String editorColorScheme, String appTheme, boolean useTabsAsSpaces, int tabSpacing, boolean autosaveEnabled, int autosaveIntervalSeconds) {
    }

    private PreferencesDialog() {
    }

    static PreferencesSelection showDialog(Frame owner, PreferencesSelection initialSelection, AppThemePalette theme) {
        JDialog dialog = new JDialog(owner, "Preferences", true);
        dialog.setLayout(new BorderLayout());
        AppThemePalette palette = theme != null ? theme : AppThemePalette.dark();
        dialog.getContentPane().setBackground(palette.frameBackground());

        final PreferencesSelection[] result = new PreferencesSelection[1];

        JPanel grid = new JPanel(new GridBagLayout());
        grid.setOpaque(false);
        grid.setBackground(palette.panelBackground());

        GridBagConstraints labelGbc = new GridBagConstraints();
        labelGbc.gridx = 0;
        labelGbc.anchor = GridBagConstraints.WEST;
        labelGbc.insets = new Insets(8, 12, 8, 12);

        GridBagConstraints controlGbc = new GridBagConstraints();
        controlGbc.gridx = 1;
        controlGbc.weightx = 1.0;
        controlGbc.fill = GridBagConstraints.HORIZONTAL;
        controlGbc.insets = new Insets(8, 0, 8, 12);

        int row = 0;

        JSpinner fontSizeSpinner = createFontSizeSpinner(initialSelection != null ? initialSelection.editorFontSize() : 14);
        applySpinnerTheme(fontSizeSpinner, palette);
        labelGbc.gridy = row; controlGbc.gridy = row++;
        grid.add(new JLabel("Editor Font Size:"), labelGbc);
        grid.add(fontSizeSpinner, controlGbc);

        JComboBox<String> appThemeCombo = new JComboBox<>(new String[]{"Light","Dark","Ultra Dark"});
        String initialAppTheme = initialSelection != null ? initialSelection.appTheme() : "Dark";
        appThemeCombo.setSelectedItem(initialAppTheme == null ? "Dark" : initialAppTheme);
        labelGbc.gridy = row; controlGbc.gridy = row++;
        grid.add(new JLabel("App Theme:"), labelGbc);
        grid.add(appThemeCombo, controlGbc);

        JComboBox<String> colorSchemeCombo = new JComboBox<>(editorSchemesForTheme(initialAppTheme));
        colorSchemeCombo.setSelectedItem(mapEditorSchemeForTheme(
                initialSelection != null ? initialSelection.editorColorScheme() : colorSchemeCombo.getItemAt(0),
                initialAppTheme));
        labelGbc.gridy = row; controlGbc.gridy = row++;
        grid.add(new JLabel("Editor Color Scheme:"), labelGbc);
        grid.add(colorSchemeCombo, controlGbc);

        appThemeCombo.addItemListener(event -> {
            if (event.getStateChange() != ItemEvent.SELECTED) return;
            String selectedAppTheme = event.getItem().toString();
            String currentScheme = colorSchemeCombo.getSelectedItem() != null ? colorSchemeCombo.getSelectedItem().toString() : null;
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
        labelGbc.gridy = row; controlGbc.gridy = row++;
        grid.add(new JLabel("Use Tabs as Spaces:"), labelGbc);
        grid.add(useTabsAsSpacesCheckbox, controlGbc);

        JSpinner tabSpacingSpinner = createTabSpacingSpinner(initialSelection != null ? initialSelection.tabSpacing() : 4);
        applySpinnerTheme(tabSpacingSpinner, palette);
        labelGbc.gridy = row; controlGbc.gridy = row++;
        grid.add(new JLabel("Tab Spacing (spaces):"), labelGbc);
        grid.add(tabSpacingSpinner, controlGbc);

        JCheckBox autosaveCheckbox = new JCheckBox();
        autosaveCheckbox.setSelected(initialSelection == null || initialSelection.autosaveEnabled());
        autosaveCheckbox.setFocusable(false);
        autosaveCheckbox.setBackground(palette.panelBackground());
        autosaveCheckbox.setForeground(palette.textColor());
        labelGbc.gridy = row; controlGbc.gridy = row++;
        grid.add(new JLabel("Enable Auto-save:"), labelGbc);
        grid.add(autosaveCheckbox, controlGbc);

        JSpinner autosaveIntervalSpinner = new JSpinner(new SpinnerNumberModel(initialSelection != null ? initialSelection.autosaveIntervalSeconds() : 10, 1, 600, 1));
        applySpinnerTheme(autosaveIntervalSpinner, palette);
        labelGbc.gridy = row; controlGbc.gridy = row++;
        grid.add(new JLabel("Auto-save Interval (sec):"), labelGbc);
        grid.add(autosaveIntervalSpinner, controlGbc);

        // Wrap grid in scroll pane
        JScrollPane scroll = new JScrollPane(grid);
        scroll.setBorder(BorderFactory.createEmptyBorder());
        scroll.getViewport().setBackground(palette.panelBackground());

        // Footer buttons
        JButton cancelButton = new JButton("Cancel");
        cancelButton.addActionListener(e -> { result[0] = null; dialog.dispose(); });
        JButton saveButton = new JButton("Save");
        saveButton.addActionListener(e -> {
            int fontSize = ((Number) fontSizeSpinner.getValue()).intValue();
            String colorScheme = colorSchemeCombo.getSelectedItem() != null
                    ? colorSchemeCombo.getSelectedItem().toString()
                    : editorSchemesForTheme(appThemeCombo.getSelectedItem() != null ? appThemeCombo.getSelectedItem().toString() : "Dark")[0];
            String appTheme = appThemeCombo.getSelectedItem() != null ? appThemeCombo.getSelectedItem().toString() : "Dark";
            boolean useTabsAsSpaces = useTabsAsSpacesCheckbox.isSelected();
            int tabSpacing = ((Number) tabSpacingSpinner.getValue()).intValue();
            boolean autosaveEnabled = autosaveCheckbox.isSelected();
            int autosaveInterval = ((Number) autosaveIntervalSpinner.getValue()).intValue();
            result[0] = new PreferencesSelection(fontSize, colorScheme, appTheme, useTabsAsSpaces, tabSpacing, autosaveEnabled, autosaveInterval);
            dialog.dispose();
        });

        JPanel footer = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 8));
        footer.setOpaque(false);
        footer.add(cancelButton);
        footer.add(saveButton);

        JPanel root = new JPanel(new BorderLayout());
        root.setOpaque(false);
        root.setBorder(BorderFactory.createEmptyBorder(12, 12, 12, 12));
        root.add(scroll, BorderLayout.CENTER);
        root.add(footer, BorderLayout.SOUTH);

        dialog.add(root, BorderLayout.CENTER);

        // Size: half of owner width/height (or sensible defaults)
        int w = 600, h = 520;
        if (owner != null && owner.isShowing()) {
            w = Math.max(480, owner.getWidth() / 2);
            h = Math.max(360, owner.getHeight() / 2);
        }
        dialog.setSize(w, h);
        dialog.setMinimumSize(new Dimension(480, 360));
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
        return spinner;
    }

    private static void applySpinnerTheme(JSpinner spinner, AppThemePalette palette) {
        if (spinner == null || palette == null) {
            return;
        }

        if (spinner.getEditor() instanceof JSpinner.NumberEditor numberEditor) {
            numberEditor.getTextField().setForeground(palette.inputForeground());
            numberEditor.getTextField().setBackground(palette.inputBackground());
            numberEditor.getTextField().setCaretColor(palette.accentColor());
        }
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
