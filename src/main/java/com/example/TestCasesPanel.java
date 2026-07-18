package com.example;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSplitPane;
import javax.swing.JTabbedPane;
import javax.swing.JTextArea;
import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Frame;
import java.awt.Font;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

final class TestCasesPanel {

    /**
     * Listener notified whenever the test-case list changes (samples or custom).
     * The parent UI uses this to update Run button state, dirty indicators, etc.
     */
    interface Listener {
        void onTestCasesChanged(TestCasesPanel source);
    }

    private final Frame owner;
    private final AppThemePalette theme;
    private final Map<String, String> copyPayloads = new HashMap<>();
    private final List<CodeExecutionService.TestCaseSpec> customTestCases = new ArrayList<>();
    private final JTabbedPane testCasesTabs = new JTabbedPane();
    private final JPanel rootPanel = new JPanel(new BorderLayout());
    private final List<Listener> listeners = new ArrayList<>();
    private List<CodeExecutionService.TestCaseSpec> sampleTestCases = List.of();

    TestCasesPanel(Frame owner, AppThemePalette theme) {
        this.owner = owner;
        this.theme = theme != null ? theme : AppThemePalette.dark();
        testCasesTabs.setBackground(this.theme.panelBackground());
        testCasesTabs.setForeground(this.theme.textColor());

        JButton addTestCaseButton = new JButton();
        javax.swing.ImageIcon addIcon = UiIconLoader.loadThemedClasspathIcon("add.png", this.theme, 16, 16);
        if (addIcon != null) {
            addTestCaseButton.setIcon(addIcon);
        } else {
            addTestCaseButton.setText("+");
        }
        addTestCaseButton.setToolTipText("Add Test Case (Ctrl+Shift+T)");
        addTestCaseButton.setFocusable(false);
        addTestCaseButton.setRequestFocusEnabled(false);
        addTestCaseButton.setPreferredSize(new Dimension(32, 28));
        addTestCaseButton.addActionListener(e -> showAddCustomTestCaseDialog());

        JLabel sectionLabel = new JLabel("Test Cases");
        sectionLabel.setForeground(this.theme.textColor());
        sectionLabel.setFont(sectionLabel.getFont().deriveFont(Font.BOLD, 15f));

        JPanel topBar = new JPanel(new BorderLayout());
        topBar.setOpaque(false);
        topBar.setBorder(BorderFactory.createEmptyBorder(0, 0, 10, 0));
        topBar.add(sectionLabel, BorderLayout.WEST);
        topBar.add(addTestCaseButton, BorderLayout.EAST);

        rootPanel.setOpaque(false);
        rootPanel.add(topBar, BorderLayout.NORTH);
        rootPanel.add(testCasesTabs, BorderLayout.CENTER);

        refreshTabs(-1);
    }

    JPanel createPanel() {
        return rootPanel;
    }

    /**
     * Replaces both the sample-derived payloads AND clears custom test cases.
     * Use this only when the problem actually changes (e.g. a new fetch), not
     * for cosmetic re-renders like zoom or theme changes — those should call
     * {@link #updateSamplePayloads(Map)} to preserve user-added tests.
     */
    void setSamplePayloads(Map<String, String> payloads) {
        copyPayloads.clear();
        if (payloads != null) {
            copyPayloads.putAll(payloads);
        }
        customTestCases.clear();
        refreshTabs(-1);
        fireChanged();
    }

    /**
     * Refreshes the sample-derived copy payloads (e.g. after a problem
     * statement re-render for zoom or theme changes) without touching the
     * custom test cases. This is the safe entry point for any code path that
     * only needs to rebuild the sample test set.
     */
    void updateSamplePayloads(Map<String, String> payloads) {
        copyPayloads.clear();
        if (payloads != null) {
            copyPayloads.putAll(payloads);
        }
        // Refresh the tabs only if the sample-derived set actually changed,
        // so the user's custom test selection is preserved when possible.
        List<CodeExecutionService.TestCaseSpec> newSamples = SampleTestCaseCollector.collect(copyPayloads);
        if (!newSamples.equals(sampleTestCases)) {
            refreshTabs(-1);
            fireChanged();
        }
    }

    /**
     * Replaces the custom test-case list. Used by persistence / restore.
     * Sample tests are not affected. Triggers a change notification.
     */
    void setCustomTestCases(List<CodeExecutionService.TestCaseSpec> tests) {
        customTestCases.clear();
        if (tests != null) {
            for (CodeExecutionService.TestCaseSpec spec : tests) {
                if (spec != null && spec.custom()) {
                    customTestCases.add(spec);
                }
            }
        }
        refreshTabs(-1);
        fireChanged();
    }

    /** Returns an immutable snapshot of the current custom tests. */
    List<CodeExecutionService.TestCaseSpec> getCustomTestCases() {
        return List.copyOf(customTestCases);
    }

