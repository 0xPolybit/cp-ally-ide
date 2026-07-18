package com.example;

import javax.swing.BorderFactory;
import javax.swing.DefaultComboBoxModel;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSpinner;
import javax.swing.JTextField;
import javax.swing.SpinnerNumberModel;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Frame;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;

/** Categorized preferences dialog. Backed by {@link DialogShell}. */
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

    private static final String[] APP_THEMES = {"Light", "Dark", "Ultra Dark"};

    record PreferencesSelection(
            int editorFontSize,
            String editorColorScheme,
            String appTheme,
            boolean useTabsAsSpaces,
            int tabSpacing,
            boolean autosaveEnabled,
            int autosaveIntervalSeconds) {
    }

    private PreferencesDialog() {
    }

    static PreferencesSelection showDialog(Frame owner, PreferencesSelection initialSelection, AppThemePalette theme) {
        AppThemePalette palette = theme != null ? theme : AppThemePalette.dark();
        DialogShell shell = new DialogShell(owner, "Preferences", palette, true);
        shell.setMinimumSize(new Dimension(520, 360));

        JPanel body = new JPanel(new GridBagLayout());
        body.setOpaque(false);
        body.setBorder(BorderFactory.createEmptyBorder(0, 0, 0, 0));

        GridBagConstraints labelGbc = new GridBagConstraints();
        labelGbc.gridx = 0;
        labelGbc.anchor = GridBagConstraints.WEST;
        labelGbc.insets = new Insets(8, 4, 8, 12);
        labelGbc.weightx = 0;

        GridBagConstraints controlGbc = new GridBagConstraints();
        controlGbc.gridx = 1;
        controlGbc.weightx = 1.0;
        controlGbc.fill = GridBagConstraints.HORIZONTAL;
        controlGbc.insets = new Insets(8, 0, 8, 4);

        int row = 0;

        JSpinner fontSizeSpinner = createFontSizeSpinner(initialSelection != null
                ? initialSelection.editorFontSize() : 14);
        applySpinnerTheme(fontSizeSpinner, palette);
        addRow(body, labelGbc, controlGbc, row++,
                "Editor Font Size", "Default 14. Range 8-32.",
                fontSizeSpinner);

        JComboBox<String> appThemeCombo = new JComboBox<>(APP_THEMES);
        String initialAppTheme = initialSelection != null ? initialSelection.appTheme() : "Dark";
        appThemeCombo.setSelectedItem(initialAppTheme == null ? "Dark" : initialAppTheme);
        applyComboTheme(appThemeCombo, palette);
        addRow(body, labelGbc, controlGbc, row++,
                "App Theme", "Application shell palette.",
                appThemeCombo);

        JComboBox<String> colorSchemeCombo = new JComboBox<>(
                editorSchemesForTheme(initialAppTheme));
        colorSchemeCombo.setSelectedItem(mapEditorSchemeForTheme(
                initialSelection != null ? initialSelection.editorColorScheme() : colorSchemeCombo.getItemAt(0),
                initialAppTheme));
        applyComboTheme(colorSchemeCombo, palette);
        addRow(body, labelGbc, controlGbc, row++,
                "Editor Color Scheme", "Syntax colors. Filtered to match the application theme.",
                colorSchemeCombo);

        appThemeCombo.addItemListener(event -> {
            if (event.getStateChange() != java.awt.event.ItemEvent.SELECTED) return;
            String selectedAppTheme = event.getItem().toString();
            String currentScheme = colorSchemeCombo.getSelectedItem() != null
                    ? colorSchemeCombo.getSelectedItem().toString() : null;
            colorSchemeCombo.setModel(new DefaultComboBoxModel<>(editorSchemesForTheme(selectedAppTheme)));
            colorSchemeCombo.setSelectedItem(mapEditorSchemeForTheme(currentScheme, selectedAppTheme));
            if (colorSchemeCombo.getSelectedItem() == null && colorSchemeCombo.getItemCount() > 0) {
                colorSchemeCombo.setSelectedIndex(0);
            }
        });

        JCheckBox useTabsAsSpacesCheckbox = new JCheckBox();
        useTabsAsSpacesCheckbox.setSelected(initialSelection != null && initialSelection.useTabsAsSpaces());
        useTabsAsSpacesCheckbox.setFocusable(true);
        useTabsAsSpacesCheckbox.setRequestFocusEnabled(true);
        useTabsAsSpacesCheckbox.setOpaque(false);
        useTabsAsSpacesCheckbox.setForeground(palette.textColor());
        useTabsAsSpacesCheckbox.setBackground(palette.frameBackground());
        addRow(body, labelGbc, controlGbc, row++,
                "Use Tabs as Spaces", "Indent with spaces instead of literal tab characters.",
                useTabsAsSpacesCheckbox);

        JSpinner tabSpacingSpinner = createTabSpacingSpinner(initialSelection != null
                ? initialSelection.tabSpacing() : 4);
        applySpinnerTheme(tabSpacingSpinner, palette);
        addRow(body, labelGbc, controlGbc, row++,
                "Tab Spacing (spaces)", "Visual width of a tab character.",
                tabSpacingSpinner);

        JCheckBox autosaveCheckbox = new JCheckBox();
        autosaveCheckbox.setSelected(initialSelection == null || initialSelection.autosaveEnabled());
        autosaveCheckbox.setFocusable(true);
        autosaveCheckbox.setRequestFocusEnabled(true);
        autosaveCheckbox.setOpaque(false);
        autosaveCheckbox.setForeground(palette.textColor());
        autosaveCheckbox.setBackground(palette.frameBackground());
        addRow(body, labelGbc, controlGbc, row++,
                "Enable Auto-save", "Save your solution while typing.",
                autosaveCheckbox);

        JSpinner autosaveIntervalSpinner = new JSpinner(new SpinnerNumberModel(
                initialSelection != null ? initialSelection.autosaveIntervalSeconds() : 10, 1, 600, 1));
        applySpinnerTheme(autosaveIntervalSpinner, palette);
        addRow(body, labelGbc, controlGbc, row++,
                "Auto-save Interval (sec)", "Seconds between automatic snapshots.",
                autosaveIntervalSpinner);

        JScrollPane scroll = new JScrollPane(body);
        scroll.setBorder(BorderFactory.createEmptyBorder());
        scroll.getViewport().setBackground(palette.frameBackground());
        scroll.getVerticalScrollBar().setUnitIncrement(16);

        shell.setContentAndHeader(scroll, "Preferences", "Workspace, editor, and saving");
        shell.setSize(620, 540);

        final PreferencesSelection[] result = new PreferencesSelection[1];
        JButton save = UiComponents.primaryButton("Save", () -> {
            int fontSize = ((Number) fontSizeSpinner.getValue()).intValue();
            String scheme = colorSchemeCombo.getSelectedItem() != null
                    ? colorSchemeCombo.getSelectedItem().toString()
                    : editorSchemesForTheme(
                            appThemeCombo.getSelectedItem() != null
                                    ? appThemeCombo.getSelectedItem().toString() : "Dark")[0];
            String appTheme = appThemeCombo.getSelectedItem() != null
                    ? appThemeCombo.getSelectedItem().toString() : "Dark";
            boolean useTabsAsSpaces = useTabsAsSpacesCheckbox.isSelected();
            int tabSpacing = ((Number) tabSpacingSpinner.getValue()).intValue();
            boolean autosaveEnabled = autosaveCheckbox.isSelected();
            int autosaveInterval = ((Number) autosaveIntervalSpinner.getValue()).intValue();
            result[0] = new PreferencesSelection(
                    fontSize, scheme, appTheme,
                    useTabsAsSpaces, tabSpacing,
                    autosaveEnabled, autosaveInterval);
            shell.dispose();
        });
        JButton cancel = UiComponents.quietButton("Cancel", shell::dispose);
        shell.addAction("Save", () -> save.doClick());
        // Cancel is always present, but DialogShell already added its own close
        // button. Add a labeled Cancel explicitly so users can read its action.
        ((java.awt.Container) shell.dialog().getContentPane().getComponent(2))
                .add(cancel);

        shell.setVisible(true);
        return result[0];
    }

    private static void addRow(JPanel body,
                                GridBagConstraints labelGbc,
                                GridBagConstraints controlGbc,
                                int row,
                                String title,
                                String description,
                                java.awt.Component control) {
        labelGbc.gridy = row;
        controlGbc.gridy = row;
        body.add(buildLabel(title, description), labelGbc);
        body.add(control, controlGbc);
    }

    private static JPanel buildLabel(String title, String description) {
        JPanel panel = new JPanel();
        panel.setOpaque(false);
        panel.setLayout(new javax.swing.BoxLayout(panel, javax.swing.BoxLayout.Y_AXIS));
        JLabel titleLabel = new JLabel(title);
        titleLabel.setForeground(AppThemePalette.dark().textColor());
        titleLabel.setFont(titleLabel.getFont().deriveFont(Font.BOLD, 13f));
        panel.add(titleLabel);
        JLabel descLabel = new JLabel("<html><span style='color: #7d8493;'>" + description + "</span></html>");
        descLabel.setFont(descLabel.getFont().deriveFont(Font.PLAIN, 11f));
        panel.add(descLabel);
        return panel;
    }

    private static JSpinner createFontSizeSpinner(int value) {
        JSpinner spinner = new JSpinner(new SpinnerNumberModel(value, 8, 32, 1));
        spinner.setFocusable(true);
        spinner.setPreferredSize(new Dimension(UiTokens.SPINNER_WIDTH, UiTokens.COMPACT_CONTROL_HEIGHT));
        spinner.setMinimumSize(new Dimension(UiTokens.SPINNER_WIDTH, UiTokens.COMPACT_CONTROL_HEIGHT));
        spinner.setMaximumSize(new Dimension(UiTokens.SPINNER_WIDTH, UiTokens.COMPACT_CONTROL_HEIGHT));
        JSpinner.NumberEditor editor = new JSpinner.NumberEditor(spinner, "#");
        spinner.setEditor(editor);
        editor.getTextField().setColumns(3);
        editor.getTextField().setHorizontalAlignment(JTextField.CENTER);
        return spinner;
    }

    private static JSpinner createTabSpacingSpinner(int value) {
        JSpinner spinner = new JSpinner(new SpinnerNumberModel(value, 2, 8, 1));
        spinner.setFocusable(true);
        spinner.setPreferredSize(new Dimension(UiTokens.SPINNER_WIDTH, UiTokens.COMPACT_CONTROL_HEIGHT));
        spinner.setMinimumSize(new Dimension(UiTokens.SPINNER_WIDTH, UiTokens.COMPACT_CONTROL_HEIGHT));
        spinner.setMaximumSize(new Dimension(UiTokens.SPINNER_WIDTH, UiTokens.COMPACT_CONTROL_HEIGHT));
        JSpinner.NumberEditor editor = new JSpinner.NumberEditor(spinner, "#");
        spinner.setEditor(editor);
        editor.getTextField().setColumns(3);
        editor.getTextField().setHorizontalAlignment(JTextField.CENTER);
        return spinner;
    }

    private static void applySpinnerTheme(JSpinner spinner, AppThemePalette palette) {
        if (spinner == null || palette == null) return;
        if (spinner.getEditor() instanceof JSpinner.NumberEditor numberEditor) {
            numberEditor.getTextField().setForeground(palette.inputForeground());
            numberEditor.getTextField().setBackground(palette.inputBackground());
            numberEditor.getTextField().setCaretColor(palette.accentColor());
        }
    }

    private static void applyComboTheme(JComboBox<String> combo, AppThemePalette palette) {
        if (combo == null || palette == null) return;
        combo.setBackground(palette.inputBackground());
        combo.setForeground(palette.inputForeground());
        combo.setFocusable(true);
    }

    private static String[] editorSchemesForTheme(String appTheme) {
        return isLightTheme(appTheme) ? LIGHT_EDITOR_SCHEMES : DARK_EDITOR_SCHEMES;
    }

    private static boolean isLightTheme(String appTheme) {
        return appTheme != null && appTheme.trim().equalsIgnoreCase("Light");
    }

    private static String mapEditorSchemeForTheme(String scheme, String appTheme) {
        String[] options = editorSchemesForTheme(appTheme);
        if (scheme == null || scheme.isBlank()) return options[0];
        String normalized = scheme.trim();
        if (isLightTheme(appTheme)) {
            if (normalized.endsWith(" Dark")) {
                normalized = normalized.substring(0, normalized.length() - " Dark".length()) + " Light";
            } else if (!normalized.endsWith(" Light")) {
                normalized = normalized + " Light";
            }
        } else {
            if (normalized.endsWith(" Light")) {
                normalized = normalized.substring(0, normalized.length() - " Light".length()) + " Dark";
            } else if (!normalized.endsWith(" Dark")) {
                normalized = normalized + " Dark";
            }
        }
        for (String option : options) {
            if (option.equalsIgnoreCase(normalized)) return option;
        }
        return options[0];
    }
}
