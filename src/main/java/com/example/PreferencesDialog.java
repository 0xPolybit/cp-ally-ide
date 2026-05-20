package com.example;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JCheckBox;
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

final class PreferencesDialog {

    record PreferencesSelection(int editorFontSize, String editorColorScheme, boolean useTabsAsSpaces, int tabSpacing) {
    }

    private PreferencesDialog() {
    }

    static PreferencesSelection showDialog(Frame owner, PreferencesSelection initialSelection) {
        JDialog dialog = new JDialog(owner, "Preferences", true);
        dialog.setLayout(new BorderLayout());
        dialog.getContentPane().setBackground(new Color(30, 31, 34));

        final PreferencesSelection[] result = new PreferencesSelection[1];

        JPanel content = new JPanel();
        content.setLayout(new BoxLayout(content, BoxLayout.Y_AXIS));
        content.setBackground(new Color(43, 45, 48));
        content.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(67, 71, 76)),
                BorderFactory.createEmptyBorder(16, 16, 16, 16)));

        JLabel titleLabel = new JLabel("Preferences");
        titleLabel.setFont(titleLabel.getFont().deriveFont(Font.BOLD, 16f));
        titleLabel.setForeground(new Color(223, 225, 229));
        titleLabel.setAlignmentX(Component.LEFT_ALIGNMENT);

        JLabel subtitleLabel = new JLabel("Configure editor appearance and behavior.");
        subtitleLabel.setForeground(new Color(169, 176, 188));
        subtitleLabel.setAlignmentX(Component.LEFT_ALIGNMENT);

        JSpinner fontSizeSpinner = createFontSizeSpinner(initialSelection != null ? initialSelection.editorFontSize() : 14);
        JPanel fontSizeRow = createSettingRow("Editor Font Size", fontSizeSpinner);

        JComboBox<String> colorSchemeCombo = new JComboBox<>(new String[]{
            "Eclipse Dark",
            "Monokai",
            "Solarized Dark",
            "Dracula",
            "Codeforces Modern"
        });
        colorSchemeCombo.setSelectedItem(initialSelection != null ? initialSelection.editorColorScheme() : "Eclipse Dark");
        if (colorSchemeCombo.getSelectedItem() == null) {
            colorSchemeCombo.setSelectedItem("Eclipse Dark");
        }
        JPanel colorSchemeRow = createSettingRow("Editor Color Scheme", colorSchemeCombo);

        JCheckBox useTabsAsSpacesCheckbox = new JCheckBox();
        useTabsAsSpacesCheckbox.setSelected(initialSelection != null && initialSelection.useTabsAsSpaces());
        useTabsAsSpacesCheckbox.setFocusable(false);
        useTabsAsSpacesCheckbox.setBackground(new Color(43, 45, 48));
        useTabsAsSpacesCheckbox.setForeground(new Color(223, 225, 229));
        JPanel useTabsAsSpacesRow = createSettingRow("Use Tabs as Spaces", useTabsAsSpacesCheckbox);

        JSpinner tabSpacingSpinner = createTabSpacingSpinner(initialSelection != null ? initialSelection.tabSpacing() : 4);
        JPanel tabSpacingRow = createSettingRow("Tab Spacing (spaces)", tabSpacingSpinner);

        content.add(titleLabel);
        content.add(Box.createRigidArea(new Dimension(0, 8)));
        content.add(subtitleLabel);
        content.add(Box.createRigidArea(new Dimension(0, 18)));
        content.add(fontSizeRow);
        content.add(Box.createRigidArea(new Dimension(0, 12)));
        content.add(colorSchemeRow);
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
                    : "Eclipse Dark";
            boolean useTabsAsSpaces = useTabsAsSpacesCheckbox.isSelected();
            int tabSpacing = ((Number) tabSpacingSpinner.getValue()).intValue();
            result[0] = new PreferencesSelection(fontSize, colorScheme, useTabsAsSpaces, tabSpacing);
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
        dialog.setSize(500, 440);
        dialog.setMinimumSize(new Dimension(480, 420));
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

    private static JPanel createSettingRow(String labelText, Component inputComponent) {
        JPanel row = new JPanel(new GridBagLayout());
        row.setOpaque(false);
        row.setAlignmentX(Component.LEFT_ALIGNMENT);

        JLabel label = new JLabel(labelText + ":");
        label.setForeground(new Color(223, 225, 229));
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
}