    void addListener(Listener listener) {
        if (listener != null && !listeners.contains(listener)) {
            listeners.add(listener);
        }
    }

    void removeListener(Listener listener) {
        listeners.remove(listener);
    }

    private void fireChanged() {
        for (Listener listener : new ArrayList<>(listeners)) {
            try {
                listener.onTestCasesChanged(this);
            } catch (Exception ex) {
                DiagnosticLogger.error("[TestCasesPanel] Listener threw: " + ex.getMessage(), ex);
            }
        }
    }

    List<CodeExecutionService.TestCaseSpec> getExecutionTestCases() {
        List<CodeExecutionService.TestCaseSpec> testCases = new ArrayList<>(sampleTestCases);
        testCases.addAll(customTestCases);
        return testCases;
    }

    private void refreshTabs(int selectedIndex) {
        sampleTestCases = SampleTestCaseCollector.collect(copyPayloads);
        testCasesTabs.removeAll();

        if (sampleTestCases.isEmpty() && customTestCases.isEmpty()) {
            JPanel emptyPanel = new JPanel();
            emptyPanel.setBackground(theme.frameBackground());
            testCasesTabs.addTab("No Test Cases", emptyPanel);
            testCasesTabs.setSelectedIndex(0);
            return;
        }

        for (int i = 0; i < sampleTestCases.size(); i++) {
            testCasesTabs.addTab("Test " + (i + 1), createTestCasePanel(sampleTestCases.get(i)));
        }

        for (int i = 0; i < customTestCases.size(); i++) {
            int customIndex = i;
            String title = "Custom Test Case " + (i + 1);
            // Renumber the spec so results dialogs match the tab title after deletions.
            CodeExecutionService.TestCaseSpec spec = customTestCases.get(i);
            if (!title.equals(spec.displayName())) {
                spec = new CodeExecutionService.TestCaseSpec(
                        spec.input(), spec.expectedOutput(), spec.custom(), spec.expectedOutputProvided(), title);
                customTestCases.set(i, spec);
            }
            testCasesTabs.addTab(title, createTestCasePanel(spec));
            testCasesTabs.setTabComponentAt(sampleTestCases.size() + i, createClosableTabHeader(title, () -> {
                if (customIndex >= 0 && customIndex < customTestCases.size()) {
                    customTestCases.remove(customIndex);
                    refreshTabs(Math.max(0, sampleTestCases.size() + customIndex - 1));
                    fireChanged();
                }
            }));
        }

        if (selectedIndex >= 0 && selectedIndex < testCasesTabs.getTabCount()) {
            testCasesTabs.setSelectedIndex(selectedIndex);
        } else if (testCasesTabs.getTabCount() > 0) {
            testCasesTabs.setSelectedIndex(0);
        }
    }

    private JPanel createClosableTabHeader(String title, Runnable onClose) {
        JPanel header = new JPanel(new BorderLayout(6, 0));
        header.setOpaque(false);

        JLabel label = new JLabel(title);
        label.setForeground(theme.textColor());

        JButton closeButton = new JButton("x");
        closeButton.setFocusable(false);
        closeButton.setRequestFocusEnabled(false);
        closeButton.setBorder(BorderFactory.createEmptyBorder(0, 4, 0, 4));
        closeButton.setMargin(new java.awt.Insets(0, 4, 0, 4));
        closeButton.addActionListener(e -> onClose.run());

        header.add(label, BorderLayout.CENTER);
        header.add(closeButton, BorderLayout.EAST);
        return header;
    }

    private JPanel createTestCasePanel(CodeExecutionService.TestCaseSpec testCase) {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setBackground(theme.frameBackground());
        panel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        JSplitPane splitPane = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT);
        splitPane.setBackground(theme.frameBackground());
        splitPane.setResizeWeight(0.5);

        splitPane.setLeftComponent(createTestDataPanel("Input", testCase.input()));
        String expectedOutput = testCase.expectedOutputProvided() ? testCase.expectedOutput() : "Not provided";
        splitPane.setRightComponent(createTestDataPanel("Expected Output", expectedOutput));

        panel.add(splitPane, BorderLayout.CENTER);
        return panel;
    }

    private JPanel createTestDataPanel(String title, String data) {
        JPanel panel = new JPanel(new BorderLayout(0, 8));
        panel.setBackground(theme.frameBackground());
        panel.setBorder(BorderFactory.createEmptyBorder(8, 8, 8, 8));

        JLabel titleLabel = new JLabel(title);
        titleLabel.setForeground(theme.textColor());
        titleLabel.setFont(titleLabel.getFont().deriveFont(Font.BOLD));

        JTextArea textArea = new JTextArea(data);
        textArea.setEditable(false);
        textArea.setFocusable(false);
        textArea.setBackground(theme.surfaceBackground());
        textArea.setForeground(theme.textColor());
        textArea.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 12));
        textArea.setLineWrap(true);
        textArea.setWrapStyleWord(false);
        textArea.setMargin(new java.awt.Insets(6, 6, 6, 6));

        JScrollPane scrollPane = new JScrollPane(textArea);
        scrollPane.setBorder(BorderFactory.createLineBorder(theme.borderColor()));
        scrollPane.setBackground(theme.frameBackground());
        scrollPane.getVerticalScrollBar().setUnitIncrement(10);

        panel.add(titleLabel, BorderLayout.NORTH);
        panel.add(scrollPane, BorderLayout.CENTER);
        return panel;
    }

    private void showAddCustomTestCaseDialog() {
        JDialog dialog = new JDialog(owner, "Add Test Case", true);
        dialog.setLayout(new BorderLayout());
        dialog.getContentPane().setBackground(theme.frameBackground());

        JPanel content = new JPanel();
        content.setLayout(new BoxLayout(content, BoxLayout.Y_AXIS));
        content.setBackground(theme.frameBackground());
        content.setBorder(BorderFactory.createEmptyBorder(14, 14, 14, 14));

        JLabel inputLabel = new JLabel("Input (required)");
        inputLabel.setForeground(theme.textColor());
        content.add(inputLabel);
        content.add(Box.createVerticalStrut(6));

        JTextArea inputArea = new JTextArea(8, 40);
        inputArea.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 12));
        inputArea.setBackground(theme.surfaceBackground());
        inputArea.setForeground(theme.textColor());
        inputArea.setCaretColor(theme.textColor());
        JScrollPane inputScroll = new JScrollPane(inputArea);
        inputScroll.setBorder(BorderFactory.createLineBorder(theme.borderColor()));
        inputScroll.setAlignmentX(Component.LEFT_ALIGNMENT);
        content.add(inputScroll);
        content.add(Box.createVerticalStrut(12));

        JLabel outputLabel = new JLabel("Expected Output (optional)");
        outputLabel.setForeground(theme.textColor());
        content.add(outputLabel);
        content.add(Box.createVerticalStrut(6));

        JTextArea outputArea = new JTextArea(8, 40);
        outputArea.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 12));
        outputArea.setBackground(theme.surfaceBackground());
        outputArea.setForeground(theme.textColor());
        outputArea.setCaretColor(theme.textColor());
        JScrollPane outputScroll = new JScrollPane(outputArea);
        outputScroll.setBorder(BorderFactory.createLineBorder(theme.borderColor()));
        outputScroll.setAlignmentX(Component.LEFT_ALIGNMENT);
        content.add(outputScroll);

        JPanel actions = new JPanel(new BorderLayout());
        actions.setOpaque(false);

        JButton cancelButton = new JButton("Cancel");
        cancelButton.setFocusable(false);
        cancelButton.addActionListener(e -> dialog.dispose());

        JButton saveButton = new JButton("Add");
        saveButton.setFocusable(false);
        saveButton.addActionListener(e -> {
            String input = inputArea.getText();
            if (input == null || input.isBlank()) {
                JOptionPane.showMessageDialog(
                        dialog,
                        "Input is required.",
                        "Missing Input",
                        JOptionPane.WARNING_MESSAGE);
                return;
            }

            String expectedOutput = outputArea.getText();
            boolean hasExpectedOutput = expectedOutput != null && !expectedOutput.isBlank();
                String normalizedInput = ensureTrailingNewline(input);
                String normalizedExpectedOutput = hasExpectedOutput ? ensureTrailingNewline(expectedOutput) : "";
            customTestCases.add(new CodeExecutionService.TestCaseSpec(
                    normalizedInput,
                    normalizedExpectedOutput,
                    true,
                    hasExpectedOutput,
                    "Custom Test Case " + (customTestCases.size() + 1)));
            refreshTabs(sampleTestCases.size() + customTestCases.size() - 1);
            fireChanged();
            dialog.dispose();
        });

        JPanel buttonRow = new JPanel();
        buttonRow.setOpaque(false);
        buttonRow.add(cancelButton);
        buttonRow.add(saveButton);
        actions.add(buttonRow, BorderLayout.EAST);

        dialog.add(content, BorderLayout.CENTER);
        dialog.add(actions, BorderLayout.SOUTH);
        dialog.setSize(640, 560);
        dialog.setLocationRelativeTo(owner);
        dialog.setVisible(true);
    }

    void addCustomTestCase() {
        showAddCustomTestCaseDialog();
    }

    private String ensureTrailingNewline(String value) {
        if (value == null || value.isEmpty()) {
            return "\n";
        }
        return value.endsWith("\n") ? value : value + "\n";
    }
}